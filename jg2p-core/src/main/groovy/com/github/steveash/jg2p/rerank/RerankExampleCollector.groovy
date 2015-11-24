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

import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.Percent
import com.google.common.collect.Lists
import com.google.common.util.concurrent.RateLimiter
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Class that knows how to collect examples for reranker training
 * @author Steve Ash
 */
class RerankExampleCollector {

  private static final Logger log = LoggerFactory.getLogger(RerankExampleCollector.class);

  private final ThreadLocalRandom rand = ThreadLocalRandom.current()
  private final TrainOptions opts;
  private final RerankableEncoder enc
  private final def limiter = RateLimiter.create(1.0 / 5.0)
  private final def total = new AtomicInteger(0)
  private final def skipped = new AtomicInteger(0)

  RerankExampleCollector(RerankableEncoder enc, TrainOptions opts) {
    this.enc = enc
    this.opts = opts;
  }

  List<RerankExample> makeExamples(List<InputRecord> inputs) {
    double recProb = ((double) opts.maxExamplesForReranker) / inputs.size()
    log.info("Collecting reranking examples Using rec prob of $recProb")
    List<RerankExample> exs = Lists.newArrayListWithExpectedSize(opts.maxExamplesForReranker)

    GParsPool.withPool {
      inputs.everyParallel { InputRecord input ->

        if (rand.nextDouble() > recProb) {
          // skip this record so we get close to the expected # of records
          return true;
        }

        def newTotal = total.incrementAndGet()
        def rrResult = enc.encode(input.xWord)
        if (rrResult == null) {
          throw new IllegalStateException("enc cant return null " + enc)
        }
        if (!rrResult.isValid) {
          log.info("Got invalid rr result for $input -> $rrResult")
          skipped.incrementAndGet()
          return true;
        }
        def best = rrResult.firstEntryFor(input.yWord.value)
        if (best == null) {
          skipped.incrementAndGet()
          return true; // this is a bad example so don't train on this
        }

        def pairProb = ((double) opts.maxPairsPerExampleForReranker) / (rrResult.overallResultCount() - 1)
        for (int i = 0; i < rrResult.overallResultCount(); i++) {
          if (i == best.indexInOverall) {
            continue;
          }
          RerankExample rr = makeExampleFor(i, rrResult, input, pairProb, best)
          if (rr == null) {
            continue;
          }

          synchronized (exs) {
            exs.add(rr)
          }
        }

        if (newTotal % 256 == 0) {
          if (limiter.tryAcquire()) {
            log.info "Completed " + newTotal + " of " + inputs.size() + " " + Percent.print(newTotal, inputs.size())
          }
        }
        return true;
      }
    }
    synchronized (exs) {
      if (isNotBlank(opts.writeOutputRerankExampleCsv)) {
        writeExamples(exs)
      }
      return exs;
    }
  }

  private void writeExamples(List<RerankExample> exs) {
    def file = makeOutputFile()
    file.withPrintWriter { pw ->
      log.info("Dumping collected examples out to disk...")
      def serial = CsvFactory.make().createSerializer()
      serial.open(pw)
      exs.each {serial.write(it)}
      serial.close(false)
      log.info("Done writing examples to $file")
    }
  }

  private RerankExample makeExampleFor(int indexInOverall, RerankableResult rrResult, InputRecord input,
                                       double pairProb,
                                       RerankableEntry best) {
    def cand = rrResult.entryAtOverallIndex(indexInOverall)

    if (cand.encoding.phones == null || cand.encoding.phones.isEmpty()) {
      log.info("Got bad cand for " + input.left.asSpaceString)
      skipped.incrementAndGet()
      return null;
    }
    if (cand.encoding.phones == best.encoding.phones) {
      return null // skip self refs
    }

    if (rand.nextDouble() > pairProb) {
      return null // skip this pair so we get close to the number of pairs that we want to eval
    }

    if (!Double.isFinite(cand.langModelScore)) {
      println "Got bad lm score from " + cand.encoding.phones.join("|") + " for " + input.left.asSpaceString
      return null
    }
    def rr = new RerankExample()
    rr.dupCountA = best.dupPhonesCount
    rr.dupCountB = cand.dupPhonesCount
    rr.encodingA = best.encoding
    rr.encodingB = cand.encoding
    rr.languageModelScoreA = best.langModelScore
    rr.languageModelScoreB = cand.langModelScore
    rr.uniqueMatchingModeA = best.hasMatchingUniqueModePhones
    rr.uniqueMatchingModeB = cand.hasMatchingUniqueModePhones
    rr.wordGraphs = input.left.value
    rr.label = RerankExample.A

    return rr
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
