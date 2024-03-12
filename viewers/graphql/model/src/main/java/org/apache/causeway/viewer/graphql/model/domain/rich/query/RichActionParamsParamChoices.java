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
package org.apache.causeway.viewer.graphql.model.domain.rich.query;

 import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.ParameterNegotiationModel;
import org.apache.causeway.core.metamodel.object.ManagedObject;
 import org.apache.causeway.core.metamodel.spec.feature.ObjectFeature;
 import org.apache.causeway.viewer.graphql.model.context.Context;
import org.apache.causeway.viewer.graphql.model.domain.Environment;
import org.apache.causeway.viewer.graphql.model.domain.Element;
import org.apache.causeway.viewer.graphql.model.domain.common.interactors.ActionParamInteractor;
 import org.apache.causeway.viewer.graphql.model.fetcher.BookmarkedPojo;
import org.apache.causeway.viewer.graphql.model.types.TypeMapper;

import lombok.val;
import lombok.extern.log4j.Log4j2;

 @Log4j2
 public class RichActionParamsParamChoices extends Element {

     private final ActionParamInteractor actionParamInteractor;

     public RichActionParamsParamChoices(
             final ActionParamInteractor actionParamInteractor,
             final Context context) {
         super(context);
         this.actionParamInteractor = actionParamInteractor;

         val objectActionParameter = actionParamInteractor.getObjectActionParameter();
         if (objectActionParameter.hasChoices()) {
             val elementType = objectActionParameter.getElementType();
             val fieldBuilder = newFieldDefinition()
                     .name("choices")
                     .type(GraphQLList.list(context.typeMapper.outputTypeFor(elementType, actionParamInteractor.getSchemaType())));
             actionParamInteractor.addGqlArguments(actionParamInteractor.getObjectMember(), fieldBuilder, TypeMapper.InputContext.CHOICES, actionParamInteractor.getParamNum());
             setField(fieldBuilder.build());
         } else {
             setField(null);
         }
     }

     @Override
     protected List<Object> fetchData(final DataFetchingEnvironment dataFetchingEnvironment) {

         val sourcePojo = BookmarkedPojo.sourceFrom(dataFetchingEnvironment);
         val objectSpecification = context.specificationLoader.loadSpecification(sourcePojo.getClass());
         if (objectSpecification == null) {
             return Collections.emptyList();
         }

         val objectAction = actionParamInteractor.getObjectMember();
         val managedObject = ManagedObject.adaptSingular(objectSpecification, sourcePojo);

         final ObjectFeature objectFeature = actionParamInteractor.getObjectActionParameter();
         val objectActionParameter = objectAction.getParameterById(objectFeature.asciiId());
         val argumentManagedObjects = actionParamInteractor.argumentManagedObjectsFor(new Environment.For(dataFetchingEnvironment), objectAction, context.bookmarkService);

         val managedAction = ManagedAction.of(managedObject, objectAction, Where.ANYWHERE);
         val pendingArgs = ParameterNegotiationModel.of(managedAction, argumentManagedObjects);
         val choices = objectActionParameter.getChoices(pendingArgs, InteractionInitiatedBy.USER);

         return choices.stream()
                    .map(ManagedObject::getPojo)
                    .collect(Collectors.toList());
     }

 }
