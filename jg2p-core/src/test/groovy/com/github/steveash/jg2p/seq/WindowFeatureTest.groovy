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

package com.github.steveash.jg2p.seq

import cc.mallet.types.Instance
import cc.mallet.types.TokenSequence

/**
 * @author Steve Ash
 */
class WindowFeatureTest extends GroovyTestCase {

  void testFeatureText() {
    Instance ins = make(["p r", "o", "g r", "a", "m m", "e", "r"])
    def ff = new WindowFeature(false, 6)
    def result = ff.pipe(ins)
    println result.getData();
  }

  private make(List<String> grams) {
    def ts = new TokenSequence()
    grams.each { ts.add(it) }
    def ins = new Instance(ts, null, null, null)
    return ins
  }

  void testFeatureShape() {
    Instance ins = make(["p r", "o", "g r", "a", "m m", "e", "r"])
    def ff = new WindowFeature(true, 6)
    def result = ff.pipe(ins)
    println result.getData();
  }
}
