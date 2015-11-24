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

import org.apache.commons.lang3.tuple.Pair
import org.apache.commons.math3.stat.correlation.KendallsCorrelation
import org.junit.Test

import static com.google.common.math.DoubleMath.fuzzyEquals

/**
 * @author Steve Ash
 */
class KendalTauTest {

  @Test
  public void shouldTestNoTies() throws Exception {
    assert 1.0d == KendallTau.calculate([
        Pair.of("A", "A"),
        Pair.of("B", "B"),
        Pair.of("C", "C"),
        Pair.of("D", "D"),
    ])

    assert 1.0d == KendallTau.calculate([
        Pair.of("A", "A")
    ])

    assert 0.0d == KendallTau.calculate([])

    assert fuzzyEquals(-1.0, KendallTau.calculate([
        Pair.of("A", "D"),
        Pair.of("B", "C"),
        Pair.of("C", "B"),
        Pair.of("D", "A"),
    ]), 0.01);

    assert fuzzyEquals(-1.0, KendallTau.calculate([
        Pair.of("D", "A"),
        Pair.of("C", "B"),
        Pair.of("B", "C"),
        Pair.of("A", "D"),
    ]), 0.01);
  }

  @Test
  public void shouldMeasureClose() throws Exception {
    assert fuzzyEquals(0.66, KendallTau.calculate([
        Pair.of("A", "A"),
        Pair.of("B", "C"),
        Pair.of("C", "B"),
        Pair.of("D", "D"),
    ]), 0.01);
  }

  @Test
  public void shouldUseCommonsMath() throws Exception {
    assert fuzzyEquals(0.8944, new KendallsCorrelation().correlation(
        [ 1.0, 1.0, 1.0, 2.0, 3.0, 4.0 ] as double[],
        [1.0, 2.0, 3.0, 4.0, 5.0, 6.0 ] as double[]
        ), 0.01);
  }

  @Test
  public void shouldMeasureWithTies() throws Exception {
    assert fuzzyEquals(0.894, KendallTau.calculate([
        Pair.of("A", "A"),
        Pair.of("A", "B"),
        Pair.of("A", "C"),
        Pair.of("B", "D"),
        Pair.of("C", "E"),
        Pair.of("D", "F"),
    ]), 0.01);

    assert fuzzyEquals(0.894, KendallTau.calculate([
        Pair.of("A", "B"),
        Pair.of("A", "C"),
        Pair.of("A", "D"),
        Pair.of("B", "E"),
        Pair.of("C", "F"),
        Pair.of("D", "G"),
    ]), 0.01);

    assert fuzzyEquals(1.0, KendallTau.calculate([
        Pair.of("B", "A"),
        Pair.of("C", "A"),
        Pair.of("D", "A"),
        Pair.of("E", "B"),
        Pair.of("F", "C"),
        Pair.of("G", "D"),
    ]), 0.01);

    assert fuzzyEquals(1.0, KendallTau.calculate([
        Pair.of("B", "A"),
        Pair.of("C", "A"),
        Pair.of("D", "A"),
        Pair.of("E", "A"),
        Pair.of("F", "A"),
        Pair.of("G", "A"),
    ]), 0.01);

    assert fuzzyEquals(-1.0, KendallTau.calculate([
        Pair.of("G", "A"),
        Pair.of("F", "A"),
        Pair.of("E", "A"),
        Pair.of("D", "B"),
        Pair.of("B", "C"),
        Pair.of("A", "D"),
    ]), 0.01);
  }
}
