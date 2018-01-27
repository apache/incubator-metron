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
package org.apache.metron.solr.dao;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.metron.indexing.dao.ColumnMetadataDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrColumnMetadataDao implements ColumnMetadataDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Map<String, FieldType> solrTypeMap;

  static {
    Map<String, FieldType> fieldTypeMap = new HashMap<>();
    fieldTypeMap.put("string", FieldType.TEXT);
    fieldTypeMap.put("pint", FieldType.INTEGER);
    fieldTypeMap.put("plong", FieldType.LONG);
    fieldTypeMap.put("pfloat", FieldType.FLOAT);
    fieldTypeMap.put("pdouble", FieldType.DOUBLE);
    fieldTypeMap.put("boolean", FieldType.BOOLEAN);
    solrTypeMap = Collections.unmodifiableMap(fieldTypeMap);
  }

  private String zkHost;

  public SolrColumnMetadataDao(String zkHost) {
    this.zkHost = zkHost;
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> indexColumnMetadata = new HashMap<>();
    Map<String, String> previousIndices = new HashMap<>();
    Set<String> fieldBlackList = Sets.newHashSet(SolrDao.ROOT_FIELD, SolrDao.VERSION_FIELD);

    for (String index : indices) {
      CloudSolrClient client = new CloudSolrClient.Builder().withZkHost(zkHost).build();
      client.setDefaultCollection(index);
      try {
        SchemaRepresentation schemaRepresentation = new SchemaRequest().process(client)
            .getSchemaRepresentation();
        schemaRepresentation.getFields().stream().forEach(field -> {
          String name = (String) field.get("name");
          if (!fieldBlackList.contains(name)) {
            FieldType type = toFieldType((String) field.get("type"));
            if (!indexColumnMetadata.containsKey(name)) {
              indexColumnMetadata.put(name, type);

              // record the last index in which a field exists, to be able to print helpful error message on type mismatch
              previousIndices.put(name, index);
            } else {
              FieldType previousType = indexColumnMetadata.get(name);
              if (!type.equals(previousType)) {
                String previousIndexName = previousIndices.get(name);
                LOG.error(String.format(
                    "Field type mismatch: %s.%s has type %s while %s.%s has type %s.  Defaulting type to %s.",
                    index, field, type.getFieldType(),
                    previousIndexName, field, previousType.getFieldType(),
                    FieldType.OTHER.getFieldType()));
                indexColumnMetadata.put(name, FieldType.OTHER);

                // the field is defined in multiple indices with different types; ignore the field as type has been set to OTHER
                fieldBlackList.add(name);
              }
            }
          }
        });
      } catch (SolrServerException e) {
        throw new IOException(e);
      } catch (SolrException e) {
        // 400 means an index is missing so continue
        if (e.code() != 400) {
          throw new IOException(e);
        }
      }
    }
    return indexColumnMetadata;
  }

  /**
   * Converts a string type to the corresponding FieldType.
   *
   * @param type The type to convert.
   * @return The corresponding FieldType or FieldType.OTHER, if no match.
   */
  private FieldType toFieldType(String type) {
    return solrTypeMap.getOrDefault(type, FieldType.OTHER);
  }
}
