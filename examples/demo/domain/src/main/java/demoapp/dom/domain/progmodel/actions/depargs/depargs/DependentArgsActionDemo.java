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
package demoapp.dom.domain.progmodel.actions.depargs.depargs;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.inject.Named;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.LabelPosition;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.applib.value.Markup;

import lombok.Getter;
import lombok.Setter;

import demoapp.dom._infra.asciidocdesc.HasAsciiDocDescription;

@XmlRootElement(name = "Demo")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@Named("demo.DependentArgs")
@DomainObject(nature=Nature.VIEW_MODEL, editing=Editing.ENABLED)
public class DependentArgsActionDemo implements HasAsciiDocDescription {

    @ObjectSupport public String title() {
        return "Dependent Arguments Demo";
    }

    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(describedAs = "Default for first dialog paramater in 'Choices', 'Auto Complete' and 'Default'")
    @Getter @Setter
    private Parity dialogParityDefault = null;

    @Property
    @PropertyLayout(describedAs = "Default for first dialog paramater in 'Hide' and 'Disable'")
    @Getter @Setter
    private boolean dialogCheckboxDefault = false;

    @Property
    @PropertyLayout(labelPosition=LabelPosition.NONE)
    public Markup getDependentText1() {
        return new Markup("Click one of above actions to see how dependent arguments work. "
                + "Set defaults for the first dialog parameter here:");
    }

    @Property
    @PropertyLayout(labelPosition=LabelPosition.NONE)
    public Markup getDependentText2() {
        return new Markup("Click one of above actions to see how dependent arguments work. "
                + "Set defaults for the first dialog parameter here:");
    }

    @Property
    @PropertyLayout(labelPosition=LabelPosition.NONE)
    public Markup getIndependentText() {
        return new Markup("Click this action above to see independent arguments do not clear "
                + "each other when changing.");
    }

    @Collection
    @Getter
    private final Set<DemoItem> items = new LinkedHashSet<>();

}

