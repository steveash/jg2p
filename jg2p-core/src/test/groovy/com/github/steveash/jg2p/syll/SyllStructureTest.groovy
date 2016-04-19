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

    def struct = new SyllStructure(ImmutableList.of("n", "a", "m a", "s", "t", "a y"),
                                   ImmutableList.of("O", "N", "O N", "O", "O", "N C")
    );
    println struct
    assert struct.syllCount == 3
    assert struct.getSyllPart(0) == "na"
    assert struct.getSyllPart(1) == "ma"
    assert struct.getSyllPart(2) == "stay"

    assert struct.getSyllPart(0, 1, 1, 0) == "na"
    assert struct.getSyllPart(0, 1, 0, 0) == "n"
    assert struct.getSyllPart(2, 1, 1, 0) == "ta"
    assert struct.getSyllPart(2, 1, 1, 1) == "tay"
    assert struct.getSyllPart(2, 2, 0, 1) == "sty"

    assert !struct.graphoneGramIndexContainsNucleus(0)
    assert struct.graphoneGramIndexContainsNucleus(1)
    assert struct.graphoneGramIndexContainsNucleus(2)
    assert !struct.graphoneGramIndexContainsNucleus(3)
    assert !struct.graphoneGramIndexContainsNucleus(4)
    assert struct.graphoneGramIndexContainsNucleus(5)
  }
}
