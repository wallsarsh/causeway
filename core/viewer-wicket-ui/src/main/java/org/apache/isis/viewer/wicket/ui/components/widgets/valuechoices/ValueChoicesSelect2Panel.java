/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.viewer.wicket.ui.components.widgets.valuechoices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.select2.ChoiceProvider;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.all.named.NamedFacet;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettings;
import org.apache.isis.viewer.wicket.model.links.LinkAndLabel;
import org.apache.isis.viewer.wicket.model.mementos.ObjectAdapterMemento;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.model.models.ScalarModelWithMultiPending;
import org.apache.isis.viewer.wicket.model.models.ScalarModelWithPending;
import org.apache.isis.viewer.wicket.ui.components.actionmenu.entityactions.EntityActionUtil;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract;
import org.apache.isis.viewer.wicket.ui.components.scalars.reference.Select2;
import org.apache.isis.viewer.wicket.ui.components.widgets.ObjectAdapterMementoProviderAbstract;
import org.apache.isis.viewer.wicket.ui.components.widgets.bootstrap.FormGroup;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

public class ValueChoicesSelect2Panel extends ScalarPanelAbstract implements ScalarModelWithPending, ScalarModelWithMultiPending {

    private static final long serialVersionUID = 1L;

    private Select2 select2;
    private ObjectAdapterMemento pending;

    public ValueChoicesSelect2Panel(final String id, final ScalarModel scalarModel) {
        super(id, scalarModel);
        pending = scalarModel.getObjectAdapterMemento();
    }

    @Override
    protected MarkupContainer addComponentForRegular() {


        // same pattern as in ReferencePanel
        if(select2 == null) {
            if(getModel().isCollection()) {
                final IModel<ArrayList<ObjectAdapterMemento>> modelObject = ScalarModelWithMultiPending.Util.createModel(this);
                select2 = Select2.newSelect2MultiChoice(ID_SCALAR_VALUE, modelObject, scalarModel);
            } else {
                final IModel<ObjectAdapterMemento> modelObject = ScalarModelWithPending.Util.createModel(this);
                select2 = Select2.newSelect2Choice(ID_SCALAR_VALUE, modelObject, scalarModel);
            }

            final ObjectAdapter[] actionArgsHint = scalarModel.getActionArgsHint();
            setChoices(actionArgsHint);
            addStandardSemantics();
        } else {
            select2.clearInput();
        }


        final MarkupContainer labelIfRegular = createFormComponentLabel();
        if(getModel().isRequired()) {
            labelIfRegular.add(new CssClassAppender("mandatory"));
        }
        
        addOrReplace(labelIfRegular);

        final Label scalarName = new Label(ID_SCALAR_NAME, getRendering().getLabelCaption(select2.component()));
        if(getModel().isRequired()) {
            final String label = scalarName.getDefaultModelObjectAsString();
            if(!Strings.isNullOrEmpty(label)) {
                scalarName.add(new CssClassAppender("mandatory"));
            }
        }
        labelIfRegular.addOrReplace(scalarName);
        NamedFacet namedFacet = getModel().getFacet(NamedFacet.class);
        if (namedFacet != null) {
            scalarName.setEscapeModelStrings(namedFacet.escaped());
        }

        // find the links...
        final List<LinkAndLabel> entityActions = EntityActionUtil.getEntityActionLinksForAssociation(this.scalarModel, getDeploymentCategory());

        addPositioningCssTo(labelIfRegular, entityActions);

        addFeedbackOnlyTo(labelIfRegular, select2.component());
        addEditPropertyTo(labelIfRegular);

        // ... add entity links to panel (below and to right)
        addEntityActionLinksBelowAndRight(labelIfRegular, entityActions);

        return labelIfRegular;
    }

    private List<ObjectAdapterMemento> getChoiceMementos(final ObjectAdapter[] argumentsIfAvailable) {
        final List<ObjectAdapter> choices =
                scalarModel.getChoices(argumentsIfAvailable, getAuthenticationSession(), getDeploymentCategory());
        
        // take a copy otherwise is only lazily evaluated
        return Lists.newArrayList(Lists.transform(choices, ObjectAdapterMemento.Functions.fromAdapter()));
    }

    protected void addStandardSemantics() {
        setRequiredIfSpecified();
    }

    private void setRequiredIfSpecified() {
        final ScalarModel scalarModel = getModel();
        final boolean required = scalarModel.isRequired();
        select2.setRequired(required);
    }

    protected MarkupContainer createFormComponentLabel() {
        final String name = getModel().getName();
        select2.setLabel(Model.of(name));

        final FormGroup labelIfRegular = new FormGroup(ID_SCALAR_IF_REGULAR, select2.component());

        final String describedAs = getModel().getDescribedAs();
        if(describedAs != null) {
            labelIfRegular.add(new AttributeModifier("title", Model.of(describedAs)));
        }

        labelIfRegular.add(select2.component());

        return labelIfRegular;
    }

