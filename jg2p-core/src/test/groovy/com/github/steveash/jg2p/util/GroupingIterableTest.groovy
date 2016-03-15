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

package com.github.steveash.jg2p.util

import com.google.common.base.Equivalence
import org.junit.Test

/**
 * @author Steve Ash
 */
class GroupingIterableTest {

  @Test
  public void shouldGroup() throws Exception {
    def equiv = { a, b -> a[0].equals(b[0]) } as Equivalence
    def src = [[0, 'a'], [0, 'b'], [1, 'c'], [2, 'd'], [2, 'e']]
    def gi = GroupingIterable.groupOver(src, equiv)
    def iter = gi.iterator()
    assert ['a', 'b'] == iter.next().collect { it[1] }
    assert ['c'] == iter.next().collect { it[1] }
    assert ['d', 'e'] == iter.next().collect { it[1] }
  }

  @Test
  public void shouldGroupEmpty() throws Exception {
    def equiv = { a, b -> a[0].equals(b[0]) } as Equivalence
    def gi = GroupingIterable.groupOver([], equiv)
    assert !gi.iterator().hasNext()
  }
}
