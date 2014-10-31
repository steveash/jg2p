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

package com.github.steveash.jg2p

import org.junit.Test

/**
 * @author Steve Ash
 */
class GramsTest {

  @Test
  public void shouldNestedLoop() throws Exception {
    def a = Word.fromNormalString("abc")
    def b = Word.fromNormalString("de")

    def expected = ["a^d", "a^e", "b^d", "b^e", "c^d", "c^e",
                    "a b^d", "b c^d", "a b^e", "b c^e", "a^d e", "b^d e", "c^d e",
                    "a b^d e", "b c^d e"].toSet()
    assert expected == Grams.gramProduct(a, b, new GramOptions(1, 2)).collect { it.left + "^" + it.right }.toSet()
  }

  @Test
  public void shouldNestedLoopWithGramEps() throws Exception {
    def a = Word.fromNormalString("ab")
    def b = Word.fromNormalString("c")
    def opts = new GramOptions(1, 2, 1, 2, true, true)
    def expected = ["a^c", "b^c", "a b^c", "a^", "b^", "^c", "a b^"].toSet()
    assert expected == Grams.gramProduct(a, b, opts).collect { it.left + "^" + it.right }.toSet()
  }
}
