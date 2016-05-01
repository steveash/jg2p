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

package com.github.steveash.jg2p.syll

/**
 * @author Steve Ash
 */
class SWordTest extends GroovyTestCase {

  void testSyllStress() {
    def sw = new SWord("P R E D I C T I O N S", "0 3 6", "0 1 2")
    for (int i = 0; i < sw.unigramCount(); i++) {
      println sw.gramAt(i) + " syllable? " + sw.isStartOfSyllable(i) + " stress? " + sw.getStressForPhoneme(i)
    }
  }

  void testPhoneCoding() {
    def sw = new SWord("S AY K AH L IH S T", "0 2 4")
    println "Coding = " + sw.oncCodingForPhones
  }
}
