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

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.GroupingIterable
import com.github.steveash.jg2p.util.Percent
import com.google.common.util.concurrent.RateLimiter
import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.operator.PoisonPill
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Class that knows how to collect examples for reranker training
 * @author Steve Ash
 */
//@CompileStatic
class RerankExampleCollector {

  private static final Logger log = LoggerFactory.getLogger(RerankExampleCollector.class);

  private final TrainOptions opts;
  private final RerankableEncoder enc
  private final RateLimiter limiter = RateLimiter.create(1.0 / 5.0)
  private final AtomicInteger total = new AtomicInteger(0)
  private final AtomicInteger skipped = new AtomicInteger(0)
  private final AtomicInteger sequence = new AtomicInteger(0)

  RerankExampleCollector(RerankableEncoder enc, TrainOptions opts) {
    this.enc = enc
    this.opts = opts;
  }

  Collection<List<RerankExample>> makeExamples(List<InputRecord> inputs) {
    assert InputRecord.OrderByX.isOrdered(inputs): "the inputs aren't ordered! what happened?"

    Iterable<List<InputRecord>> gi = GroupingIterable.groupOver(inputs, InputRecord.EqualByX)
    log.info("Collecting reranking examples from " + inputs.size() + " grouped inputs")
    def outputFile = makeOutputFile()
    GParsPool.withPool {
      def dfout = new DataflowQueue()
      def writer = Dataflow.task {
        outputFile.withPrintWriter { pw ->
          def serial = CsvFactory.make().createSerializer()
          serial.open(pw)
          while (true) {
            def msg = dfout.val
            if (msg instanceof PoisonPill) {
              break;
            }
            List<RerankExample> exs = msg as List<RerankExample>
            exs.each { serial.write(it) }
          }
          serial.close(false)
          log.info("Done writing examples to $file")
        }
      }
      gi.eachParallel { List inRecs ->
        List<InputRecord> recs = (List<InputRecord>) inRecs
        Word xWord = (Word) (((InputRecord) recs[0]).left)

        def newTotal = total.incrementAndGet()
        def rrResult = enc.encode(xWord)
        if (rrResult == null) {
          throw new IllegalStateException("enc cant return null " + enc)
        }
        if (!rrResult.isValid) {
          log.warn("Got invalid rr result for $xWord -> $rrResult")
          skipped.incrementAndGet()
          return;
        }
        def goodPhones = recs.collect { InputRecord rec -> rec.yWord.value }.toSet()
        def outs = RerankExample.makeExamples(rrResult, xWord, goodPhones, sequence.incrementAndGet())
        if (outs.every { !it.relevant }) {
          skipped.incrementAndGet()
          return;
        }

        dfout << outs

        if (limiter.tryAcquire()) {
          log.info "Completed " + total.get() + " of " + inputs.size() + " " + Percent.print(newTotal, inputs.size())
        }
      }
      log.info("Waiting for writer to catch up...")
      dfout << PoisonPill.instance
      writer.get()
    }

    log.info("Finished all " + total.get() + " entries, skipped " + skipped.get() + " of them")
    return new RerankExampleCsvReader().readFrom(outputFile.absolutePath)
  }

  private File makeOutputFile() {
    if (isNotBlank(opts.writeOutputRerankExampleCsv)) {
      return new File(opts.writeOutputRerankExampleCsv)
    }
    def outFile = File.createTempFile("reranker-ex", "csv")
    outFile.deleteOnExit()
    log.info("Temporarily writing examples to " + outFile)
    return outFile
  }
}