    @Override
    protected Component addComponentForCompact() {
        final Label labelIfCompact = new Label(ID_SCALAR_IF_COMPACT, getModel().getObjectAsString());
        addOrReplace(labelIfCompact);
        return labelIfCompact;
    }

    
    protected ChoiceProvider<ObjectAdapterMemento> newChoiceProvider(final List<ObjectAdapterMemento> choicesMementos) {
        return new FixedObjectAdapterMementoProvider(scalarModel, choicesMementos, wicketViewerSettings);
    }

    static class FixedObjectAdapterMementoProvider extends ObjectAdapterMementoProviderAbstract {

        private static final long serialVersionUID = 1L;
        private final List<ObjectAdapterMemento> choicesMementos;

        public FixedObjectAdapterMementoProvider(
                final ScalarModel scalarModel,
                final List<ObjectAdapterMemento> choicesMementos,
                final WicketViewerSettings wicketViewerSettings) {
            super(scalarModel, wicketViewerSettings);
            this.choicesMementos = choicesMementos;
        }

        @Override
        public Collection<ObjectAdapterMemento> toChoices(final Collection<String> ids) {
            final List<ObjectAdapterMemento> mementos = obtainMementos(null);

            final Predicate<ObjectAdapterMemento> lookupOam = new Predicate<ObjectAdapterMemento>() {
                @Override
                public boolean apply(ObjectAdapterMemento input) {
                    final String id = (String) getId(input);
                    return ids.contains(id);
                }
            };
            return Lists.newArrayList(FluentIterable.from(mementos).filter(lookupOam).toList());
        }

        @Override
        protected List<ObjectAdapterMemento> obtainMementos(String term) {
            return obtainMementos(term, choicesMementos);
        }

    }

    @Override
    protected boolean alwaysRebuildGui() {
        return true;
    }

    @Override
    protected void onBeforeRenderWhenViewMode() { 
        // View: Read only
        select2.setEnabled(false);
    }

    @Override
    protected void onBeforeRenderWhenEnabled() { 
        // Edit: read/write
        select2.setEnabled(true);

        // TODO: should the title AttributeModifier installed in onBeforeWhenDisabled be removed here?
    }

    @Override
    protected void onBeforeRenderWhenDisabled(final String disableReason) {
        super.onBeforeRenderWhenDisabled(disableReason);
        setTitleAttribute(disableReason);
        select2.setEnabled(false);
    }

    private void setTitleAttribute(final String titleAttribute) {
        getComponentForRegular().add(new AttributeModifier("title", Model.of(titleAttribute)));
    }

    
    @Override
    protected void addFormComponentBehavior(Behavior behavior) {
        for (Behavior b : select2.getBehaviors(ScalarUpdatingBehavior.class)) {
            select2.remove(b);
        }
        select2.add(behavior);
    }

    // //////////////////////////////////////

    @Override
    public boolean updateChoices(ObjectAdapter[] argsIfAvailable) {
        setChoices(argsIfAvailable);
        return true;
    }

    /**
     * sets up the choices, also ensuring that any currently held value is compatible.
     */
    private void setChoices(ObjectAdapter[] argsIfAvailable) {
        final List<ObjectAdapterMemento> choicesMementos = getChoiceMementos(argsIfAvailable);
        
        final ChoiceProvider<ObjectAdapterMemento> provider = newChoiceProvider(choicesMementos);
        select2.setProvider(provider);

        getModel().clearPending();

        final ObjectAdapterMemento objectAdapterMemento = getModel().getObjectAdapterMemento();
        if(objectAdapterMemento == null) {
            select2.getModel().setObject(null);
        } else {

            if(!getModel().isCollection()) {

                // if currently held value is not compatible with choices, then replace with the first choice
                if(!choicesMementos.contains(objectAdapterMemento)) {

                    final ObjectAdapterMemento newAdapterMemento =
                            choicesMementos.isEmpty()
                                    ? null
                                    : choicesMementos.get(0);

                    select2.getModel().setObject(newAdapterMemento);
                    getModel().setObjectMemento(newAdapterMemento, getPersistenceSession(), getSpecificationLoader());
                }

            } else {

            }
        }
    }

    
    // //////////////////////////////////////

    @Override
    public ObjectAdapterMemento getPending() {
        return pending;
    }

    public void setPending(ObjectAdapterMemento pending) {
        this.pending = pending;
    }

    @Override
    public ArrayList<ObjectAdapterMemento> getMultiPending() {
        return pending != null ? pending.getList() : null;
    }

    @Override
    public void setMultiPending(final ArrayList<ObjectAdapterMemento> pending) {
        this.pending = ObjectAdapterMemento.createForList(pending, scalarModel.getTypeOfSpecification().getSpecId());
    }

    public ScalarModel getScalarModel() {
        return scalarModel;
    }

    // //////////////////////////////////////

    @com.google.inject.Inject
    private WicketViewerSettings wicketViewerSettings;
}
