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


package org.apache.isis.core.progmodel.facets.actions.choices;

import org.apache.isis.core.metamodel.facets.Facet;
import org.apache.isis.core.metamodel.facets.FacetAbstract;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.actions.choices.ActionParameterChoicesFacet;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMap;
import org.apache.isis.core.metamodel.runtimecontext.SpecificationLookup;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;


public abstract class ActionParameterChoicesFacetAbstract extends FacetAbstract implements ActionParameterChoicesFacet {

    public static Class<? extends Facet> type() {
        return ActionParameterChoicesFacet.class;
    }

    private final SpecificationLookup specificationLookup;
    private final AdapterMap adapterMap;

    public ActionParameterChoicesFacetAbstract(
        final FacetHolder holder, final SpecificationLookup specificationLookup, final AdapterMap adapterManager) {
        super(type(), holder, false);
        this.specificationLookup = specificationLookup;
        this.adapterMap = adapterManager;
    }

    protected ObjectSpecification getSpecification(Class<?> type) {
        return type != null? getSpecificationLookup().loadSpecification(type): null;
    }
    
    
    ///////////////////////////////////////////////////////////
    // Dependencies
    ///////////////////////////////////////////////////////////
    
    protected SpecificationLookup getSpecificationLookup() {
        return specificationLookup;
    }
    
    protected AdapterMap getAdapterMap() {
        return adapterMap;
    }
    
}


