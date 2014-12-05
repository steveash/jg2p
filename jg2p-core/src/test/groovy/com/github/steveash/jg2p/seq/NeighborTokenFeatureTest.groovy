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

package com.github.steveash.jg2p.seq

import cc.mallet.types.Instance
import cc.mallet.types.Token
import cc.mallet.types.TokenSequence
import org.junit.Test

/**
 * @author Steve Ash
 */
class NeighborTokenFeatureTest {

  @Test
  public void shouldProcessTokens() throws Exception {
    def ntf = new NeighborTokenFeature(true, -2, -1, +1, +2)
    def (Instance inst, TokenSequence ts) = make("S", "T", "E", "V", "E")
    ntf.pipe(inst)
    println "0" + ts.get(0).toStringWithFeatureNames()
    println "1" + ts.get(1).toStringWithFeatureNames()
    println "2" + ts.get(2).toStringWithFeatureNames()
    println "3" + ts.get(3).toStringWithFeatureNames()
    println "4" + ts.get(4).toStringWithFeatureNames()
    assert ts.get(0).features.size() == 2
    assert ts.get(1).features.size() == 3
    assert ts.get(2).features.size() == 4
    assert ts.get(3).features.size() == 3
    assert ts.get(4).features.size() == 2
  }

  @Test
  public void shouldProcessTokens2() throws Exception {
    def ntf = new NeighborTokenFeature(true, [
        new NeighborTokenFeature.NeighborWindow(-2, 2),
        new NeighborTokenFeature.NeighborWindow(-1, 1),
        new NeighborTokenFeature.NeighborWindow(1, 3),
    ])
    def (Instance inst, TokenSequence ts) = make("ST", "E", "PH", "E", "NSON")
    ntf.pipe(inst)
    println "0" + ts.get(0).toStringWithFeatureNames()
    println "1" + ts.get(1).toStringWithFeatureNames()
    println "2" + ts.get(2).toStringWithFeatureNames()
    println "3" + ts.get(3).toStringWithFeatureNames()
    println "4" + ts.get(4).toStringWithFeatureNames()
    assert ts.get(0).features.size() == 1
    assert ts.get(1).features.size() == 3
    assert ts.get(2).features.size() == 3
    assert ts.get(3).features.size() == 3
    assert ts.get(4).features.size() == 2
  }

  def make(String... s) {
    def ts = new TokenSequence(s.collect { new Token(it) })
    return [new Instance(ts, null, null, null), ts]
  }
}
