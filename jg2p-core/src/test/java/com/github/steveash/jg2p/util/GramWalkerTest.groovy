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

import org.junit.Test

/**
 * @author Steve Ash
 */
class GramWalkerTest {
  public static final grams = ["A B", "C", "D E", "F E G"]

  @Test
  public void shouldReturnWindowsOrNull() throws Exception {
    assert GramWalker.window(grams, 1, 0, 1, 3) == "D E F"
    assert GramWalker.window(grams, 2, 1, 0, 2) == "E F"
    assert GramWalker.window(grams, 3, 2, 0, 1) == "G"
    assert GramWalker.window(grams, 3, 2, 0, 2) == null
    assert GramWalker.window(grams, 1, 0, 0, 1) == "C"
    assert GramWalker.window(grams, 1, 0, 2, 1) == "E"
    assert GramWalker.window(grams, 0, 0, 0, 8) == "A B C D E F E G"

  }

  @Test
  public void shouldGetBackwards() throws Exception {
    assert GramWalker.window(grams, 1, 0, -2, 1) == "A"
    assert GramWalker.window(grams, 1, 0, -2, 2) == "A B"
    assert GramWalker.window(grams, 2, 1, -4, 3) == "A B C"
    assert GramWalker.window(grams, 3, 2, -7, 7) == "A B C D E F E"
    assert GramWalker.window(grams, 3, 2, -8, 8) == null
    assert GramWalker.window(grams, 0, 0, -1, 1) == null

  }
}
