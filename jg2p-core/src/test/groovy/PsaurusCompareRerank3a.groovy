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
import com.github.steveash.jg2p.rerank.Rerank2Model
import com.github.steveash.jg2p.rerank.RerankExample
import com.github.steveash.jg2p.rerank.VowelReplacer
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.*
import com.google.common.math.DoubleMath
import groovy.transform.Field
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool
import kylm.model.ngram.NgramLM

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to evaluate the whole schematic with the reranking model trained from rerank2
 * using the mallet reranking model
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))
// 5b is the last asymm one, 4 is the last symm one, 3 is the best symm one
//@Field RerankModel rr = RerankModel.from(new File("../resources/dt_rerank_3.pmml"))
@Field Rerank2Model rr2 = ReadWrite.readFromFile(Rerank2Model.class, new File("../resources/dt_rerank_F7_2.dat"))
@Field boolean useRr2 = true

//def file = "g014b2b-results.train"
def file = "g014b2b.test"
def inps = InputReader.makePSaurusReader().readFromClasspath(file)
//def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file).take(250)
def grouped = inps.groupBy { it.xWord.asSpaceString }

@Field PhoneticEncoder enc = ReadWrite.
    readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_F7_pe1.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(25)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

@Field NgramLM lm = ReadWrite.readFromFile(NgramLM.class, new File("../resources/lm_7_kn.dat"))

Stopwatch watch = Stopwatch.createStarted()
def total = new AtomicInteger(0)
def right = new AtomicInteger(0)
def counts = ConcurrentHashMultiset.create()
println "Starting..."

def rightWords = Sets.newConcurrentHashSet()

new File("../resources/bad_rerank_wrongorder.txt").withPrintWriter { pwWrong ->
  new File("../resources/bad_rerank_missed.txt").withPrintWriter { pwMissing ->
    GParsPool.withPool {
      grouped.values().everyParallel { Collection<InputRecord> inputs ->

        def input = inputs.first()
        def newTotal = total.incrementAndGet()
        def cans = enc.complexEncode(input.xWord)
        def ans = cans.overallResults.findAll {it.phones != null && !it.phones.isEmpty()}

        assert ans.size() > 0
        def dups = HashMultiset.create()
        ans.each { dups.add(it.phones) }
        def modeEntry = dups.entrySet().max { it.count }
        assert modeEntry != null
        int candidatesSameAsMode = dups.entrySet().count { it.count == modeEntry.count }
        List<String> modePhones = modeEntry.element
        boolean uniqueMode = (candidatesSameAsMode == 1)

        /* using the "best by average odds ranking */

        def graph = HashBasedTable.create()
        for (int i = 0; i < ans.size(); i++) {
          for (int j = 0; j < ans.size(); j++) {
            if (i == j) {
              continue
            };
            def a = ans[i]
            def b = ans[j]
            def pb = probs(a, b, modePhones, uniqueMode, dups, input.left.value)
            def domprob = pb.get(RerankExample.A)
            def ndprob = pb.get(RerankExample.B)
            def logodds = DoubleMath.log2(domprob) - DoubleMath.log2(ndprob)
            graph.put(i, j, logodds)
          }
        }

        // for each vertex calculate the overall sum of odds and see who has the max
        def reranked = new ArrayList(ans.size())
        graph.rowKeySet().each { int i ->
          def sum = graph.row(i).values().sum()
          reranked << [i, sum]
        }
        reranked = reranked.sort { it[1] }.reverse()
        def w = ans.get(reranked[0][0])

        if (inputs.any { it.yWord.value == w.phones }) {
          right.incrementAndGet()
          rightWords.add(input.xWord.asSpaceString)
        } else {
          boolean printed = false
          int i = 0
          reranked.any { r ->
            def cand = ans.get(r[0]).phones
            if (inputs.any { it.yWord.value == cand }) {
              counts.add("RIGHT_" + i)
              synchronized (PsaurusCompareRerank3a.class) {
                pwWrong.println(input.xWord.asSpaceString + "," + reranked[0][0] + "," + w.phones.join("|") + "," +
                                i + "," + r[0] + "," + inputs.collect { it.yWord.value.join("|") }.join(" ~ "))
                printed = true
              }
              return true // stop trying to find the right reranked value
            }
            i += 1
            return false
          }
          if (!printed) {
            pwMissing.println(input.xWord.asSpaceString + "," + reranked[0][0] + "," + w.phones.join("|") + "," +
                              inputs.collect { it.yWord.value.join("|") }.join(" ~ "))
          }
        }

        if (newTotal % 1000 == 0) {
          println "Completed " + newTotal + " of " + inps.size()
        }
        return true;
      }
    }
  }
}

watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Right ${right.get()}"
println "Accuracy " + Percent.print(right.get(), tot)
counts.entrySet().each { Multiset.Entry e ->
  println e.element + " - " + e.count
}
println "Eval took " + watch

//a, b, modePhones, uniqueMode, dups
private probs(Encoding a, Encoding b, List<String> modePhones, boolean uniqueMode,
              Multiset<List<String>> dups, List<String> wordGraphs) {

  def rre = new RerankExample()
  rre.dupCountA = dups.count(a.phones)
  rre.dupCountB = dups.count(b.phones)
  rre.encodingA = a
  rre.encodingB = b
  rre.languageModelScoreA = lm.getSentenceProbNormalized(a.phones.toArray(new String[0]))
  rre.languageModelScoreB = lm.getSentenceProbNormalized(b.phones.toArray(new String[0]))
  rre.uniqueMatchingModeA = uniqueMode && modePhones == a.phones
  rre.uniqueMatchingModeB = uniqueMode && modePhones == b.phones
  rre.wordGraphs = wordGraphs
  return rr2.probabilities(rre)
}
