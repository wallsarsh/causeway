/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.runtimeservices.session;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.iactn.Interaction;
import org.apache.isis.applib.services.iactnlayer.InteractionContext;
import org.apache.isis.applib.services.iactnlayer.InteractionLayer;
import org.apache.isis.applib.services.iactnlayer.InteractionService;
import org.apache.isis.applib.services.iactnlayer.ThrowingRunnable;
import org.apache.isis.applib.services.inject.ServiceInjector;
import org.apache.isis.applib.util.schema.ChangesDtoUtils;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.applib.util.schema.InteractionDtoUtils;
import org.apache.isis.commons.internal.base._Casts;
import org.apache.isis.commons.internal.concurrent._ConcurrentContext;
import org.apache.isis.commons.internal.concurrent._ConcurrentTaskList;
import org.apache.isis.commons.internal.debug._Probe;
import org.apache.isis.commons.internal.debug.xray.XrayUi;
import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.core.interaction.integration.InteractionAwareTransactionalBoundaryHandler;
import org.apache.isis.core.interaction.scope.InteractionScopeAware;
import org.apache.isis.core.interaction.scope.InteractionScopeBeanFactoryPostProcessor;
import org.apache.isis.core.interaction.scope.InteractionScopeLifecycleHandler;
import org.apache.isis.applib.services.iactnlayer.InteractionLayerTracker;
import org.apache.isis.core.interaction.session.IsisInteraction;
import org.apache.isis.core.metamodel.services.publishing.CommandPublisher;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;
import org.apache.isis.core.runtime.events.MetamodelEventService;
import org.apache.isis.core.security.authentication.InteractionContextFactory;
import org.apache.isis.core.security.authentication.manager.AuthenticationManager;

import static org.apache.isis.commons.internal.base._With.requires;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * Is the factory of {@link Interaction}s.
 *
 * @implNote holds a reference to the current session using a thread-local
 */
