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


package org.apache.isis.core.metamodel.specloader.internal;

import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.lang.ToString;
import org.apache.isis.core.metamodel.adapter.Instance;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.consent.InteractionInvocationMethod;
import org.apache.isis.core.metamodel.consent.InteractionResult;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionAddToFacet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionClearFacet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionRemoveFromFacet;
import org.apache.isis.core.metamodel.facets.propcoll.access.PropertyAccessorFacet;
import org.apache.isis.core.metamodel.interactions.CollectionAddToContext;
import org.apache.isis.core.metamodel.interactions.CollectionRemoveFromContext;
import org.apache.isis.core.metamodel.interactions.CollectionUsabilityContext;
import org.apache.isis.core.metamodel.interactions.CollectionVisibilityContext;
import org.apache.isis.core.metamodel.interactions.InteractionUtils;
import org.apache.isis.core.metamodel.interactions.UsabilityContext;
import org.apache.isis.core.metamodel.interactions.ValidityContext;
import org.apache.isis.core.metamodel.interactions.VisibilityContext;
import org.apache.isis.core.metamodel.runtimecontext.AuthenticationSessionProvider;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMap;
import org.apache.isis.core.metamodel.runtimecontext.QuerySubmitter;
import org.apache.isis.core.metamodel.runtimecontext.SpecificationLookup;
import org.apache.isis.core.metamodel.runtimecontext.spec.feature.FeatureType;
import org.apache.isis.core.metamodel.runtimecontext.spec.feature.ObjectAssociationAbstract;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.specloader.internal.peer.ObjectMemberPeer;
import org.apache.isis.core.metamodel.util.CollectionFacetUtils;


public class OneToManyAssociationImpl extends ObjectAssociationAbstract implements OneToManyAssociation {

    private final ObjectMemberPeer reflectiveAdapter;
    
    public OneToManyAssociationImpl(
    		final ObjectMemberPeer association,
            final AuthenticationSessionProvider authenticationSessionProvider,
    		final SpecificationLookup specificationLookup,
    		final AdapterMap adapterManager,
            final QuerySubmitter querySubmitter) {
        super(association.getIdentifier().getMemberName(), getSpecification(specificationLookup, association.getType()), FeatureType.COLLECTION, association, authenticationSessionProvider, specificationLookup, adapterManager, querySubmitter);
        this.reflectiveAdapter = association;
    }


    public ObjectMemberPeer getAssociationPeer(){
    	return reflectiveAdapter;
    }

    // /////////////////////////////////////////////////////////////
    // Hidden (or visible)
    // /////////////////////////////////////////////////////////////

    @Override
    public VisibilityContext<?> createVisibleInteractionContext(
            final AuthenticationSession session,
            final InteractionInvocationMethod invocationMethod,
            final ObjectAdapter ownerAdapter) {
        return new CollectionVisibilityContext(session, invocationMethod, ownerAdapter, getIdentifier());
    }

    // /////////////////////////////////////////////////////////////
    // Disabled (or enabled)
    // /////////////////////////////////////////////////////////////

    @Override
    public UsabilityContext<?> createUsableInteractionContext(
            final AuthenticationSession session,
            final InteractionInvocationMethod invocationMethod,
            final ObjectAdapter ownerAdapter) {
        return new CollectionUsabilityContext(session, invocationMethod, ownerAdapter, getIdentifier());
    }


    // /////////////////////////////////////////////////////////////
    // Validate Add
    // /////////////////////////////////////////////////////////////

    @Override
    public ValidityContext<?> createValidateAddInteractionContext(
            final AuthenticationSession session,
            final InteractionInvocationMethod invocationMethod,
            final ObjectAdapter ownerAdapter,
            final ObjectAdapter proposedToAddAdapter) {
        return new CollectionAddToContext(session, invocationMethod, ownerAdapter, getIdentifier(), proposedToAddAdapter);
    }

    /**
     * TODO: currently this method is hard-coded to assume all interactions are initiated
     * {@link InteractionInvocationMethod#BY_USER by user}.
     */
    @Override
    public Consent isValidToAdd(final ObjectAdapter ownerAdapter, final ObjectAdapter proposedToAddAdapter) {
        return isValidToAddResult(ownerAdapter, proposedToAddAdapter).createConsent();
    }

    private InteractionResult isValidToAddResult(final ObjectAdapter ownerAdapter, final ObjectAdapter proposedToAddAdapter) {
        final ValidityContext<?> validityContext = createValidateAddInteractionContext(getAuthenticationSession(),
                InteractionInvocationMethod.BY_USER, ownerAdapter, proposedToAddAdapter);
        return InteractionUtils.isValidResult(this, validityContext);
    }

    // /////////////////////////////////////////////////////////////
    // Validate Remove
    // /////////////////////////////////////////////////////////////

    @Override
    public ValidityContext<?> createValidateRemoveInteractionContext(
            final AuthenticationSession session,
            final InteractionInvocationMethod invocationMethod,
            final ObjectAdapter ownerAdapter,
            final ObjectAdapter proposedToRemoveAdapter) {
        return new CollectionRemoveFromContext(session, invocationMethod, ownerAdapter, getIdentifier(),
                proposedToRemoveAdapter);
    }

