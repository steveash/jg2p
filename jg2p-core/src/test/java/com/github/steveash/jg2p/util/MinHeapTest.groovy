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
class MinHeapTest {

  @Test
  public void shouldMaintainHeap() throws Exception {
    def h = new MinHeap<Integer>(5)
    assert h.isEmpty()
    h.add(10)
    assert h.peek() == 10
    h.add(5)
    assert h.peek() == 5
    h.add(11)
    assert h.peek() == 5
    h.add(12)
    assert h.peek() == 5
    h.add(3)
    assert h.peek() == 3
    assert h.isFull()

    assert h.remove() == 3
    assert !h.isFull()
    assert h.peek() == 5
    assert h.remove() == 5
    assert h.remove() == 10
    assert h.remove() == 11
    assert h.remove() == 12
    assert h.isEmpty()

    h.add(5)
    assert h.peek() == 5
    h.add(3)
    assert h.peek() == 3
    assert h.remove() == 3
    h.add(9)
    assert h.peek() == 5
    h.add(3)
    assert h.peek() == 3
    h.remove()
    h.add(15)
    assert h.remove() == 5
    assert h.remove() == 9
    assert h.remove() == 15
    assert h.isEmpty()
  }
}
