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
package org.apache.metron.writer;

import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterMessage;

import java.util.List;

/**
 * This interface is used by the {@link org.apache.metron.writer.BulkWriterComponent} to determine if a batch should be flushed.
 * @param <MESSAGE_T> Message type
 */
public interface FlushPolicy<MESSAGE_T> {

  /**
   * This method is called whenever messages are passed to {@link org.apache.metron.writer.BulkWriterComponent#write(String, String, Object, BulkMessageWriter, WriterConfiguration)}.
   * Each implementation of {@link org.apache.metron.writer.FlushPolicy#shouldFlush(String, WriterConfiguration, List)} will be called in order
   * and the first one to return true will trigger a flush and continue on.
   * @param sensorType
   * @param configurations
   * @param messages
   * @return
   */
  boolean shouldFlush(String sensorType, WriterConfiguration configurations, List<BulkWriterMessage<MESSAGE_T>> messages);

  /**
   * This method is used to clear any internal state a {@link org.apache.metron.writer.FlushPolicy} maintains to determine if a batch should be flushed.
   * This method is called for all {@link org.apache.metron.writer.FlushPolicy} implementations after a batch is flushed with
   * {@link org.apache.metron.writer.BulkWriterComponent#flush(String, BulkMessageWriter, WriterConfiguration, List)}.
   * @param sensorType
   */
  void reset(String sensorType);
}
