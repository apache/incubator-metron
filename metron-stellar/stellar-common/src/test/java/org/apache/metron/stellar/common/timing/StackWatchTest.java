/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.timing;

import static org.junit.Assert.*;

import java.util.ArrayList;
import org.junit.Test;

public class StackWatchTest {

  @Test
  public void testStackWatch() throws Exception {
    // General test, call three top level functions, the first of two having
    // nested calls
    StackWatch watch = new StackWatch("testStackWatch");
    // root timing
    watch.startTime("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stopTime();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit((l, n) -> {
      levels.add(l);
      System.out.println(String.format("%d -> %s : %d", l, n.getPath(), n.getTime()));
    });

    // validate that we have the right number of 'timings'
    assertEquals(levels.size(), 9);
  }

  @Test
  public void testNonStartOuter() throws Exception {
    // Test a case where we are doing timings, but don't give it an 'outer' time
    StackWatch watch = new StackWatch("testStackWatch");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    watch.stopTime();

    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit((l, n) -> {
      levels.add(l);
      System.out.println(String.format("%d -> %s : %d", l, n.getPath(), n.getTime()));
    });
    assertEquals(levels.size(), 8);
  }

  @Test
  public void testDidNotStopAll() throws Exception {
    // Test that we can handle not having stop called on all the
    // timings started in a run
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTime("Test");
    functionOne(watch);
    functionTwo(watch);
    functionThree(watch);
    functionNoStop(watch);
    watch.stopTime();
    final ArrayList<Integer> levels = new ArrayList<>();
    watch.visit((l, n) -> {
      levels.add(l);
      System.out.println(String.format("%d -> %s : %d", l, n.getPath(), n.getTime()));
    });

    assertEquals(levels.size(), 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullNameException() {
    StackWatch watch = new StackWatch("testStackWatch");
    watch.startTime(null);
  }

  private void functionOne(StackWatch watch) throws Exception {
    watch.startTime("One");
    Thread.sleep(500);
    functionOneOne(watch);
    watch.stopTime();
  }

  private void functionOneOne(StackWatch watch) throws Exception {
    watch.startTime("OneOne");
    Thread.sleep(500);
    functionOneTwo(watch);
    watch.stopTime();

  }

  private void functionOneTwo(StackWatch watch) throws Exception {
    watch.startTime("OneTwo");
    Thread.sleep(500);
    watch.stopTime();
  }

  private void functionTwo(StackWatch watch) throws Exception {
    watch.startTime("Two");
    Thread.sleep(500);
    functionTwoOne(watch);
    watch.stopTime();
  }

  private void functionTwoOne(StackWatch watch) throws Exception {
    watch.startTime("TwoOne");
    Thread.sleep(500);
    functionTwoTwo(watch);
    watch.stopTime();
  }

  private void functionTwoTwo(StackWatch watch) throws Exception {
    watch.startTime("TwoTwo");
    Thread.sleep(500);
    watch.stopTime();
  }

  private void functionThree(StackWatch watch) throws Exception {
    watch.startTime("Three");
    Thread.sleep(500);
    watch.stopTime();
  }

  private void functionNoStop(StackWatch watch) throws Exception {
    watch.startTime("NoStop");
    Thread.sleep(500);
  }

}