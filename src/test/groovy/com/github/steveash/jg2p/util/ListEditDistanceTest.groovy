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

package com.github.steveash.jg2p.util

import org.junit.Test

/**
 * @author Steve Ash
 */
class ListEditDistanceTest {

  @Test
  public void shouldCalculateEdits() throws Exception {
    assertTwo("steve", "steve", 0)
    assertTwo("steve", "stteve", 1)
    assertTwo("steve", "sttevee", 2)
    assertTwo("stevvvve", "steve", 3)
    assertTwo("abcd", "efgh", 4)
  }

  void assertTwo(String a, String b, int expected) {
    def aa = []
    a.each { aa << it }
    def bb = []
    b.each { bb << it }

    assert ListEditDistance.editDistance(aa, bb, expected + 1) == expected
  }
}
