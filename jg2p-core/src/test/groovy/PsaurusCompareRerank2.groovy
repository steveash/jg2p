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
import com.github.steveash.jg2p.rerank.RerankModel
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
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))

def file = "g014b2b-results.train"
//def file = "g014b2b.test"
//def inps = InputReader.makePSaurusReader().readFromClasspath(file)
def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)  //.take(250)

@Field PhoneticEncoder enc = ReadWrite.
    readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f3_B.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(5)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

@Field NgramLM lm = ReadWrite.readFromFile(NgramLM.class, new File("../resources/lm_7_kn.dat"))
@Field def goodShapes = ["CCvC", "CCv", "CC", "vCCv", "v", "vC", "vCC", "vCCC", "vCvC", "vv", "vCv", "CCC", "CCCv"]

Stopwatch watch = Stopwatch.createStarted()
def total = new AtomicInteger(0)
def skipped = new AtomicInteger(0)
@Field Random rand = new Random(0xCAFEBABE)
def totalPairsPerEntryToInclude = 10
def totalEntriesToInclude = 20000
println "Starting..."

@Field List<String> scoreHeaders = ["lmScore", "tagScore", "alignScore", "uniqueMode", "dups", "alignIndex",
                                    "overallIndex", "shapeEdit", "shapeLenDiff", "leadingConsMatch", "leadingConsMismatch"]
scoreHeaders.addAll(goodShapes)
def headers = ["seq", "word", "phones", "label", "A", "B"]
scoreHeaders.each {
  headers << "A_" + it
  headers << "B_" + it
}

// calculate the probability of including any particular record
double recProb = totalEntriesToInclude.toDouble() / inps.size()
println "Using rec prob of $recProb"
new File("../resources/psaur_rerank_train.txt").withPrintWriter { pw ->
  pw.println(headers.join("\t"))
  GParsPool.withPool {
    inps.everyParallel { InputRecord input ->

      if (rand.nextDouble() > recProb) {
        // skip this record so we get close to the expected # of records
        return true;
      }

      def newTotal = total.incrementAndGet()
      def cans = enc.complexEncode(input.xWord)
      List<Encoding> ans = cans.alignResults.collect {it.encodings}.flatten()
      assert ans.size() > 0
      ans.sort(PhoneticEncoder.OrderByTagScore)
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

      // need the index in the alignment set that the best answer was
      def bestAlignIndex = -1
      cans.alignResults.each { ar ->
        ar.encodings.eachWithIndex { e, idx ->
          if (e.is(bestAns)) {
            bestAlignIndex = idx
          }
        }
      }
      assert bestAlignIndex >= 0
      def wordShape = WordShape.graphShape(input.xWord.value, false)
      def xx = input.xWord.asSpaceString
      def yy = input.yWord.value.join("|")
      def best = score(bestAns, wordShape, bestAlignIndex, modePhones, uniqueMode, dups, ans, xx)
      def pairProb = totalPairsPerEntryToInclude.toDouble() / (ans.size() - 1)

      cans.alignResults.each { alignResult ->
        alignResult.encodings.eachWithIndex { candResult, index ->
          if (candResult.phones == bestAns.phones) {
            return // skip self refs
          }
          if (rand.nextDouble() > pairProb) {
            return // skip this pair so we get close to the number of pairs that we want to eval
          }
          def cand = score(candResult, wordShape, index, modePhones, uniqueMode, dups, ans, xx)

          ArrayList<String> line = makeLine(best, cand, "A", bestAns.phones, candResult.phones, newTotal, xx, yy)
          ArrayList<String> line2 = makeLine(cand, best, "B", candResult.phones, bestAns.phones, newTotal, xx, yy)
          pw.println(line.join("\t"))
          pw.println(line2.join("\t"))
        }
      }

      if (newTotal % 5000 == 0) {
        println "Completed " + newTotal + " of " + inps.size()
      }
      return true;
    }
  }
}

private makeLine(def aa, def bb, String label, List<String> aPhones, List<String> bPhones, int newTotal, String xx, String yy) {
  def line = [newTotal, xx, yy, label, aPhones.join("|"), bPhones.join("|")]
  scoreHeaders.each {
    line << aa[it]
    line << bb[it]
  }
  return line
}

private score(Encoding ans, String wordShape, int alignIndex, List<String> modePhones, boolean uniqueMode,
              Multiset<List<String>> dups, List<Encoding> overall, String spaceSepWord) {

  def ansShape = WordShape.phoneShape(ans.phones, false)
  def leadingConsMatch = false;
  def leadingConsMismatch = false;
  def graphChar = spaceSepWord.substring(0, 1)
  if (Graphemes.isConsonant(graphChar) && Phonemes.isSimpleConsonantGraph(graphChar)) {
    def phoneSymbol = ans.phones.first().substring(0, 1)
    if (Graphemes.isConsonant(phoneSymbol) && graphChar.equalsIgnoreCase(phoneSymbol)) {
      leadingConsMatch = true
    } else {
      leadingConsMismatch = true
    }
  }

  def score = [:]
  score << [lmScore: lm.getSentenceProbNormalized(ans.phones.toArray(new String[0]))]
  score << [tagScore: ans.tagProbability()]
  score << [alignScore: ans.alignScore]
  score << [uniqueMode: (uniqueMode && ans.phones == modePhones ? "1" : "0")]
  score << [dups: (dups.count(ans.phones))]
  score << [alignIndex: alignIndex]
  score << [overallIndex: overall.findIndexOf { it.phones == ans.phones }]
  score << [shapeEdit: StringUtils.getLevenshteinDistance(ansShape, wordShape)]
  score << [shapeLenDiff: wordShape.length() - ansShape.length()]
  score << [leadingConsMatch: (leadingConsMatch ? "1" : "0")]
  score << [leadingConsMismatch: (leadingConsMismatch ? "1" : "0")]

  goodShapes.each { String shp ->
    score.put(shp, (wordShape.startsWith(shp) && ansShape.startsWith(shp) ? "1" : "0"))
  }
  return score
}

watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Total skipped ${skipped.get()}"
println "Eval took " + watch
