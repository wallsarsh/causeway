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

package org.apache.isis.viewer.wicket.ui.pages.entity;

import org.apache.wicket.Application;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager.ConcurrencyChecking;
import org.apache.isis.core.metamodel.adapter.version.ConcurrencyException;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.deployment.DeploymentCategory;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.core.runtime.system.DeploymentType;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.wicket.model.common.PageParametersUtils;
import org.apache.isis.viewer.wicket.model.hints.IsisUiHintEvent;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.components.widgets.breadcrumbs.BreadcrumbModel;
import org.apache.isis.viewer.wicket.ui.components.widgets.breadcrumbs.BreadcrumbModelProvider;
import org.apache.isis.viewer.wicket.ui.pages.PageAbstract;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

/**
 * Web page representing an entity.
 */
@AuthorizeInstantiation("org.apache.isis.viewer.wicket.roles.USER")
public class EntityPage extends PageAbstract {

    private static final long serialVersionUID = 1L;
    
    private final EntityModel model;

    /**
     * Called reflectively, in support of 
     * {@link BookmarkablePageLink bookmarkable} links.
     */
    public EntityPage(final PageParameters pageParameters) {
        this(pageParameters, createEntityModel(pageParameters));
    }

    /**
     * Creates an EntityModel from the given page parameters.
     * Redirects to the application home page if there is no OID in the parameters.
     *
     * @param parameters The page parameters with the OID
     * @return An EntityModel for the requested OID
     */
    private static EntityModel createEntityModel(final PageParameters parameters) {
        String oid = EntityModel.oidStr(parameters);
        if (Strings.isEmpty(oid)) {
            throw new RestartResponseException(Application.get().getHomePage());
        }
        return new EntityModel(parameters);
    }

    private EntityPage(final PageParameters pageParameters, final EntityModel entityModel) {
        this(pageParameters, entityModel, null);
    }

    public EntityPage(final ObjectAdapter adapter) {
        this(adapter, null);
    }

    /**
     * Ensure that any {@link ConcurrencyException} that might have occurred already
     * (eg from an action invocation) is show.
     */
    public EntityPage(final ObjectAdapter adapter, final ConcurrencyException exIfAny) {
        this(PageParametersUtils.newPageParameters(), newEntityModel(adapter, exIfAny));
    }

    private static EntityModel newEntityModel(
            final ObjectAdapter adapter,
            final ConcurrencyException exIfAny) {
        final EntityModel model = new EntityModel(adapter);
        model.setException(exIfAny);
        return model;
    }

    private EntityPage(
            final PageParameters pageParameters,
            final EntityModel entityModel,
            final String titleString) {
        super(pageParameters, titleString, ComponentType.ENTITY);

        this.model = entityModel;

        final ObjectAdapter objectAdapter;
        try {
            // check object still exists
            objectAdapter = entityModel.getObject();
        } catch(final RuntimeException ex) {
            removeAnyBookmark(model);
            removeAnyBreadcrumb(model);

            // we throw an authorization exception here to avoid leaking out information as to whether the object exists or not.
            throw new ObjectMember.AuthorizationException(ex);
        }

        // check that the entity overall can be viewed.
        if(!ObjectAdapter.Util.isVisible(objectAdapter, InteractionInitiatedBy.USER)) {
            throw new ObjectMember.AuthorizationException();
        }




        //
        // invalidate the cache so that can do dynamic reloading of layout metadata etc.
        //
        // Note that it's necessary to load the page twice.  (I think) that the first time is to load the new
        // Java class files into the webapp (but too "late" to be used), the second then works.
        // Moving this functionality earlier on in the web request pipeline (eg WebRequestCycleForIsis)
        // made no difference.
        //
        // what might help is using some sort of daemon process to monitor when the class files change, and then
        // reload (a la JRebel).  Don't think DCEVM by itself is enough, but possibly using
        // https://github.com/fakereplace/fakereplace or https://github.com/spring-projects/spring-loaded
        // might instead suffice since they provide a java agent similar to JRebel.
        //
        if(!getDeploymentType().isProduction()) {
            getSpecificationLoader().invalidateCacheFor(objectAdapter.getObject());
        }

        if(titleString == null) {
            final String titleStr = objectAdapter.titleString(null);
            setTitle(titleStr);
        }

        WebMarkupContainer entityPageContainer = new WebMarkupContainer("entityPageContainer");
        entityPageContainer.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                ObjectAdapter adapter = entityModel.getObject();
                return adapter.getObject().getClass().getSimpleName();
            }
        }));
        themeDiv.addOrReplace(entityPageContainer);

        addChildComponents(entityPageContainer, model);
        
        // bookmarks and breadcrumbs
        bookmarkPage(model);
        addBreadcrumb(entityModel);

        addBookmarkedPages(entityPageContainer);


        // TODO mgrigorov: Zero Clipboard has been moved to EntityIconAndTitlePanel where the entity model is available.
        // Is this still needed for something else ?!
        //
        // ensure the copy link holds this page.
        send(this, Broadcast.BREADTH, new IsisUiHintEvent(entityModel, null));
    }

    private void addBreadcrumb(final EntityModel entityModel) {
        final BreadcrumbModelProvider session = (BreadcrumbModelProvider) getSession();
        final BreadcrumbModel breadcrumbModel = session.getBreadcrumbModel();
        breadcrumbModel.visited(entityModel);
    }

    private void removeAnyBreadcrumb(final EntityModel entityModel) {
        final BreadcrumbModelProvider session = (BreadcrumbModelProvider) getSession();
        final BreadcrumbModel breadcrumbModel = session.getBreadcrumbModel();
        breadcrumbModel.remove(entityModel);
    }

    /**
     * A rather crude way of intercepting the redirect-and-post strategy.
     * 
     * <p>
     * Performs eager loading of corresponding {@link EntityModel}, with
     * {@link ConcurrencyChecking#NO_CHECK no} concurrency checking.
     */
    @Override
    protected void onBeforeRender() {
        this.model.load(ConcurrencyChecking.NO_CHECK);
        super.onBeforeRender();
    }

    private DeploymentType getDeploymentType() {
        return IsisContext.getDeploymentType();
    }

    protected DeploymentCategory getDeploymentCategory() {
        return getDeploymentType().getDeploymentCategory();
    }


}
