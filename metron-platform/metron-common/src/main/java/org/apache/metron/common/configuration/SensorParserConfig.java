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
package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorParserConfig {

  private String parserClassName;
  private String sensorTopic;
  private String writerClassName;
  private Map<String, Object> parserConfig = new HashMap<>();

  public String getWriterClassName() {
    return writerClassName;
  }
  public void setWriterClassName(String classNames) {
    this.writerClassName = classNames;
  }

  public String getParserClassName() {
    return parserClassName;
  }

  public void setParserClassName(String parserClassName) {
    this.parserClassName = parserClassName;
  }

  public String getSensorTopic() {
    return sensorTopic;
  }

  public void setSensorTopic(String sensorTopic) {
    this.sensorTopic = sensorTopic;
  }

  public Map<String, Object> getParserConfig() {
    return parserConfig;
  }

  public void setParserConfig(Map<String, Object> parserConfig) {
    this.parserConfig = parserConfig;
  }

  public static SensorParserConfig fromBytes(byte[] config) throws IOException {
    return JSONUtils.INSTANCE.load(new String(config), SensorParserConfig.class);
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public String toString() {
    return "{" +
            "parserClassName='" + parserClassName + '\'' +
            ", sensorTopic='" + sensorTopic + '\'' +
            ", writerClassName='" + writerClassName + '\'' +
            ", parserConfig=" + parserConfig +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorParserConfig that = (SensorParserConfig) o;

    if (getParserClassName() != null ? !getParserClassName().equals(that.getParserClassName()) : that.getParserClassName() != null)
      return false;
    if (getSensorTopic() != null ? !getSensorTopic().equals(that.getSensorTopic()) : that.getSensorTopic() != null)
      return false;
    if (writerClassName != null ? !writerClassName.equals(that.writerClassName) : that.writerClassName != null)
      return false;
    return getParserConfig() != null ? getParserConfig().equals(that.getParserConfig()) : that.getParserConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getParserClassName() != null ? getParserClassName().hashCode() : 0;
    result = 31 * result + (getSensorTopic() != null ? getSensorTopic().hashCode() : 0);
    result = 31 * result + (writerClassName != null ? writerClassName.hashCode() : 0);
    result = 31 * result + (getParserConfig() != null ? getParserConfig().hashCode() : 0);
    return result;
  }
}
