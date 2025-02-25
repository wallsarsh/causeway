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
package org.apache.causeway.core.metamodel.facets.actions.action.semantics;

import java.util.Optional;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facets.actions.semantics.ActionSemanticsFacet;
import org.apache.causeway.core.metamodel.facets.actions.semantics.ActionSemanticsFacetAbstract;

public class ActionSemanticsFacetForActionAnnotation extends ActionSemanticsFacetAbstract {

    public static ActionSemanticsFacet create(
            final Optional<Action> actionsIfAny,
            final FacetHolder holder) {

        return actionsIfAny
                .map(Action::semantics)
                .filter(semanticsOf -> semanticsOf != SemanticsOf.NOT_SPECIFIED)
                .map(semanticsOf ->
                (ActionSemanticsFacet)new ActionSemanticsFacetForActionAnnotation(semanticsOf, holder))
                .orElse(new ActionSemanticsFacetFallbackToNonIdempotent(holder));
    }

    private ActionSemanticsFacetForActionAnnotation(SemanticsOf of, final FacetHolder holder) {
        super(of, holder);
    }

}
