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
package org.apache.causeway.extensions.commandlog.applib.contributions;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.mixins.system.HasInteractionId;
import org.apache.causeway.applib.services.bookmark.BookmarkService;
import org.apache.causeway.extensions.commandlog.applib.CausewayModuleExtCommandLogApplib;
import org.apache.causeway.extensions.commandlog.applib.dom.CommandLogEntry;
import org.apache.causeway.extensions.commandlog.applib.dom.CommandLogEntryRepositoryAbstract;

import lombok.RequiredArgsConstructor;

/**
 * Contributes a <tt>recentCommands</tt> action to any domain object
 * (unless also {@link HasInteractionId} - commands don't themselves have commands).
 *
 * @since 2.0 {@index}
 */
@Action(
        commandPublishing = Publishing.DISABLED,
        domainEvent = Object_recentCommands.ActionDomainEvent.class,
        executionPublishing = Publishing.DISABLED,
        semantics = SemanticsOf.SAFE
)
@ActionLayout(
        cssClassFa = "fa-bolt",
        describedAs = "Returns the last (published) 30 commands that have either targeted this object, or that have returned this object as their result",
        position = ActionLayout.Position.PANEL_DROPDOWN,
        fieldSetId = LayoutConstants.FieldSetId.METADATA,
        sequence = "900.1"
)
@RequiredArgsConstructor
public class Object_recentCommands {

    public static class ActionDomainEvent
            extends CausewayModuleExtCommandLogApplib.ActionDomainEvent<Object_recentCommands> { }

    private final Object domainObject;

    @MemberSupport public List<? extends CommandLogEntry> act() {
        return bookmarkService.bookmarkFor(domainObject)
        .map(commandLogEntryRepository::findRecentByTargetOrResult)
        .orElse(Collections.emptyList());
    }

    /**
     * Hide if the mixee itself implements {@link HasInteractionId}.
     * (commands don't have commands).
     */
    @MemberSupport public boolean hideAct() {
        return (domainObject instanceof HasInteractionId);
    }

    @Inject
    CommandLogEntryRepositoryAbstract<? extends CommandLogEntry> commandLogEntryRepository;
    @Inject BookmarkService bookmarkService;
}