@Service
@Named("isis.runtimeservices.InteractionServiceDefault")
@Order(OrderPrecedence.MIDPOINT)
@Primary
@Qualifier("Default")
@Log4j2
public class InteractionServiceDefault
implements
    InteractionService,
        InteractionLayerTracker {

    @Inject AuthenticationManager authenticationManager;
    @Inject MetamodelEventService runtimeEventService;
    @Inject SpecificationLoader specificationLoader;
    @Inject ServiceInjector serviceInjector;

    @Inject InteractionAwareTransactionalBoundaryHandler txBoundaryHandler;
    @Inject ClockService clockService;
    @Inject CommandPublisher commandPublisher;
    @Inject List<InteractionScopeAware> interactionScopeAwareBeans;

    private InteractionScopeLifecycleHandler interactionScopeLifecycleHandler;

    @PostConstruct
    public void initIsisInteractionScopeSupport() {
        this.interactionScopeLifecycleHandler = InteractionScopeBeanFactoryPostProcessor
                .initIsisInteractionScopeSupport(serviceInjector);
    }

    //@PostConstruct .. too early, needs services to be provisioned first
    @EventListener
    public void init(ContextRefreshedEvent event) {

        requires(authenticationManager, "authenticationManager");

        log.info("Initialising Isis System");
        log.info("working directory: {}", new File(".").getAbsolutePath());

        runtimeEventService.fireBeforeMetamodelLoading();

        val taskList = _ConcurrentTaskList.named("IsisInteractionFactoryDefault Init")
                .addRunnable("SpecificationLoader::createMetaModel", specificationLoader::createMetaModel)
                .addRunnable("ChangesDtoUtils::init", ChangesDtoUtils::init)
                .addRunnable("InteractionDtoUtils::init", InteractionDtoUtils::init)
                .addRunnable("CommandDtoUtils::init", CommandDtoUtils::init)
                ;

        taskList.submit(_ConcurrentContext.forkJoin());
        taskList.await();

        { // log any validation failures, experimental code however, not sure how to best propagate failures
            val validationResult = specificationLoader.getOrAssessValidationResult();
            if(validationResult.getNumberOfFailures()==0) {
                log.info("Validation PASSED");
            } else {
                log.error("### Validation FAILED, failure count: {}", validationResult.getNumberOfFailures());
                validationResult.forEach(failure->{
                    log.error("# " + failure.getMessage());
                });
                //throw _Exceptions.unrecoverable("Validation FAILED");
            }
        }

        runtimeEventService.fireAfterMetamodelLoaded();

    }

    private final ThreadLocal<Stack<InteractionLayer>> interactionLayerStack =
            ThreadLocal.withInitial(Stack::new);

    @Override
    public int getInteractionLayerCount() {
        return interactionLayerStack.get().size();
    }

    @Override
    public InteractionLayer openInteraction() {
        return currentInteractionLayer()
                // or else create an anonymous authentication layer
                .orElseGet(()->openInteraction(InteractionContextFactory.anonymous()));
    }

    @Override
    public InteractionLayer openInteraction(
            final @NonNull InteractionContext interactionContextToUse) {

        val isisInteraction = getOrCreateIsisInteraction();

        // check whether we should reuse any current authenticationLayer,
        // that is, if current authentication and authToUse are equal

        val reuseCurrentLayer = currentInteractionContext()
                .map(currentInteractionContext -> Objects.equals(currentInteractionContext, interactionContextToUse))
                .orElse(false);

        if(reuseCurrentLayer) {
            // we are done, just return the stack's top
            return interactionLayerStack.get().peek();
        }

        val interactionLayer = new InteractionLayer(isisInteraction, interactionContextToUse);

        interactionLayerStack.get().push(interactionLayer);

        if(isInBaseLayer()) {
        	postInteractionOpened(isisInteraction);
        }

        if(log.isDebugEnabled()) {
            log.debug("new interaction layer created (conversation-id={}, total-layers-on-stack={}, {})",
                    interactionId.get(),
                    interactionLayerStack.get().size(),
                    _Probe.currentThreadId());
        }

        if(XrayUi.isXrayEnabled()) {
            _Xray.newInteractionLayer(interactionLayerStack.get());
        }

        return interactionLayer;
    }

    private IsisInteraction getOrCreateIsisInteraction() {

        final Stack<InteractionLayer> interactionLayers = interactionLayerStack.get();
        return interactionLayers.isEmpty()
    			? new IsisInteraction(UUID.randomUUID())
				: _Casts.uncheckedCast(interactionLayers.firstElement().getInteraction());
    }

    @Override
    public void closeInteractionLayers() {
        log.debug("about to close the interaction stack (conversation-id={}, total-layers-on-stack={}, {})",
                interactionId.get(),
                interactionLayerStack.get().size(),
                _Probe.currentThreadId());

        closeInteractionLayerStackDownToStackSize(0);
    }

	@Override
    public Optional<InteractionLayer> currentInteractionLayer() {
    	val stack = interactionLayerStack.get();
    	return stack.isEmpty()
    	        ? Optional.empty()
                : Optional.of(stack.lastElement());
    }

    @Override
    public boolean isInInteraction() {
        return !interactionLayerStack.get().isEmpty();
    }

    // -- AUTHENTICATED EXECUTION

    @Override
    @SneakyThrows
    public <R> R call(
            final @NonNull InteractionContext interactionContext,
            final @NonNull Callable<R> callable) {

        final int stackSizeWhenEntering = interactionLayerStack.get().size();
        openInteraction(interactionContext);

        try {
            serviceInjector.injectServicesInto(callable);
            return callable.call();
        } finally {
            closeInteractionLayerStackDownToStackSize(stackSizeWhenEntering);
        }
    }

    @Override
    @SneakyThrows
    public void run(
            final @NonNull InteractionContext interactionContext,
            final @NonNull ThrowingRunnable runnable) {

        final int stackSizeWhenEntering = interactionLayerStack.get().size();
        openInteraction(interactionContext);

        try {
            serviceInjector.injectServicesInto(runnable);
            runnable.run();
        } finally {
            closeInteractionLayerStackDownToStackSize(stackSizeWhenEntering);
        }

    }

    // -- ANONYMOUS EXECUTION

    @Override
    @SneakyThrows
    public <R> R callAnonymous(@NonNull final Callable<R> callable) {
        if(isInInteraction()) {
            serviceInjector.injectServicesInto(callable);
            return callable.call(); // reuse existing session
        }
        return call(InteractionContextFactory.anonymous(), callable);
    }

    /**
     * Variant of {@link #callAnonymous(Callable)} that takes a runnable.
     * @param runnable
     */
    @Override
    @SneakyThrows
    public void runAnonymous(@NonNull final ThrowingRunnable runnable) {
        if(isInInteraction()) {
            serviceInjector.injectServicesInto(runnable);
            runnable.run(); // reuse existing session
            return;
        }
        run(InteractionContextFactory.anonymous(), runnable);
    }

    // -- CONVERSATION ID

    private final ThreadLocal<UUID> interactionId = ThreadLocal.withInitial(()->null);

    @Override
    public Optional<UUID> getInteractionId() {
        return Optional.ofNullable(interactionId.get());
    }

    // -- HELPER

    private boolean isInBaseLayer() {
    	return interactionLayerStack.get().size()==1;
    }

    private void postInteractionOpened(IsisInteraction interaction) {
        interactionId.set(interaction.getInteractionId());
        interactionScopeAwareBeans.forEach(bean->bean.beforeEnteringTransactionalBoundary(interaction));
        txBoundaryHandler.onOpen(interaction);
        val isSynchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        interactionScopeAwareBeans.forEach(bean->bean.afterEnteringTransactionalBoundary(interaction, isSynchronizationActive));
        interactionScopeLifecycleHandler.onTopLevelInteractionOpened();
    }

    private void preInteractionClosed(IsisInteraction interaction) {
        completeAndPublishCurrentCommand();
        interactionScopeLifecycleHandler.onTopLevelInteractionClosing(); // cleanup the isis-session scope
        val isSynchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        interactionScopeAwareBeans.forEach(bean->bean.beforeLeavingTransactionalBoundary(interaction, isSynchronizationActive));
        txBoundaryHandler.onClose(interaction);
        interactionScopeAwareBeans.forEach(bean->bean.afterLeavingTransactionalBoundary(interaction));
        interaction.close(); // do this last
    }

    private void closeInteractionLayerStackDownToStackSize(int downToStackSize) {

        log.debug("about to close authenication stack down to size {} (conversation-id={}, total-sessions-on-stack={}, {})",
                downToStackSize,
                interactionId.get(),
                interactionLayerStack.get().size(),
                _Probe.currentThreadId());

        val stack = interactionLayerStack.get();
        while(stack.size()>downToStackSize) {
        	if(isInBaseLayer()) {
        		// keep the stack unmodified yet, to allow for callbacks to properly operate
        		preInteractionClosed(_Casts.uncheckedCast(stack.peek().getInteraction()));
        	}
        	_Xray.closeInteractionLayer(stack);
            stack.pop();
        }
        if(downToStackSize == 0) {
            // cleanup thread-local
            interactionLayerStack.remove();
            interactionId.remove();
        }
    }

    private IsisInteraction getInternalInteractionElseFail() {
        val interaction = currentInteractionElseFail();
        if(interaction instanceof IsisInteraction) {
            return (IsisInteraction) interaction;
        }
        throw _Exceptions.unrecoverableFormatted("the framework does not recognice "
                + "this implementation of an Interaction: %s", interaction.getClass().getName());
    }

    // -- HELPER - COMMAND COMPLETION

    private void completeAndPublishCurrentCommand() {

        val interaction = getInternalInteractionElseFail();
        val command = interaction.getCommand();

        if(command.getStartedAt() != null && command.getCompletedAt() == null) {
            // the guard is in case we're here as the result of a redirect following a previous exception;just ignore.

            val priorInteractionExecution = interaction.getPriorExecution();
            final Timestamp completedAt =
                    priorInteractionExecution != null
                    ?
                        // copy over from the most recent (which will be the top-level) interaction
                        priorInteractionExecution.getCompletedAt()
                    :
                        // this could arise as the result of calling SessionManagementService#nextSession within an action
                        // the best we can do is to use the current time

                        // REVIEW: as for the interaction object, it is left somewhat high-n-dry.
                         clockService.getClock().javaSqlTimestamp();

            command.updater().setCompletedAt(completedAt);
        }

        commandPublisher.complete(command);

        interaction.clear();
    }

}
