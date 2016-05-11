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
package org.apache.metron.common.bolt;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import org.apache.curator.test.TestingServer;
import org.apache.metron.common.Constants;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigType;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfiguredEnrichmentBoltTest extends BaseConfiguredBoltTest {

  private Set<String> enrichmentConfigurationTypes = new HashSet<>();
  private String zookeeperUrl;

  public static class StandAloneConfiguredEnrichmentBolt extends ConfiguredEnrichmentBolt {

    public StandAloneConfiguredEnrichmentBolt(String zookeeperUrl) {
      super(zookeeperUrl);
    }

    @Override
    public void execute(Tuple input) {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @Override
    public void reloadCallback(String name, ConfigType type) {
      configsUpdated.add(name);
    }
  }

  @Before
  public void setupConfiguration() throws Exception {
    TestingServer testZkServer = new TestingServer(true);
    this.zookeeperUrl = testZkServer.getConnectString();
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile(TestConstants.SAMPLE_CONFIG_PATH);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    enrichmentConfigurationTypes.add(Constants.GLOBAL_CONFIG_NAME);
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.ENRICHMENTS_CONFIGS_PATH);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
      enrichmentConfigurationTypes.add(sensorType);
    }
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile(TestConstants.PARSER_CONFIGS_PATH);
    for (String sensorType : sensorParserConfigs.keySet()) {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), zookeeperUrl);
    }
  }

  @Test
  public void test() throws Exception {
    EnrichmentConfigurations sampleConfigurations = new EnrichmentConfigurations();
    try {
      StandAloneConfiguredEnrichmentBolt configuredBolt = new StandAloneConfiguredEnrichmentBolt(null);
      configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
      Assert.fail("A valid zookeeper url must be supplied");
    } catch (RuntimeException e){}

    configsUpdated = new HashSet<>();
    sampleConfigurations.updateGlobalConfig(ConfigurationsUtils.readGlobalConfigFromFile(TestConstants.SAMPLE_CONFIG_PATH));
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.ENRICHMENTS_CONFIGS_PATH);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      sampleConfigurations.updateSensorEnrichmentConfig(sensorType, sensorEnrichmentConfigs.get(sensorType));
    }

    StandAloneConfiguredEnrichmentBolt configuredBolt = new StandAloneConfiguredEnrichmentBolt(zookeeperUrl);
    configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
    waitForConfigUpdate(enrichmentConfigurationTypes);
    Assert.assertEquals(sampleConfigurations, configuredBolt.configurations);

    configsUpdated = new HashSet<>();
    Map<String, Object> sampleGlobalConfig = sampleConfigurations.getGlobalConfig();
    sampleGlobalConfig.put("newGlobalField", "newGlobalValue");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(Constants.GLOBAL_CONFIG_NAME);
    Assert.assertEquals("Add global config field", sampleConfigurations.getGlobalConfig(), configuredBolt.configurations.getGlobalConfig());

    configsUpdated = new HashSet<>();
    sampleGlobalConfig.remove("newGlobalField");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(Constants.GLOBAL_CONFIG_NAME);
    Assert.assertEquals("Remove global config field", sampleConfigurations, configuredBolt.configurations);

    configsUpdated = new HashSet<>();
    String sensorType = "testSensorConfig";
    SensorEnrichmentConfig testSensorConfig = new SensorEnrichmentConfig();
    testSensorConfig.setBatchSize(50);
    testSensorConfig.setIndex("test");
    Map<String, List<String>> enrichmentFieldMap = new HashMap<>();
    enrichmentFieldMap.put("enrichmentTest", new ArrayList<String>() {{
      add("enrichmentField");
    }});
    testSensorConfig.getEnrichment().setFieldMap(enrichmentFieldMap);
    Map<String, List<String>> threatIntelFieldMap = new HashMap<>();
    threatIntelFieldMap.put("threatIntelTest", new ArrayList<String>() {{
      add("threatIntelField");
    }});
    testSensorConfig.getThreatIntel().setFieldMap(threatIntelFieldMap);
    sampleConfigurations.updateSensorEnrichmentConfig(sensorType, testSensorConfig);
    ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, testSensorConfig, zookeeperUrl);
    waitForConfigUpdate(sensorType);
    Assert.assertEquals("Add new sensor config", sampleConfigurations, configuredBolt.configurations);

    configsUpdated = new HashSet<>();
    String someConfigType = "someConfig";
    Map<String, Object> someConfig = new HashMap<>();
    someConfig.put("someField", "someValue");
    sampleConfigurations.updateConfig(someConfigType, someConfig);
    ConfigurationsUtils.writeConfigToZookeeper(someConfigType, someConfig, zookeeperUrl);
    waitForConfigUpdate(someConfigType);
    Assert.assertEquals("Add new misc config", sampleConfigurations, configuredBolt.configurations);
    configuredBolt.cleanup();
  }

  private void waitForConfigUpdate(final String expectedConfigUpdate) {
    waitForConfigUpdate(new HashSet<String>() {{ add(expectedConfigUpdate); }});
  }

  private void waitForConfigUpdate(Set<String> expectedConfigUpdates) {
    int count = 0;
    while (!configsUpdated.equals(expectedConfigUpdates)) {
      if (count++ > 5) {
        Assert.fail("ConfiguredBolt was not updated in time");
        return;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}