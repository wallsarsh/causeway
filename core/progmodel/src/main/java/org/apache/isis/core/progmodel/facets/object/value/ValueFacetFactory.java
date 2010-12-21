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


package org.apache.isis.core.progmodel.facets.object.value;

import org.apache.isis.applib.adapters.EncoderDecoder;
import org.apache.isis.applib.annotation.Value;
import org.apache.isis.core.commons.lang.StringUtils;
import org.apache.isis.core.metamodel.config.IsisConfiguration;
import org.apache.isis.core.metamodel.config.IsisConfigurationAware;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.FacetUtil;
import org.apache.isis.core.metamodel.facets.MethodRemover;
import org.apache.isis.core.metamodel.facets.object.aggregated.AggregatedFacet;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.facets.object.immutable.ImmutableFacet;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.facets.object.value.ValueFacet;
import org.apache.isis.core.metamodel.java5.AnnotationBasedFacetFactoryAbstract;
import org.apache.isis.core.metamodel.runtimecontext.AuthenticationSessionProvider;
import org.apache.isis.core.metamodel.runtimecontext.AuthenticationSessionProviderAware;
import org.apache.isis.core.metamodel.runtimecontext.DependencyInjector;
import org.apache.isis.core.metamodel.runtimecontext.DependencyInjectorAware;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMap;
import org.apache.isis.core.metamodel.runtimecontext.AdapterMapAware;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeatureType;
import org.apache.isis.core.progmodel.facets.object.ebc.EqualByContentFacet;
import org.apache.isis.core.progmodel.facets.object.ident.icon.IconFacet;
import org.apache.isis.core.progmodel.facets.object.ident.title.TitleFacet;


/**
 * Processes the {@link Value} annotation.
 * 
 * <p>
 * As a result, will always install the following facets:
 * <ul>
 * <li> {@link TitleFacet} - based on the <tt>title()</tt> method if present, otherwise uses
 * <tt>toString()</tt></li>
 * <li> {@link IconFacet} - based on the <tt>iconName()</tt> method if present, otherwise derived from the
 * class name</li>
 * </ul>
 * <p>
 * In addition, the following facets may be installed:
 * <ul>
 * <li> {@link ParseableFacet} - if a {@link Parser} has been specified explicitly in the annotation (or is
 * picked up through an external configuration file)</li>
 * <li> {@link EncodableFacet} - if an {@link EncoderDecoder} has been specified explicitly in the annotation
 * (or is picked up through an external configuration file)</li>
 * <li> {@link ImmutableFacet} - if specified explicitly in the annotation
 * <li> {@link EqualByContentFacet} - if specified explicitly in the annotation
 * </ul>
 * <p>
 * Note that {@link AggregatedFacet} is <i>not</i> installed.
 */
public class ValueFacetFactory extends AnnotationBasedFacetFactoryAbstract implements IsisConfigurationAware, AuthenticationSessionProviderAware, AdapterMapAware, DependencyInjectorAware {

	
    private IsisConfiguration configuration;
	private AuthenticationSessionProvider authenticationSessionProvider;
	private AdapterMap adapterManager;
	private DependencyInjector dependencyInjector;
	

    public ValueFacetFactory() {
        super(ObjectFeatureType.OBJECTS_ONLY);
    }

    @Override
    public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder holder) {
        return FacetUtil.addFacet(create(cls, holder));
    }

    /**
     * Returns a {@link ValueFacet} implementation.
     */
    private ValueFacet create(final Class<?> cls, final FacetHolder holder) {

        // create from annotation, if present
        final Value annotation = getAnnotation(cls, Value.class);
        if (annotation != null) {
            final ValueFacetAnnotation facet = new ValueFacetAnnotation(cls, holder, getIsisConfiguration(), createValueSemanticsProviderContext());
            if (facet.isValid()) {
                return facet;
            }
        }

        // otherwise, try to create from configuration, if present
        final String semanticsProviderName = ValueSemanticsProviderUtil.semanticsProviderNameFromConfiguration(cls,
                configuration);
        if (!StringUtils.isNullOrEmpty(semanticsProviderName)) {
            final ValueFacetFromConfiguration facet = new ValueFacetFromConfiguration(semanticsProviderName, holder, getIsisConfiguration(), createValueSemanticsProviderContext());
            if (facet.isValid()) {
                return facet;
            }
        }

        // otherwise, no value semantic
        return null;
    }

    protected ValueSemanticsProviderContext createValueSemanticsProviderContext() {
        return new ValueSemanticsProviderContext(getAuthenticationSessionProvider(), getSpecificationLookup(), getAdapterManager(), getDependencyInjector());
    }

	// ////////////////////////////////////////////////////////////////////
    // Injected
    // ////////////////////////////////////////////////////////////////////

    public IsisConfiguration getIsisConfiguration() {
        return configuration;
    }
    @Override
    public void setIsisConfiguration(final IsisConfiguration configuration) {
        this.configuration = configuration;
    }


    public AuthenticationSessionProvider getAuthenticationSessionProvider() {
        return authenticationSessionProvider;
    }
    @Override
    public void setAuthenticationSessionProvider(AuthenticationSessionProvider authenticationSessionProvider) {
        this.authenticationSessionProvider = authenticationSessionProvider;
    }

    public AdapterMap getAdapterManager() {
        return adapterManager;
    }
    @Override
    public void setAdapterMap(AdapterMap adapterManager) {
        this.adapterManager = adapterManager;
    }

    public DependencyInjector getDependencyInjector() {
        return dependencyInjector;
    }
    @Override
    public void setDependencyInjector(DependencyInjector dependencyInjector) {
        this.dependencyInjector = dependencyInjector;
    }

}
