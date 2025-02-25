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
package org.apache.causeway.testdomain.model.good;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Publishing;

import lombok.RequiredArgsConstructor;

/**
 * For (test) mixin descriptions see {@link ProperMemberSupport}.
 */
@Action(executionPublishing = Publishing.ENABLED)
@RequiredArgsConstructor
public class ProperMemberSupport_action5 {

    private final ProperMemberSupport mixee;

    @MemberSupport public ProperMemberSupport act() {
        return mixee;
    }

    // -- IMPERATIVE NAMING AND DESCRIBING

    @MemberSupport public String namedAct() {
        return "named-imperative[action5]";
    }

    @MemberSupport public String describedAct() {
        return "described-imperative[action5]";
    }

}
