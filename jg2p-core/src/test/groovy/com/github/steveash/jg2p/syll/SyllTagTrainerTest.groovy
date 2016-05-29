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

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.Alignment
import org.apache.commons.lang3.tuple.Pair

/**
 * @author Steve Ash
 */
class SyllTagTrainerTest extends GroovyTestCase {

  void testSyllables() {
    def alg = new Alignment(Word.fromNormalString("photography"), [
        Pair.of("p h", "F"), Pair.of("o", "OH"), Pair.of("t", "T"), Pair.of("o", "AH"), Pair.of("g r", "G R"),
        Pair.of("a", "AE"), Pair.of("p h", "F"), Pair.of("y", "EE EE")
    ], 1.0, null, new SWord("F OH T AH G R AE F EE EE", "0 2 4 7"))

    def sylls = SyllTagTrainer.makeSyllablesFor(alg)
    sylls.each {println it}
    assert sylls.size() == 4
  }

  void testSyllables2() {
    def alg = new Alignment(Word.fromNormalString("ah"), [Pair.of("a h", "AH")], 1.0, null,
                            new SWord("AH", "0"))

    def sylls = SyllTagTrainer.makeSyllablesFor(alg)
    sylls.each {println it}
    assert sylls.size() == 1
  }

  void testMarksToGrams() {
    def alg = new Alignment(Word.fromNormalString("photography"), [
        Pair.of("p h", "F"), Pair.of("o", "OH"), Pair.of("t", "T"), Pair.of("o", "AH"), Pair.of("g r", "G R"),
        Pair.of("a", "AE"), Pair.of("p h", "F"), Pair.of("y", "EE EE")
    ], 1.0, null, new SWord("F OH T AH G R AE F EE EE", "0 2 4 7"))
    def marks = SyllTagTrainer.makeSyllMarksFor(alg)
    def grams = SyllTagTrainer.makeSyllGramsFromMarks(marks)
    println alg
    println marks
    println grams
    assert alg.graphones.size() == grams.size()
  }

  void testSyllMarks() {
    def alg = new Alignment(Word.fromNormalString("photography"), [
        Pair.of("p h", "F"), Pair.of("o", "OH"), Pair.of("t", "T"), Pair.of("o", "AH"), Pair.of("g r", "G R"),
        Pair.of("a", "AE"), Pair.of("p h", "F"), Pair.of("y", "EE EE")
    ], 1.0, null, new SWord("F OH T AH G R AE F EE EE", "0 2 4 7"))
    def marks = SyllTagTrainer.makeSyllableGraphEndMarksFor(alg)
    println alg
    println marks
    assert marks.count {it == "Z"} == 4
  }

  void testSyllMarksConstrained() {
    def word = Word.fromNormalString("photography")

    def marks = SyllTagTrainer.makeSyllableGraphEndMarksFromGraphStarts(word, [0, 3, 5, 8].toSet())
    println marks
    assert marks.count {it == "Z"} == 4
  }
}
