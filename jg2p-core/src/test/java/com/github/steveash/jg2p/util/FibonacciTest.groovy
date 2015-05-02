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
class FibonacciTest {

  @Test
  public void shouldCalculateBucket() throws Exception {
    assert 0 == Fibonacci.prevFibNumber(0)
    assert 1 == Fibonacci.prevFibNumber(1)
    assert 2 == Fibonacci.prevFibNumber(2)
    assert 3 == Fibonacci.prevFibNumber(3)
    assert 3 == Fibonacci.prevFibNumber(4)
    assert 5 == Fibonacci.prevFibNumber(5)
    assert 5 == Fibonacci.prevFibNumber(6)
    assert 5 == Fibonacci.prevFibNumber(7)
    assert 8 == Fibonacci.prevFibNumber(8)
    assert 8 == Fibonacci.prevFibNumber(9)
    assert 8 == Fibonacci.prevFibNumber(10)
    assert 8 == Fibonacci.prevFibNumber(11)
    assert 8 == Fibonacci.prevFibNumber(12)
    assert 13 == Fibonacci.prevFibNumber(13)

  }
}
