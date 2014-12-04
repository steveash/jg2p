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

package com.github.steveash.jg2p.align

import com.github.steveash.jg2p.Word
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Steve Ash
 */
class AlignerTrainerTest {

  private static final Logger log = LoggerFactory.getLogger(AlignerTrainerTest.class);

  @Test
  public void shouldSmokeTestEm() throws Exception {
    def records = InputReader.makeDefaultFormatReader().readFromClasspath("sample.txt")
    log.info("Read ${records.size()} records to trainAndSave...")

    def opts = new TrainOptions()
    def a = new AlignerTrainer(opts)
    def model = a.train(records)
    def p = model.getTransitions()

    log.info("Prob table has " + p.entryCount() + " entries")
    int i = 0;
    for (def pp : p) {
      if (pp.value == 0) continue;

      log.info(" $i : ${pp.rowKey} -> ${pp.columnKey} = ${pp.value}")
      i += 1;
      if (i > 100) break;
    }

    printExample(model, "fresh", "F R EH SH")
    printExample(model, "wrinkling", "R IH NG K L IH NG")
  }

  private printExample(AlignModel v, String left, String right) {
    def x = Word.fromNormalString(left)
    def results = v.align(x, Word.fromSpaceSeparated(right), 3)
    def inferBest = v.inferAlignments(x, 3)
    println "$left to $right got ${results.size()}"
    results.each { println it }
    println "Inferred best"
    inferBest.each { println it }
  }
}
