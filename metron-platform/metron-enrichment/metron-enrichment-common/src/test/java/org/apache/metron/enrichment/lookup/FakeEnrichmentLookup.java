/*
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
package org.apache.metron.enrichment.lookup;

import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.handler.KeyWithContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link EnrichmentLookup} useful for testing.
 *
 * <p>Maintains a static, in-memory set of enrichments to mimic the behavior of
 * an {@link EnrichmentLookup} that interacts with HBase.
 */
public class FakeEnrichmentLookup extends EnrichmentLookup implements Serializable {

  /**
   * The available enrichments.  This is static so that all
   * instances 'see' the same set of enrichments.
   */
  private static Map<EnrichmentKey, EnrichmentValue> enrichments = Collections.synchronizedMap(new HashMap<>());

  public FakeEnrichmentLookup() {
    super(null, null, null);
  }

  /**
   * Add an enrichment.
   * @param key The enrichment key.
   * @param value The enrichment value.
   * @return
   */
  public FakeEnrichmentLookup withEnrichment(EnrichmentKey key, EnrichmentValue value) {
    this.enrichments.put(key, value);
    return this;
  }

  /**
   * Deletes all enrichments.
   */
  public FakeEnrichmentLookup deleteAll() {
    enrichments.clear();
    return this;
  }

  @Override
  public boolean exists(EnrichmentKey key, HBaseContext context, boolean logAccess) {
    return enrichments.containsKey(key);
  }

  @Override
  public Iterable<Boolean> exists(Iterable<KeyWithContext<EnrichmentKey, HBaseContext>> keys, boolean logAccess) throws IOException {
    List<Boolean> results = new ArrayList<>();
    for(KeyWithContext<EnrichmentKey, HBaseContext> keyWithContext: keys) {
      EnrichmentKey key = keyWithContext.getKey();
      results.add(enrichments.containsKey(key));
    }
    return results;
  }

  @Override
  public LookupKV<EnrichmentKey, EnrichmentValue> get(EnrichmentKey key, HBaseContext context, boolean logAccess) {
    EnrichmentValue value = enrichments.get(key);
    return new LookupKV<>(key, value);
  }

  @Override
  public Iterable<LookupKV<EnrichmentKey, EnrichmentValue>> get(Iterable<KeyWithContext<EnrichmentKey, HBaseContext>> keys, boolean logAccess) throws IOException {
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for(KeyWithContext<EnrichmentKey, HBaseContext> keyWithContext: keys) {
      EnrichmentKey key = keyWithContext.getKey();
      HBaseContext context = keyWithContext.getContext();
      if(enrichments.containsKey(key)) {
        results.add(get(key, context, logAccess));
      }
    }
    return results;
  }



  @Override
  public void close() throws IOException {
    // nothing to do
  }
}