    /**
     * TODO: currently this method is hard-coded to assume all interactions are initiated
     * {@link InteractionInvocationMethod#BY_USER by user}.
     */
    @Override
    public Consent isValidToRemove(final ObjectAdapter ownerAdapter, final ObjectAdapter proposedToRemoveAdapter) {
        return isValidToRemoveResult(ownerAdapter, proposedToRemoveAdapter).createConsent();
    }

    private InteractionResult isValidToRemoveResult(final ObjectAdapter ownerAdapter, final ObjectAdapter proposedToRemoveAdapter) {
        final ValidityContext<?> validityContext = createValidateRemoveInteractionContext(getAuthenticationSession(),
                InteractionInvocationMethod.BY_USER, ownerAdapter, proposedToRemoveAdapter);
        return InteractionUtils.isValidResult(this, validityContext);
    }

    private boolean readWrite() {
        return !isNotPersisted();
    }

    // /////////////////////////////////////////////////////////////
    // get, isEmpty, add, clear
    // /////////////////////////////////////////////////////////////

    @Override
    public ObjectAdapter get(final ObjectAdapter ownerAdapter) {

        final PropertyAccessorFacet accessor = getFacet(PropertyAccessorFacet.class);
        final Object collection = accessor.getProperty(ownerAdapter);
        if (collection == null) {
            return null;
        }
        return getAdapterMap().adapterFor(collection, ownerAdapter, this);
    }




    @Override
    public boolean isEmpty(final ObjectAdapter parentAdapter) {
        // REVIEW should we be able to determine if a collection is empty without loading it?
        final ObjectAdapter collection = get(parentAdapter);
        final CollectionFacet facet = CollectionFacetUtils.getCollectionFacetFromSpec(collection);
        return facet.size(collection) == 0;
    }


    // /////////////////////////////////////////////////////////////
    // add, clear
    // /////////////////////////////////////////////////////////////

    @Override
    public void addElement(final ObjectAdapter ownerAdapter, final ObjectAdapter referencedAdapter) {
        if (referencedAdapter == null) {
            throw new IllegalArgumentException("Can't use null to add an item to a collection");
        }
        if (readWrite()) {
            if (ownerAdapter.isPersistent() && referencedAdapter.isTransient()) {
                throw new IsisException("can't set a reference to a transient object from a persistent one: "
                        + ownerAdapter.titleString() + " (persistent) -> " + referencedAdapter.titleString() + " (transient)");
            }
            final CollectionAddToFacet facet = getFacet(CollectionAddToFacet.class);
            facet.add(ownerAdapter, referencedAdapter);
        }
    }

    @Override
    public void removeElement(final ObjectAdapter ownerAdapter, final ObjectAdapter referencedAdapter) {
        if (referencedAdapter == null) {
            throw new IllegalArgumentException("element should not be null");
        }
        if (readWrite()) {
            final CollectionRemoveFromFacet facet = getFacet(CollectionRemoveFromFacet.class);
            facet.remove(ownerAdapter, referencedAdapter);
        }
    }

    public void removeAllAssociations(final ObjectAdapter ownerAdapter) {
        final CollectionClearFacet facet = getFacet(CollectionClearFacet.class);
        facet.clear(ownerAdapter);
    }

    @Override
    public void clearCollection(final ObjectAdapter ownerAdapter) {
        if (readWrite()) {
            final CollectionClearFacet facet = getFacet(CollectionClearFacet.class);
            facet.clear(ownerAdapter);
        }
    }

    // /////////////////////////////////////////////////////////////
    // defaults
    // /////////////////////////////////////////////////////////////

    @Override
    public ObjectAdapter getDefault(final ObjectAdapter ownerAdapter) {
        return null;
    }

    @Override
    public void toDefault(final ObjectAdapter ownerAdapter) {}

    
    // /////////////////////////////////////////////////////////////
    // options (choices)
    // /////////////////////////////////////////////////////////////

    @Override
    public ObjectAdapter[] getChoices(final ObjectAdapter ownerAdapter) {
        return new ObjectAdapter[0];
    }

    @Override
    public boolean hasChoices() {
        return false;
    }



    // /////////////////////////////////////////////////////////////
    // getInstance
    // /////////////////////////////////////////////////////////////
    
    @Override
    public Instance getInstance(ObjectAdapter adapter) {
        OneToManyAssociation specification = this;
        return adapter.getInstance(specification);
    }
    

    // /////////////////////////////////////////////////////////////
    // debug, toString
    // /////////////////////////////////////////////////////////////

    @Override
    public String debugData() {
        final DebugString debugString = new DebugString();
        debugString.indent();
        debugString.indent();
        reflectiveAdapter.debugData(debugString);
        return debugString.toString();
    }

    @Override
    public String toString() {
        final ToString str = new ToString(this);
        str.append(super.toString());
        str.append(",");
        str.append("persisted", !isNotPersisted());
        str.append("type", getSpecification() == null ? "unknown" : getSpecification().getShortName());
        return str.toString();
    }





}
