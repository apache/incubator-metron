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
package org.apache.metron.writer.bolt;

import org.apache.metron.common.bolt.ConfiguredIndexingBolt;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import static org.apache.storm.utils.TupleUtils.isTick;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.WriterToBulkWriter;
import org.apache.metron.writer.message.MessageGetter;
import org.apache.metron.writer.message.MessageGetters;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class BulkMessageWriterBolt extends ConfiguredIndexingBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private BulkMessageWriter<JSONObject> bulkMessageWriter;
  private BulkWriterComponent<JSONObject> writerComponent;
  private String messageGetterStr = MessageGetters.NAMED.name();
  private transient MessageGetter messageGetter = null;
  private transient OutputCollector collector;
  private transient Function<WriterConfiguration, WriterConfiguration> configurationTransformation = null;
  private BatchTimeoutHelper timeoutHelper = null;
  private int batchTimeoutDivisor = 1;

  public BulkMessageWriterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public BulkMessageWriterBolt withBulkMessageWriter(BulkMessageWriter<JSONObject > bulkMessageWriter) {
    this.bulkMessageWriter = bulkMessageWriter;
    return this;
  }

  public BulkMessageWriterBolt withMessageWriter(MessageWriter<JSONObject> messageWriter) {
    this.bulkMessageWriter = new WriterToBulkWriter<>(messageWriter);
    return this;
  }

  public BulkMessageWriterBolt withMessageGetter(String messageGetter) {
    this.messageGetterStr = messageGetter;
    return this;
  }

  /**
   * If this BulkMessageWriterBolt is in a topology where it is daisy-chained with
   * other queuing Writers, then the max amount of time it takes for a tuple
   * to clear the whole topology is the sum of all the batchTimeouts for all the
   * daisy-chained Writers.  In the common case where each Writer is using the default
   * batchTimeout, it is then necessary to divide that batchTimeout by the number of
   * daisy-chained Writers.  For example, the Enrichment and Threat Intel features
   * both use a BulkMessageWriterBolt, but are in a single topology, so one would
   * initialize those Bolts withBatchTimeoutDivisor(2).  Default value, if not set, is 1.
   *
   * If non-default batchTimeouts are configured for some components, the administrator
   * will want to take this behavior into account.
   *
   * @param batchTimeoutDivisor
   * @return
   */
  public BulkMessageWriterBolt withBatchTimeoutDivisor(int batchTimeoutDivisor) {
    this.batchTimeoutDivisor = batchTimeoutDivisor;
    return this;
  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    // configure how often a tick tuple will be sent to our bolt
    if (timeoutHelper == null) {
      // Not sure if called before or after prepare(), so do some of the same stuff as prepare() does,
      // to get the valid WriterConfiguration:
      if(bulkMessageWriter instanceof WriterToBulkWriter) {
        configurationTransformation = WriterToBulkWriter.TRANSFORMATION;
      }
      else {
        configurationTransformation = x -> x;
      }
      WriterConfiguration wrconf = configurationTransformation.apply(
              new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));

      timeoutHelper = new BatchTimeoutHelper(wrconf::getAllConfiguredTimeouts, batchTimeoutDivisor);
    }
    int requestedTickFreqSecs = timeoutHelper.getRecommendedTickInterval();

    Map<String, Object> conf = super.getComponentConfiguration();
    if (conf == null) {
      conf = new HashMap<String, Object>();
    }
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, requestedTickFreqSecs);
    LOG.info("Requesting " + Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS + " set to " + Integer.toString(requestedTickFreqSecs));
    return conf;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.writerComponent = new BulkWriterComponent<>(collector);
    this.collector = collector;
    super.prepare(stormConf, context, collector);
    messageGetter = MessageGetters.valueOf(messageGetterStr);
    if(bulkMessageWriter instanceof WriterToBulkWriter) {
      configurationTransformation = WriterToBulkWriter.TRANSFORMATION;
    }
    else {
      configurationTransformation = x -> x;
    }
    try {
      bulkMessageWriter.init(stormConf
              , configurationTransformation.apply(
                      new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations())));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    try
    {
      if (isTick(tuple)) {
        if (!(bulkMessageWriter instanceof WriterToBulkWriter)) {
          //WriterToBulkWriter doesn't allow batching, so no need to flush on Tick.
          LOG.debug("Flushing message queues older than their batchTimeouts");
          writerComponent.flushTimeouts(bulkMessageWriter, configurationTransformation.apply(
                  new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations())));
        }
        return;
      }

      JSONObject message = messageGetter.getMessage(tuple);
      String sensorType = MessageUtils.getSensorType(message);
      LOG.trace("Writing enrichment message: {}", message);
      WriterConfiguration writerConfiguration = configurationTransformation.apply(
              new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));
      if(writerConfiguration.isDefault(sensorType)) {
        //want to warn, but not fail the tuple
        collector.reportError(new Exception("WARNING: Default and (likely) unoptimized writer config used for " + bulkMessageWriter.getName() + " writer and sensor " + sensorType));
      }
      writerComponent.write(sensorType
                           , tuple
                           , message
                           , bulkMessageWriter
                           , writerConfiguration
                           );
    }
    catch(Exception e) {
      throw new RuntimeException("This should have been caught in the writerComponent.  If you see this, file a JIRA", e);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
