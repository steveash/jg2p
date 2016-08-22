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

package com.github.steveash.jg2p.wfst

/**
 * @author Steve Ash
 */
class ListAcceptorTest extends GroovyTestCase {

  void testSimpleAccept() {
    def la = new ListAcceptor([
        (["A", "B"]):"a,b",
        (["A", "C"]):"a,c",
        (["B", "C"]):"b,c",
        (["A", "B", "C"]):"a,b,c",
        (["A", "B", "C", "D"]):"a,b,c,d"
    ])
    assert ["a,b"] == la.accept(["A", "B", "F"]).collect {it.value}
    assert [] == la.accept(["F"]).collect {it.value}
    assert ["a,b", "a,b,c"] == la.accept(["A", "B", "C"]).collect {it.value}
    assert ["a,b", "a,b,c", "a,b,c,d"] == la.accept(["A", "B", "C", "D"]).collect {it.value}
    assert ["b,c"] == la.accept(["B", "C", "E", "X"]).collect {it.value}
    assert [] == la.accept(["B", "X"]).collect {it.value}
  }
}
