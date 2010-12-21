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


package org.apache.isis.core.progmodel.facets.object.parseable;

import org.apache.isis.applib.annotation.Parseable;
import org.apache.isis.core.commons.lang.StringUtils;
import org.apache.isis.core.metamodel.config.IsisConfiguration;
import org.apache.isis.core.metamodel.config.IsisConfigurationAware;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.FacetUtil;
import org.apache.isis.core.metamodel.facets.MethodRemover;
import org.apache.isis.core.metamodel.java5.AnnotationBasedFacetFactoryAbstract;
import org.apache.isis.core.metamodel.runtimecontext.AuthenticationSessionProvider;
import org.apache.isis.core.metamodel.runtimecontext.AuthenticationSessionProviderAware;
import org.apache.isis.core.metamodel.runtimecontext.DependencyInjector;
import org.apache.isis.core.metamodel.runtimecontext.DependencyInjectorAware;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMap;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMapAware;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeatureType;


public class ParseableFacetFactory extends AnnotationBasedFacetFactoryAbstract implements IsisConfigurationAware, AuthenticationSessionProviderAware, AdapterMapAware, DependencyInjectorAware {

    private IsisConfiguration configuration;

    private AuthenticationSessionProvider authenticationSessionProvider; 
    private AdapterMap adapterManager;
    private DependencyInjector dependencyInjector;
    
    public ParseableFacetFactory() {
        super(ObjectFeatureType.OBJECTS_ONLY);
    }

    @Override
    public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder holder) {
        return FacetUtil.addFacet(create(cls, holder));
    }

    private ParseableFacetAbstract create(final Class<?> cls, final FacetHolder holder) {
        final Parseable annotation = getAnnotation(cls, Parseable.class);

        // create from annotation, if present
        if (annotation != null) {
            final ParseableFacetAnnotation facet = new ParseableFacetAnnotation(cls, getIsisConfiguration(), holder, authenticationSessionProvider, adapterManager, dependencyInjector);
            if (facet.isValid()) {
                return facet;
            }
        }

        // otherwise, try to create from configuration, if present
        final String parserName = ParserUtil.parserNameFromConfiguration(cls, getIsisConfiguration());
        if (!StringUtils.isNullOrEmpty(parserName)) {
            final ParseableFacetFromConfiguration facet = new ParseableFacetFromConfiguration(parserName, holder, authenticationSessionProvider, dependencyInjector, adapterManager);
            if (facet.isValid()) {
                return facet;
            }
        }

        return null;
    }


	// ////////////////////////////////////////////////////////////////////
    // Dependencies (injected via setters since *Aware)
    // ////////////////////////////////////////////////////////////////////

    public IsisConfiguration getIsisConfiguration() {
        return configuration;
    }
    /**
     * Injected since {@link IsisConfigurationAware}.
     */
    @Override
    public void setIsisConfiguration(final IsisConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void setAuthenticationSessionProvider(AuthenticationSessionProvider authenticationSessionProvider) {
        this.authenticationSessionProvider = authenticationSessionProvider;
    }
    @Override
    public void setAdapterMap(AdapterMap adapterManager) {
        this.adapterManager = adapterManager;
    }
    @Override
    public void setDependencyInjector(DependencyInjector dependencyInjector) {
        this.dependencyInjector = dependencyInjector;
    }
	
}
