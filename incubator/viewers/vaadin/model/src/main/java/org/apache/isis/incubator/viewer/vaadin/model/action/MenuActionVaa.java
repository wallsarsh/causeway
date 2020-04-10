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
package org.apache.isis.incubator.viewer.vaadin.model.action;

import com.vaadin.flow.component.Component;

import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.viewer.common.model.action.ActionUiModel;
import org.apache.isis.viewer.common.model.actionlink.ActionLinkFactory;
import org.apache.isis.viewer.common.model.object.ObjectUiModel;

public class MenuActionVaa extends ActionUiModel<Component> {

    public MenuActionVaa(
            final ActionLinkFactory<Component> actionLinkFactory,
            final String actionName, 
            final ObjectAction objectAction,
            final ObjectUiModel actionHolder) {
        
        super(actionLinkFactory, actionName, objectAction, actionHolder);
    }
    
}
