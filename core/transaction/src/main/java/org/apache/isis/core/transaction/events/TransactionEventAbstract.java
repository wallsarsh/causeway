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
package org.apache.isis.core.transaction.events;

import java.util.EventObject;

import org.springframework.transaction.TransactionStatus;

import lombok.Getter;

public abstract class TransactionEventAbstract extends EventObject {

    private static final long serialVersionUID = 1L;
    
    public enum Type {
        BEFORE_BEGIN,
        AFTER_BEGIN,
        BEFORE_COMMIT,
        AFTER_COMMIT,
        BEFORE_ROLLBACK,
        AFTER_ROLLBACK,
    }

    /**
     * Same as {@link #getSource()}.
     */
    @Getter
    private final TransactionStatus transactionStatus;

    @Getter
    private final Type type;

    public TransactionEventAbstract(
            final TransactionStatus source,
            final Type type) {
        super(source);
        this.transactionStatus = source;
        this.type = type;
    }


}
