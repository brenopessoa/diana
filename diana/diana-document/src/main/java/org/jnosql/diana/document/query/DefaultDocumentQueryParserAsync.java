/*
 *
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *
 */
package org.jnosql.diana.document.query;

import jakarta.nosql.QueryException;
import jakarta.nosql.document.DocumentCollectionManagerAsync;
import jakarta.nosql.document.DocumentEntity;
import jakarta.nosql.document.DocumentObserverParser;
import jakarta.nosql.document.DocumentPreparedStatementAsync;
import jakarta.nosql.document.DocumentQueryParserAsync;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class DefaultDocumentQueryParserAsync implements DocumentQueryParserAsync {

    private final SelectQueryParser select = new SelectQueryParser();
    private final DeleteQueryParser delete = new DeleteQueryParser();
    private final InsertQueryParser insert = new InsertQueryParser();
    private final UpdateQueryParser update = new UpdateQueryParser();

    @Override
    public void query(String query, DocumentCollectionManagerAsync collectionManager,
                      Consumer<List<DocumentEntity>> callBack, DocumentObserverParser observer) {

        validation(query, collectionManager, callBack, observer);
        String command = query.substring(0, 6);
        switch (command) {
            case "select":
                select.queryAsync(query, collectionManager, callBack, observer);
                return;
            case "delete":
                delete.queryAsync(query, collectionManager, callBack, observer);
                return;
            case "insert":
                insert.queryAsync(query, collectionManager, callBack, observer);
                return;
            case "update":
                update.queryAsync(query, collectionManager, callBack, observer);
                return;
            default:
                throw new QueryException(String.format("The command was not recognized at the query %s ", query));
        }
    }

    @Override
    public DocumentPreparedStatementAsync prepare(String query, DocumentCollectionManagerAsync collectionManager,
                                                  DocumentObserverParser observer) {

        validation(query, collectionManager, observer);
        String command = query.substring(0, 6);

        switch (command) {
            case "select":
                return select.prepareAsync(query, collectionManager, observer);
            case "delete":
                return delete.prepareAsync(query, collectionManager, observer);
            case "insert":
                return insert.prepareAsync(query, collectionManager, observer);
            case "update":
                return update.prepareAsync(query, collectionManager, observer);
            default:
                throw new QueryException(String.format("The command was not recognized at the query %s ", query));
        }
    }

    private void validation(String query, DocumentCollectionManagerAsync collectionManager,
                            DocumentObserverParser observer) {

        requireNonNull(query, "query is required");
        requireNonNull(observer, "observer is required");
        requireNonNull(collectionManager, "collectionManager is required");
        if (query.length() < 6) {
            throw new QueryException(String.format("The query %s is invalid", query));
        }
    }

    private void validation(String query, DocumentCollectionManagerAsync collectionManager,
                            Consumer<List<DocumentEntity>> callBack, DocumentObserverParser observer) {

        requireNonNull(query, "query is required");
        requireNonNull(collectionManager, "collectionManager is required");
        requireNonNull(callBack, "callBack is required");
        requireNonNull(observer, "observer is required");
        if (query.length() < 6) {
            throw new QueryException(String.format("The query %s is invalid", query));
        }
    }
}
