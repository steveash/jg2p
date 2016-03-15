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

package com.github.steveash.jg2p.rerank

import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.GroupingIterable
import com.google.common.base.Equivalence
import com.google.common.collect.Lists
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Reads reranker examples for training from a csv file
 * @author Steve Ash
 */
class RerankExampleCsvReader {

  private static final Logger log = LoggerFactory.getLogger(RerankExampleCsvReader.class);

  List<List<RerankExample>> readFrom(String exampleCsvFile) {
    def exs = Lists.newArrayList()
    new File(exampleCsvFile).withReader { r ->
      def deser = CsvFactory.make().createDeserializer()
      def count = 0;
      deser.open(r)
      while (deser.hasNext()) {
        RerankExample ex = deser.next()
        if (ex.encoding.phones == null || ex.encoding.phones.isEmpty() ) {
          log.warn("Problem with example on line $count got $ex skipping...")
        } else {
          exs.add(ex)
        }

        count += 1
        if (count % 5000 == 0) {
          log.info("Parsed $count input reranker example csv records...")
        }
      }
      log.info("Got ${exs.size()} inputs to train on from many lines of input")
    }
    def gi = GroupingIterable.groupOver(exs, {RerankExample a, RerankExample b -> a.sequence == b.sequence} as Equivalence)
    def outputList = Lists.newArrayList(gi)
    log.info("Got " + outputList.size() + " grouped example lists from reader")
    return outputList
  }
}
