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
class WordTest {

  @Test
  public void shouldIterateOverGrams() throws Exception {
    def w = Word.fromNormalString("steve")
    assert ["s", "t", "e", "v", "e"] == w.gramsSize(1).toList()
    assert ["s t", "t e", "e v", "v e"] == w.gramsSize(2).toList()
    assert ["s t e", "t e v", "e v e"] == w.gramsSize(3).toList()
    assert ["s", "t", "e", "v", "e", "s t", "t e", "e v", "v e"] == w.gramsSizes(1,2).toList()
  }

  @Test
  public void shouldGram() throws Exception {
    def w = Word.fromNormalString("steve")
    assert "s t" == w.gram(0, 2)
    assert "v e" == w.gram(3, 2)
  }
}
