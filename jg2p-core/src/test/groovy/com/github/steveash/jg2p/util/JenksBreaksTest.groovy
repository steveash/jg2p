/*
 * Copyright 2015 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p.util

import com.google.common.collect.Lists
import groovy.xml.MarkupBuilder
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * @author Steve Ash
 */
class JenksBreaksTest {

  private final Random random = new Random(0x123456789ABCDEFL);


  @Test
  public void shouldFindEasyBreak() throws Exception {
    List<Double> data = Lists.newArrayList();
    for (int i = 0; i < 50; i++) {
      data.add(random.nextDouble() * 200);
      data.add(random.nextDouble() * 200 + 300);
    }
    List<Double> breaks = JenksBreaks.computeBreaks(data, 2);
    assertEquals(0, breaks.get(0), 20);
    assertEquals(300, breaks.get(1), 20);
    assertEquals(500, breaks.get(2), 20);
  }

  @Test
  public void shouldFindReallyEasyBreak() throws Exception {
    List<Double> data = Lists.newArrayList(1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 8.0d, 9.0d, 10.0d);
    List<Double> breaks = JenksBreaks.computeBreaks(data, 2);
    println breaks
    assertEquals(3, breaks.size());
    assertEquals(1.0d, breaks.get(0), 0);
    assertEquals(8.0d, breaks.get(1), 0);
    assertEquals(10.0d, breaks.get(2), 0);
  }

  @Test
  public void shouldCalculatePerfectGof() throws Exception {
    List<Double> data = Lists.newArrayList(10.0d, 10.0d, 10.0d, 20.0d, 20.0d, 20.0d, 20.0d);
    List<Double> breaks = JenksBreaks.computeBreaks(data, 2);
    println breaks
    assertEquals(10, breaks.get(0), 0.01)
    assertEquals(20, breaks.get(1), 0.01)
    assertEquals(20, breaks.get(2), 0.01)
    assertEquals(1.0, JenksBreaks.goodnessOfFit(data, breaks), 0.01)
  }

  @Test
  public void shouldCalculateAlmostPerfectGof() throws Exception {
    List<Double> data = Lists.newArrayList(10.0d, 10.0d, 10.0d, 20.0d, 20.0d, 20.0d, 25.0d);
    List<Double> breaks = JenksBreaks.computeBreaks(data, 2);
    println breaks
    assertEquals(10, breaks.get(0), 0.01)
    assertEquals(20, breaks.get(1), 0.01)
    assertEquals(25, breaks.get(2), 0.01)
    assertEquals(0.92, JenksBreaks.goodnessOfFit(data, breaks), 0.01)
  }
}
