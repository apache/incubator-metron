/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.bolt.mapper.ColumnList;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Writes ProfileMeasurement values that can be read during automated testing.
 */
public class ProfileWriter {

  private RowKeyBuilder rowKeyBuilder;
  private ColumnBuilder columnBuilder;
  private HBaseClient hbaseClient;
  private HBaseProfilerClient client;

  public ProfileWriter(RowKeyBuilder rowKeyBuilder, ColumnBuilder columnBuilder, HTableInterface table, long periodDurationMillis) {
    this.rowKeyBuilder = rowKeyBuilder;
    this.columnBuilder = columnBuilder;
    this.hbaseClient = new HBaseClient((c, t) -> table, table.getConfiguration(), table.getName().getNameAsString());
    this.client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder, periodDurationMillis);
  }

  /**
   * Writes profile measurements that can be used for testing.
   *
   * @param prototype      A prototype for the types of ProfileMeasurements that should be written.
   * @param count          The number of profile measurements to write.
   * @param group          The name of the group.
   * @param valueGenerator A function that consumes the previous ProfileMeasurement value and produces the next.
   */
  public void write(ProfileMeasurement prototype, int count, List<Object> group, Function<Object, Object> valueGenerator) {

    ProfileMeasurement m = prototype;
    for(int i=0; i<count; i++) {

      // generate the next value that should be written
      Object nextValue = valueGenerator.apply(m.getValue());

      // create a measurement for the next profile period to be written
      ProfilePeriod next = m.getPeriod().next();
      m = new ProfileMeasurement()
              .withProfileName(prototype.getProfileName())
              .withEntity(prototype.getEntity())
              .withPeriod(next.getStartTimeMillis(), prototype.getPeriod().getDurationMillis(), TimeUnit.MILLISECONDS)
              .withGroups(group)
              .withValue(nextValue);
      write(m);
    }
  }

  /**
   * Write a ProfileMeasurement.
   * @param m The ProfileMeasurement to write.
   */
  private void write(ProfileMeasurement m) {

    byte[] rowKey = rowKeyBuilder.rowKey(m);
    ColumnList cols = columnBuilder.columns(m);

    hbaseClient.addMutation(rowKey, cols, Durability.SKIP_WAL);
    hbaseClient.mutate();
  }

  public static void main(String[] args) throws Exception {
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder();

    Configuration config = HBaseConfiguration.create();
    config.set("hbase.master.hostname", "node1");
    config.set("hbase.regionserver.hostname", "node1");
    config.set("hbase.zookeeper.quorum", "node1");

    HTableProvider provider = new HTableProvider();
    HTableInterface table = provider.getTable(config, "profiler");

    long periodDuration = 15;
    TimeUnit periodUnits = TimeUnit.MINUTES;

    long when = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2);
    ProfileMeasurement measure = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("192.168.66.121")
            .withPeriod(when, periodDuration, periodUnits);

    ProfileWriter writer = new ProfileWriter(rowKeyBuilder, columnBuilder, table, periodUnits.toMillis(periodDuration));
    writer.write(measure, 2 * 24 * 4, Collections.emptyList(), val -> new Random().nextInt(10));
  }
}
