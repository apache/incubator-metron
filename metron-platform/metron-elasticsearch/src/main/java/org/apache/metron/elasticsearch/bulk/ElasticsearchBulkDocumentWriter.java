/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.elasticsearch.bulk;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Writes documents to an Elasticsearch index in bulk.
 *
 * @param <D> The type of document to write.
 */
public class ElasticsearchBulkDocumentWriter<D extends Document> implements BulkDocumentWriter<D> {

    /**
     * A {@link Document} along with the index it will be written to.
     */
    private class Indexable {
        D document;
        String index;

        public Indexable(D document, String index) {
            this.document = document;
            this.index = index;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Optional<SuccessListener> onSuccess;
    private Optional<FailureListener> onFailure;
    private ElasticsearchClient client;
    private List<Indexable> documents;

    public ElasticsearchBulkDocumentWriter(ElasticsearchClient client) {
        this.client = client;
        this.onSuccess = Optional.empty();
        this.onFailure = Optional.empty();
        this.documents = new ArrayList<>();
    }

    @Override
    public void onSuccess(SuccessListener<D> onSuccess) {
        this.onSuccess = Optional.of(onSuccess);
    }

    @Override
    public void onFailure(FailureListener<D> onFailure) {
        this.onFailure = Optional.of(onFailure);
    }

    @Override
    public void addDocument(D document, String index) {
        documents.add(new Indexable(document, index));
        LOG.debug("Adding document to batch; document={}, index={}", document, index);
    }

    @Override
    public void write() {
        try {
            // create an index request for each document
            List<DocWriteRequest> requests = documents
                    .stream()
                    .map(ix -> createRequest(ix.document, ix.index))
                    .collect(Collectors.toList());

            // submit one bulk request and handle the response
            BulkResponse bulkResponse = client.getHighLevelClient().bulk(new BulkRequest().add(requests));
            List<D> successful = handleBulkResponse(bulkResponse, documents);

            // notify the success callback
            onSuccess.ifPresent(listener -> listener.onSuccess(successful));
            LOG.debug("Wrote document(s) to Elasticsearch; batchSize={}, success={}, failed={}, took={} ms",
                    documents.size(), successful.size(), documents.size() - successful.size(), bulkResponse.getTookInMillis());

        } catch(IOException e) {
            // failed to submit bulk request; all documents failed
            if(onFailure.isPresent()) {
                for(D failed: getDocuments()) {
                    onFailure.get().onFailure(failed, e, ExceptionUtils.getRootCauseMessage(e));
                }
            }
            LOG.error("Failed to submit bulk request; all documents failed", e);

        } finally {
            // flush all documents no matter which ones succeeded or failed
            documents.clear();
        }
    }

    @Override
    public int size() {
        return documents.size();
    }

    private IndexRequest createRequest(D document, String index) {
        if(document.getTimestamp() == null) {
            throw new IllegalArgumentException("Document must contain the timestamp");
        }

        return new IndexRequest()
                .source(document.getDocument())
                .type(document.getSensorType() + "_doc")
                .index(index)
                .id(documentID(document))
                .timestamp(document.getTimestamp().toString());
    }

    /**
     * Returns the document ID that should be used to index
     * the {@link Document} in Elasticsearch.
     *
     * @param document The document to index.
     * @return The document ID.
     */
    private String documentID(D document) {
        String documentID;
        if(document.getDocumentID().isPresent()) {
            documentID = document.getDocumentID().get();
            LOG.debug("Updating an existing document with known doc ID; docID={}, guid={}, sensorType={}",
                    documentID, document.getGuid(), document.getSensorType());

        } else {
            documentID = null;
            LOG.debug("Creating document and allow Elasticsearch to auto-generate the doc ID; guid={}, sensorType={}",
                    document.getGuid(), document.getSensorType());
        }
        return documentID;
    }

    /**
     * Handles the {@link BulkResponse} received from Elasticsearch.
     * @param bulkResponse The response received from Elasticsearch.
     * @param documents The documents included in the bulk request.
     * @return The documents that were successfully written. Failed documents are excluded.
     */
    private List<D> handleBulkResponse(BulkResponse bulkResponse, List<Indexable> documents) {
        List<D> successful = new ArrayList<>();

        // interrogate the response to distinguish between those that succeeded and those that failed
        Iterator<BulkItemResponse> iterator = bulkResponse.iterator();
        while(iterator.hasNext()) {
            BulkItemResponse response = iterator.next();
            if(response.isFailed()) {
                // request failed
                D failed = getDocument(response.getItemId());
                Exception cause = response.getFailure().getCause();
                String message = response.getFailureMessage();
                onFailure.ifPresent(listener -> listener.onFailure(failed, cause, message));
                LOG.error(format("Failed to write message; error=%s", message), cause);

            } else {
                // request succeeded
                D success = getDocument(response.getItemId());
                success.setDocumentID(response.getResponse().getId());
                successful.add(success);
            }
        }

        return successful;
    }

    private List<D> getDocuments() {
        return documents.stream().map(ix -> ix.document).collect(Collectors.toList());
    }

    private D getDocument(int index) {
        return documents.get(index).document;
    }
}