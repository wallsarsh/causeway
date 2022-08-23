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
package org.apache.isis.persistence.jdo.datanucleus.valuetypes;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.jdo.identity.CharIdentity;

import org.springframework.stereotype.Component;

import org.apache.isis.applib.annotation.PriorityPrecedence;
import org.apache.isis.applib.services.bookmark.IdStringifier;
import org.apache.isis.applib.util.schema.CommonDtoUtils;
import org.apache.isis.applib.value.semantics.ValueDecomposition;
import org.apache.isis.applib.value.semantics.ValueSemanticsBasedOnIdStringifierWithTargetEntityClassSupport;
import org.apache.isis.commons.internal.factory._InstanceUtil;
import org.apache.isis.schema.common.v2.ValueType;

import lombok.Builder;
import lombok.NonNull;
import lombok.val;

@Component
@Priority(PriorityPrecedence.LATE)
public class JdoCharIdentityValueSemantics
extends ValueSemanticsBasedOnIdStringifierWithTargetEntityClassSupport<CharIdentity> {

    @Inject IdStringifier<Character> idStringifierForCharacter;

    public JdoCharIdentityValueSemantics() {
        super(CharIdentity.class);
    }

    /**
     * for testing only
     */
    @Builder
    JdoCharIdentityValueSemantics(final IdStringifier<Character> idStringifierForCharacter) {
        this();
        this.idStringifierForCharacter = idStringifierForCharacter;
    }

    // -- COMPOSER

    @Override
    public ValueDecomposition decompose(final CharIdentity value) {
        return CommonDtoUtils.typedTupleBuilder(value)
                .addFundamentalType(ValueType.STRING, "targetClassName", CharIdentity::getTargetClassName)
                .addFundamentalType(ValueType.STRING, "key", this::enstring)
                .buildAsDecomposition();
    }

    @Override
    public CharIdentity compose(final ValueDecomposition decomposition) {
        val elementMap = CommonDtoUtils.typedTupleAsMap(decomposition.rightIfAny());
        final String targetClassName = (String)elementMap.get("targetClassName");
        final String key = (String)elementMap.get("key");
        return destring(key, _InstanceUtil.loadClass(targetClassName));
    }

    // -- ID STRINGIFIER

    @Override
    public String enstring(final @NonNull CharIdentity value) {
        return idStringifierForCharacter.enstring(value.getKey());
    }

    @Override
    public CharIdentity destring(
            final @NonNull String stringified,
            final @NonNull Class<?> targetEntityClass) {
        val idValue = idStringifierForCharacter.destring(stringified);
        return new CharIdentity(targetEntityClass, idValue);
    }

}
