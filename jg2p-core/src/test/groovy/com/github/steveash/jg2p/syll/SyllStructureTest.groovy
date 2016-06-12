/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jg2p.syll

import com.google.common.collect.ImmutableList

/**
 * @author Steve Ash
 */
class SyllStructureTest extends GroovyTestCase {

  void testStructure() {

    def struct = new SyllStructure(["n", "a", "m a", "s", "t", "a y"],
                                   ["O", "N", "O N", "O", "O", "N C"],
                                   [0, 2, 4].toSet()
    );
    println struct
    assert struct.syllCount == 3
    assert struct.getSyllPart(0) == "nA"
    assert struct.getSyllPart(1) == "mA"
    assert struct.getSyllPart(2) == "stAy"

    assert struct.getSyllPart(0, 1, 1, 0) == "nA"
    assert struct.getSyllPart(0, 1, 0, 0) == "n"
    assert struct.getSyllPart(2, 1, 1, 0) == "tA"
    assert struct.getSyllPart(2, 1, 1, 1) == "tAy"
    assert struct.getSyllPart(2, 2, 0, 1) == "sty"

    assert !struct.graphoneGramIndexContainsNucleus(0)
    assert struct.graphoneGramIndexContainsNucleus(1)
    assert struct.graphoneGramIndexContainsNucleus(2)
    assert !struct.graphoneGramIndexContainsNucleus(3)
    assert !struct.graphoneGramIndexContainsNucleus(4)
    assert struct.graphoneGramIndexContainsNucleus(5)

    assert struct.getSyllIndexForGraphemeIndex(0) == 0
    assert struct.getSyllIndexForGraphemeIndex(1) == 0
    assert struct.getSyllIndexForGraphemeIndex(2) == 1
    assert struct.getSyllIndexForGraphemeIndex(3) == 1
    assert struct.getSyllIndexForGraphemeIndex(4) == 2
    assert struct.getSyllIndexForGraphemeIndex(5) == 2
    assert struct.getSyllIndexForGraphemeIndex(6) == 2
    assert struct.getSyllIndexForGraphemeIndex(7) == 2
  }
}
