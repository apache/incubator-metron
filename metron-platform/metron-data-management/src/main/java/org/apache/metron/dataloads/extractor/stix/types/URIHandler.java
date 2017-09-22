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
package org.apache.metron.dataloads.extractor.stix.types;

import com.google.common.collect.ImmutableList;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.mitre.cybox.common_2.AnyURIObjectPropertyType;
import org.mitre.cybox.objects.URIObjectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URIHandler extends AbstractObjectTypeHandler<URIObjectType> {
  public URIHandler() {
    super(URIObjectType.class);
  }

  @Override
  public Iterable<LookupKV> extract(URIObjectType type, Map<String, Object> config) throws IOException {
    List<LookupKV> ret = new ArrayList<>();
    if(type != null) {
      AnyURIObjectPropertyType val = type.getValue();
      if(val != null) {
        Object v = val.getValue();
        if(v != null) {
          final String indicatorType = getType();
          LookupKV results = new LookupKV(new EnrichmentKey(indicatorType, v.toString())
                  , new EnrichmentValue(
                  new HashMap<String, Object>() {{
                    put("source-type", "STIX");
                    put("uri", v.toString());
                    put("indicator-type", indicatorType);
                    put("source", type.toXMLString());
                  }}
          )
          );
          ret.add(results);
        }
      }
    }
    return ret;
  }

  @Override
  public List<String> getPossibleTypes() {
    return ImmutableList.of(getType());
  }
}
