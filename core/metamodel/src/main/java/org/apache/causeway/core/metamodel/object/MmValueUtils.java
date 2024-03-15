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
package org.apache.causeway.core.metamodel.object;

import java.util.Optional;

import org.springframework.lang.Nullable;

import org.apache.causeway.applib.value.semantics.TemporalSupport;
import org.apache.causeway.applib.value.semantics.TemporalSupport.TemporalDecomposition;
import org.apache.causeway.applib.value.semantics.ValueSemanticsProvider;
import org.apache.causeway.core.metamodel.facets.object.value.ValueFacet;
import org.apache.causeway.core.metamodel.spec.ObjectSpecification;
import org.apache.causeway.core.metamodel.spec.feature.ObjectFeature;

import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MmValueUtils {

    // -- CONTEXT FACTORIES

    //TODO[CAUSEWAY-3489] ever used ?
    public Optional<ValueSemanticsProvider.Context> createValueSemanticsContext(
            final @Nullable ObjectFeature feature,
            final @Nullable ObjectSpecification elementType) {
        return valueFacet(elementType)
                .map(valueFacet->valueFacet.createValueSemanticsContext(feature));
    }

    //TODO[CAUSEWAY-3489] ever used ?
    public Optional<ValueSemanticsProvider.Context> createValueSemanticsContext(
            final @Nullable ObjectFeature feature,
            final @Nullable ManagedObject valueObject) {
        return valueFacet(valueObject)
                .map(valueFacet->valueFacet.createValueSemanticsContext(feature));
    }

    // -- TEMPORAL DECOMPOSITION

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Optional<TemporalDecomposition> temporalDecomposition(
            final @Nullable ObjectFeature objectFeature,
            final @Nullable ManagedObject valueObject) {
        return valueFacet(valueObject)
                .filter(valueFacet->!ManagedObjects.isNullOrUnspecifiedOrEmpty(valueObject))
                .flatMap(valueFacet->(Optional<TemporalSupport>)valueFacet.selectTemporalSupportForFeature(objectFeature))
                .flatMap((final TemporalSupport temporalDecomposer)->{
                    val pojo = MmUnwrapUtils.single(valueObject);
                    return temporalDecomposer.decomposeTemporal(pojo);
                });
    }

    // -- HELPER

    @SuppressWarnings({ "rawtypes" })
    private Optional<ValueFacet> valueFacet(final @Nullable ObjectSpecification elementType) {
        return elementType!=null
                ? elementType.valueFacet()
                : Optional.empty();
    }

    @SuppressWarnings({ "rawtypes" })
    private Optional<ValueFacet> valueFacet(final @Nullable ManagedObject valueObject) {
        if(!ManagedObjects.isSpecified(valueObject)) {
            return Optional.empty();
        }
        return valueFacet(valueObject.getSpecification());
    }

}
