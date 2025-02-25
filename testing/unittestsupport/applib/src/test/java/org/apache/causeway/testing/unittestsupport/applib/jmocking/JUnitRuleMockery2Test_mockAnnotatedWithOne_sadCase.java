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
package org.apache.causeway.testing.unittestsupport.applib.jmocking;

import org.jmock.auto.Mock;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.causeway.testing.unittestsupport.applib.jmocking.JUnitRuleMockery2.ClassUnderTest;
import org.apache.causeway.testing.unittestsupport.applib.jmocking.JUnitRuleMockery2.Mode;
import org.apache.causeway.testing.unittestsupport.applib.jmocking.JUnitRuleMockery2.One;

public class JUnitRuleMockery2Test_mockAnnotatedWithOne_sadCase {

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(Mode.INTERFACES_AND_CLASSES);

    @One
    @Mock
    private Collaborator collaborator;

    @ClassUnderTest
    private CollaboratingUsingConstructorInjection collaborating;

    // no longer necessary :-)
    //    @Before
    //	public void setUp() throws Exception {
    //    	collaborating = (CollaboratingUsingConstructorInjection) context.getClassUnderTest();
    //	}

    @Ignore("This isn't actually possible to test, because the test is actually thrown by the rule, which is further up the callstack than the test method")    @Test(expected=AssertionError.class)
    public void invocationOnCollaboratorIsIgnored() {
        collaborating.dontCollaborateWithCollaborator();
    }

}
