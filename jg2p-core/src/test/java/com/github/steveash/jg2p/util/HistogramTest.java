/*
 * Copyright 2014 Steve Ash
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

package com.github.steveash.jg2p.util;

import com.google.common.collect.Iterators;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HistogramTest {
  private static final Logger log = LoggerFactory.getLogger(HistogramTest.class);

  @Test
  public void testSimpleHisto() throws Exception {
    Histogram histo = new Histogram(0.0, 10.0, 10);
    assertThat(histo.iteratorNonEmptyBins().hasNext(), Matchers.is(false));

    histo.add(5.0);
    assertThat(histo.getCountAt(5.0), is(1));
    assertThat(histo.getCountAtIndex(5), is(1));
    assertThat(histo.iteratorNonEmptyBins().hasNext(), Matchers.is(true));

    assertThat(Iterators.size(histo.iteratorNonEmptyBins()), is(1));
    assertThat(Iterators.size(histo.iterator()), is(10));
  }

  @Test
  public void testSimpleMerge() throws Exception {
    Histogram histo = new Histogram(0.0, 10.0, 10);
    histo.add(5.0);

    Histogram otherHisto = new Histogram(0.0, 10.0, 10);
    otherHisto.add(1.0);
    otherHisto.add(1.0);

    Histogram merged = Histogram.merge(histo, otherHisto);
    assertThat(merged.getCountAt(5.0), is(1));
    assertThat(merged.getCountAt(1.0), is(2));
  }

  @Test
  public void testEveryBucket() throws Exception {
    Histogram histo1 = new Histogram(0.0, 1.0, 20000);
    testEveryBucket(histo1, getHistoString(histo1));

    Histogram histo2 = new Histogram(0.65263, 2.98232, 13454);
    testEveryBucket(histo2, getHistoString(histo2));

    Histogram histo3 = new Histogram(0.0, 10.0, 100);
    testEveryBucket(histo3, getHistoString(histo3));
  }

  private String getHistoString(Histogram histo) {
    return String.format("Histo(%.4f, %.4f, %d)", histo.getMin(), histo.getMaxExcl(), histo.getBinCount());
  }

  public void testEveryBucket(Histogram histo, String histoName) {
    for (int i = 0; i < histo.getBinCount(); i++) {
      double min = histo.getMinBinRange(i);
      double max = histo.getMaxBinRange(i);
      double epsilon = histo.getBinWidth() / 10;
      double lower = min + epsilon;
      double upper = max - epsilon;
      double mid = (min + max) / 2.0;

      histo.add(lower);
      histo.add(upper);
      histo.add(mid);

      // Theoretically, if any of these is true, they should all be true, but we'll play it safe.
      assertThat(histoName + ": " + i, histo.getCountAt(lower), is(3));
      assertThat(histoName + ": " + i, histo.getCountAt(upper), is(3));
      assertThat(histoName + ": " + i, histo.getCountAt(mid), is(3));

      histo.clear();
    }
  }

  @Test
  public void testSmallWidth() throws Exception {
    Histogram histo = new Histogram(0.0, 1.0, 2000);
    histo.add(0.0);
    histo.add(0.0001);
    histo.add(0.0004);
    histo.add(0.00047);
    assertThat(histo.getCountAt(0.0), is(4));
    assertThat(histo.getCountAt(0.0003), is(4));
    assertThat(histo.getCountAt(0.00047), is(4));
    assertThat(histo.getCountAtIndex(0), is(4));

    histo.add(.617);
    histo.add(.6171);
    histo.add(.6172);
    histo.add(.6173);
    histo.add(.61745);
    assertThat(histo.getCountAt(.617), is(5));
    assertThat(histo.getCountAt(.6173), is(5));
    assertThat(histo.getCountAt(.61745), is(5));
    assertThat(histo.getCountAtIndex(1234), is(5));
  }
}