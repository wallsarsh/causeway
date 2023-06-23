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
package org.apache.causeway.client.kroviz.ui.core

import io.kvision.core.CssSize
import io.kvision.core.UNIT
import io.kvision.panel.SimplePanel
import io.kvision.tabulator.Layout
import io.kvision.tabulator.TableType
import io.kvision.tabulator.Tabulator
import io.kvision.tabulator.TabulatorOptions
import io.kvision.tabulator.js.Tabulator.CellComponent
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.apache.causeway.client.kroviz.core.event.ResourceProxy
import org.apache.causeway.client.kroviz.core.model.CollectionDM
import org.apache.causeway.client.kroviz.core.model.Exhibit
import org.apache.causeway.client.kroviz.utils.StringUtils

/**
 * access attributes from dynamic (JS) objects with varying
 * - numbers of attributes
 * - attribute types (can only be determined at runtime) and
 * - accessor names
 */
class RoTable(displayCollection: CollectionDM) : SimplePanel() {

    init {
        title = StringUtils.extractTitle(displayCollection.title)
        width = CssSize(100, UNIT.perc)
        val columns = ColumnFactory().buildColumns(displayCollection)
        val model = displayCollection.data
        val options = TabulatorOptions(
            movableColumns = true,
            height = Constants.calcHeight,
            layout = Layout.FITDATA,
            columns = columns,
            persistenceMode = false,
        )
        val tabulator = createTabulator(model, options)
        tabulator.setEventListener<Tabulator<Exhibit>> {
            cellClickTabulator = {
                // can't check cast to external interface
                val cc = it.detail as CellComponent
                val column = cc.getColumn().getField()
                if (column == "icon") {
                    val exhibit = cc.getData().asDynamic()
                    val url = exhibit["url"]
                    ResourceProxy().loadObjectByUrl(url)
                }
            }
        }
        add(tabulator)
    }

    @OptIn(InternalSerializationApi::class)
    private fun createTabulator(
        data: MutableList<Exhibit>,
        options: TabulatorOptions<dynamic>
    ): Tabulator<dynamic> {
        val dataUpdateOnEdit = true
        val className: String? = null
        val init: (Tabulator<dynamic>.() -> Unit)? = null
        val tableTypes = setOf(TableType.STRIPED, TableType.HOVER)
        val serializer = Exhibit::class.serializer()
        val tabulator = Tabulator(data, dataUpdateOnEdit, options, tableTypes, serializer = serializer)
        if (className != null)
            tabulator.addCssClass(className)
        init?.invoke(tabulator)
        return tabulator
    }

}
