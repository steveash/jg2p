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

package com.github.steveash.jg2p.align

import org.junit.Test

/**
 * @author Steve Ash
 */
class ProbTableTest {

  @Test
  public void shouldUnionXy() throws Exception {
    def a = new ProbTable();
    a.setProb("1", "3", 12)
    a.setProb("2", "3", 13)
    a.setProb("4", "5", 14)

    def b = new ProbTable();
    b.setProb("1", "3", 12) // common
    b.setProb("2", "3", 13) // common
    b.setProb("6", "7", 14)
    b.setProb("7", "8", 15)

    def c = ProbTable.unionOfAllCells(a, b)
    assert c.size() == 5
    assert c.collect { it.left + it.right }.toList().sort() == ["13", "23", "45", "67", "78"]
  }
}
