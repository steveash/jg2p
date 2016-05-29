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

package com.github.steveash.jg2p.syllchain

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.syll.SWord
import org.apache.commons.lang3.tuple.Pair

/**
 * @author Steve Ash
 */
class SyllChainTrainerTest extends GroovyTestCase {

  void testSplitGraphsBySylls() {
    def alg = new Alignment(Word.fromNormalString("exponentially"), [
            Pair.of("e", "EH"), Pair.of("x", "K S"), Pair.of("p", "P"), Pair.of("o", "OW"), Pair.of("n", "N"),
            Pair.of("e", "EH"), Pair.of("n", "N"), Pair.of("t i", "SH"), Pair.of("a", "AH"), Pair.of("l l", "L"),
            Pair.of("y", "IY")
        ], 1.0, null, new SWord("EH K S P OW N EH N SH AH L IY", "0 2 5 8 10"))
    def splits = SyllChainTrainer.splitGraphsByPhoneSylls(alg)
    println alg
    println splits
    println alg.inputWord.splitBy(splits)
    assert splits.size() == alg.syllWord.syllCount()
  }
}
