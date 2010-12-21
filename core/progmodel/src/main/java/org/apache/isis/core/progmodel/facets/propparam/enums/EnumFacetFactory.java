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

package org.apache.isis.core.progmodel.facets.propparam.enums;

import org.apache.isis.core.commons.lang.CastUtils;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.FacetUtil;
import org.apache.isis.core.metamodel.facets.MethodRemover;
import org.apache.isis.core.progmodel.facets.value.ValueUsingValueSemanticsProviderFacetFactory;

public class EnumFacetFactory<T extends Enum<T>> extends ValueUsingValueSemanticsProviderFacetFactory<T> {

    public EnumFacetFactory() {
        super(ChoicesFacet.class);
    }

    @Override
    public boolean process(Class<?> cls, MethodRemover methodRemover, FacetHolder holder) {

        if (!cls.isEnum()) {
            return false;
        }

        addFacets(new EnumValueSemanticsProvider<T>(holder, asT(cls), getConfiguration(), getContext()));
        FacetUtil.addFacet(new ChoicesFacetEnum(holder, cls.getEnumConstants()));
        return true;
    }

    protected Class<T> asT(Class<?> cls) {
        return CastUtils.cast(cls);
    }

}
