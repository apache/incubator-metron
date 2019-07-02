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
package org.apache.metron.elasticsearch.dao;

import org.apache.metron.elasticsearch.bulk.BulkDocumentWriterResults;
import org.apache.metron.elasticsearch.bulk.ElasticsearchBulkDocumentWriter;
import org.apache.metron.elasticsearch.bulk.WriteFailure;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.elasticsearch.action.support.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class ElasticsearchUpdateDao implements UpdateDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private AccessConfig accessConfig;
  private ElasticsearchRetrieveLatestDao retrieveLatestDao;
  private ElasticsearchBulkDocumentWriter<Document> documentWriter;

  public ElasticsearchUpdateDao(ElasticsearchClient client,
      AccessConfig accessConfig,
      ElasticsearchRetrieveLatestDao searchDao) {
    this.accessConfig = accessConfig;
    this.retrieveLatestDao = searchDao;
    this.documentWriter = new ElasticsearchBulkDocumentWriter<>(client)
            .withRefreshPolicy(WriteRequest.RefreshPolicy.NONE);
  }

  @Override
  public Document update(Document update, Optional<String> index) throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(update, index);

    Map<Document, Optional<String>> results = batchUpdate(updates);
    return results.keySet().iterator().next();
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    Map<String, Object> globalConfig = accessConfig.getGlobalConfigSupplier().get();
    String indexPostfix = ElasticsearchUtils.getIndexFormat(globalConfig).format(new Date());

    for (Map.Entry<Document, Optional<String>> entry : updates.entrySet()) {
      Document document = entry.getKey();
      Optional<String> optionalIndex = entry.getValue();
      String indexName = optionalIndex.orElse(getIndexName(document, indexPostfix));
      documentWriter.addDocument(document, indexName);
    }

    // write the documents. if any document fails, raise an exception.
    BulkDocumentWriterResults<Document> results = documentWriter.write();
    int failures = results.getFailures().size();
    if(failures > 0) {
      int successes = results.getSuccesses().size();
      String msg = format("Failed to update all documents; %d successes, %d failures", successes, failures);
      LOG.error(msg);

      // log each individual failure
      for(WriteFailure<Document> failure: results.getFailures()) {
        LOG.error(failure.getMessage(), failure.getCause());
      }

      // raise an exception using the first exception as the root cause, although there may be many
      Throwable cause = results.getFailures().get(0).getCause();
      throw new IOException(msg, cause);
    }

    return updates;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return addCommentToAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return removeCommentFromAlert(request, latest);
  }

  public ElasticsearchUpdateDao withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
    documentWriter.withRefreshPolicy(refreshPolicy);
    return this;
  }

  protected String getIndexName(Document update, String indexPostFix) throws IOException {
    return findIndexNameByGUID(update.getGuid(), update.getSensorType())
            .orElse(ElasticsearchUtils.getIndexName(update.getSensorType(), indexPostFix, null));
  }

  protected Optional<String> findIndexNameByGUID(String guid, String sensorType) throws IOException {
    return retrieveLatestDao.searchByGuid(
            guid,
            sensorType,
            hit -> Optional.ofNullable(hit.getIndex()));
  }
}
