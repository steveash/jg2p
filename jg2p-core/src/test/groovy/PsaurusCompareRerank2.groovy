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

import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.PhoneticEncoder.Encoding
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.phoseq.Graphemes
import com.github.steveash.jg2p.phoseq.Phonemes
import com.github.steveash.jg2p.phoseq.WordShape
import com.github.steveash.jg2p.rerank.Rerank2Model
import com.github.steveash.jg2p.rerank.RerankExample
import com.github.steveash.jg2p.rerank.VowelReplacer
import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import groovy.transform.Field
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool
import kylm.model.ngram.NgramLM
import org.apache.commons.lang3.StringUtils

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to create training data for the reranking model
 * this will use the extra generated candidates (with vowelReplacer) but will use a fairly large feature set
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))

def file = "g014b2b-results.train"
//def file = "g014b2b.test"
//def inps = InputReader.makePSaurusReader().readFromClasspath(file)
def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)

@Field PhoneticEncoder enc = ReadWrite.
    readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f4C_250.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(25)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

@Field NgramLM lm = ReadWrite.readFromFile(NgramLM.class, new File("../resources/lm_7_kn.dat"))

Stopwatch watch = Stopwatch.createStarted()
def total = new AtomicInteger(0)
def skipped = new AtomicInteger(0)
@Field Random rand = new Random(0xCAFEBABE)
def totalPairsPerEntryToInclude = 12
def totalEntriesToInclude = 50000
println "Starting..."

// calculate the probability of including any particular record
double recProb = totalEntriesToInclude.toDouble() / inps.size()
def vr = new VowelReplacer()
println "Using rec prob of $recProb"
new File("../resources/psaur_rerank_train.txt").withPrintWriter { pw ->
  def serial = CsvFactory.make().createSerializer()
  serial.open(pw)

  GParsPool.withPool {
    inps.everyParallel { InputRecord input ->

      if (rand.nextDouble() > recProb) {
        // skip this record so we get close to the expected # of records
        return true;
      }

      def newTotal = total.incrementAndGet()
      def cans = enc.complexEncode(input.xWord)
      def ans = vr.updateResults(cans.overallResults)

      assert ans.size() > 0
      def dups = HashMultiset.create()
      ans.each { dups.add(it.phones) }
      def modeEntry = dups.entrySet().max { it.count }
      assert modeEntry != null
      int candidatesSameAsMode = dups.entrySet().count { it.count == modeEntry.count }
      List<String> modePhones = modeEntry.element
      boolean uniqueMode = (candidatesSameAsMode == 1)

      def bestAns = ans.find { input.yWord.value == it.phones }

      if (bestAns == null) {
        skipped.incrementAndGet()
        return true; // this is a bad example so don't train on this
      }

      def bestDupCount = dups.count(bestAns.phones)
      def bestUniqueMode = uniqueMode && bestAns.phones == modePhones
      def bestLm = lm.getSentenceProbNormalized(bestAns.phones.toArray(new String[0]))
      def pairProb = totalPairsPerEntryToInclude.toDouble() / (ans.size() - 1)

      ans.each { cand ->
        if (cand.phones == bestAns.phones) {
          return // skip self refs
        }

        if (rand.nextDouble() > pairProb) {
          return // skip this pair so we get close to the number of pairs that we want to eval
        }

        def rr = new RerankExample()
        rr.dupCountA = bestDupCount
        rr.dupCountB = dups.count(cand.phones)
        rr.encodingA = bestAns
        rr.encodingB = cand
        rr.languageModelScoreA = bestLm
        rr.languageModelScoreB = lm.getSentenceProbNormalized(cand.phones.toArray(new String[0]))
        rr.uniqueMatchingModeA = bestUniqueMode
        rr.uniqueMatchingModeB = uniqueMode && cand.phones == modePhones
        rr.wordGraphs = input.left.value
        rr.label = RerankExample.A

        synchronized (serial) {
          serial.write(rr);
        }
      }

      if (newTotal % 5000 == 0) {
        println "Completed " + newTotal + " of " + inps.size()
      }
      return true;
    }
  }

  synchronized (serial) {
    serial.close(false)
  }
}

watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Total skipped ${skipped.get()}"
println "Eval took " + watch
