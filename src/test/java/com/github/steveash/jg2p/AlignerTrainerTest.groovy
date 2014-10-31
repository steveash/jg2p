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

package com.github.steveash.jg2p

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
    def records = new InputReader().readFromClasspath("sample.txt")
    log.info("Read ${records.size()} records to train...")
    def a = new AlignerTrainer(new GramOptions())
    def p = a.train(records)

    log.info("Prob table has " + p.entryCount() + " entries")
    int i = 0;
    for (def pp : p) {
      if (pp.value == 0) continue;

      log.info(" $i : ${pp.rowKey} -> ${pp.columnKey} = ${pp.value}")
      i += 1;
      if (i > 100) return;
    }
  }
}
