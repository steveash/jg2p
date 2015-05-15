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
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.ArrayTable
import com.google.common.collect.HashBasedTable
import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Table
import com.google.common.math.DoubleMath
import groovy.transform.Field
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool
import kylm.model.ngram.NgramLM
import org.apache.commons.lang3.StringUtils
import org.jpmml.evaluator.ProbabilityClassificationMap

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to evaluate the whole schematic with the reranking model trained from rerank2
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))
@Field RerankModel rr = RerankModel.from(new File("../resources/dt_rerank_4.pmml"))

//def file = "g014b2b-results.train"
def file = "g014b2b.test"
def inps = InputReader.makePSaurusReader().readFromClasspath(file)
//def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file).take(250)

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
def right = new AtomicInteger(0)
println "Starting..."

@Field List<String> scoreHeaders = ["lmScore", "tagScore", "alignScore", "uniqueMode", "dups", "alignIndex",
                                    "overallIndex", "shapeEdit", "shapeLenDiff", "leadingConsMatch", "leadingConsMismatch"]
scoreHeaders.addAll(goodShapes)

GParsPool.withPool {
  inps.everyParallel { InputRecord input ->

    def newTotal = total.incrementAndGet()
    def cans = enc.complexEncode(input.xWord)
    List<Integer> ansAlignIndex = cans.alignResults.collectMany { (0..<(it.encodings.size())).collect() }
    List<Encoding> ans = cans.alignResults.collectMany { it.encodings }
    assert ans.size() > 0
    assert ansAlignIndex.size() == ans.size()
    def encToAlign = new IdentityHashMap<Encoding, Integer>()
    for (int i = 0; i < ans.size(); i++) {
      encToAlign.put(ans.get(i), ansAlignIndex.get(i))
    }
    ans.sort(PhoneticEncoder.OrderByTagScore)
    def dups = HashMultiset.create()
    ans.each { dups.add(it.phones) }
    def modeEntry = dups.entrySet().max { it.count }
    assert modeEntry != null
    int candidatesSameAsMode = dups.entrySet().count { it.count == modeEntry.count }
    List<String> modePhones = modeEntry.element
    boolean uniqueMode = (candidatesSameAsMode == 1)
    def xx = input.xWord.asSpaceString
    def wordShape = WordShape.graphShape(input.xWord.value, false)

    /* using the "best by average odds ranking */

    def graph = HashBasedTable.create()
    for (int i = 0; i < ans.size(); i++) {
      for (int j = 0; j < ans.size(); j++) {
        if (i == j) continue;
        def a = ans[i]
        def b = ans[j]
        def aindex = encToAlign.get(a)
        def bindex = encToAlign.get(b)
        def pb = probs(a, b, wordShape, aindex, bindex, modePhones, uniqueMode, dups, ans, xx)
        def aprob = pb.getProbability("A")
        def bprob = pb.getProbability("B")
	def logodds = DoubleMath.log2(aprob) - DoubleMath.log2(bprob)
        graph.put(i, j, logodds)
      }
    }

    // for each vertex calculate the overall sum of odds and see who has the max
    def maxOdds = Double.NEGATIVE_INFINITY
    def bestIndex = -1
    graph.rowKeySet().each { int i ->
      def sum = graph.row(i).values().sum()
      if (sum > maxOdds) {
        maxOdds = sum
        bestIndex = i
      }
    }
    def w = ans.get(bestIndex)

    /*** this is the align approach ***/
/*
    // go up each align group (worst to best) to find "the best" showing preference based on the tag prob order
    def alignWinners = [] // winners of each align group, in the same order
    cans.alignResults.each { alignGroup ->
      def w = null
      def windex = -1;
      for (int i = alignGroup.encodings.size(); i >= 0; i--) {
        def c = alignGroup.encodings[i]
        if (w == null) {
          w = c
          windex = i
          continue
        }
        w = winner(w, c, wordShape, windex, i, ans, modePhones, uniqueMode, dups, xx)
      }
      alignWinners << [w, windex]
    }

    def w = null
    def windex = -1;
    for (int i = alignWinners.size() - 1; i >= 0; i--) {
      Encoding c; int cindex;
      (c, cindex) = alignWinners[i]
      if (w == null) {
        w = c
        windex = cindex
        continue
      }
      w = winner(w, c, wordShape, windex, cindex, ans, modePhones, uniqueMode, dups, xx)
    }
    */

    if (w.phones == input.yWord.value) {
      right.incrementAndGet()
    }

    if (newTotal % 1000 == 0) {
      println "Completed " + newTotal + " of " + inps.size()
    }
    return true;
  }
}

watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Right ${right.get()}"
println "Accuracy " + Percent.print(right.get(), tot)
println "Eval took " + watch

private Encoding winner(Encoding a, Encoding b, String wordShape, int aAlignIndex, int bAlignIndex, List<Encoding> overall, List<String> modePhones,
                        boolean uniqueMode, Multiset<List<String>> dups, String spaceSepWord) {

  ProbabilityClassificationMap probs =
      probs(a, b, wordShape, aAlignIndex, bAlignIndex, modePhones, uniqueMode, dups, overall, spaceSepWord)
  def label = (String) probs.getResult()
  if (label == "A") {
    return a
  } else if (label == "B") {
    return b
  }
  throw new Exception("didn't get A or B got $label")
}

private probs(Encoding a, Encoding b, String wordShape, int aAlignIndex, int bAlignIndex, List<String> modePhones, boolean uniqueMode,
              Multiset<List<String>> dups, List<Encoding> overall, String spaceSepWord) {
  def aScore = score(a, wordShape, aAlignIndex, modePhones, uniqueMode, dups, overall, spaceSepWord)
  def bScore = score(b, wordShape, bAlignIndex, modePhones, uniqueMode, dups, overall, spaceSepWord)
  def s = [:]
  scoreHeaders.each { h ->
    def aa = aScore[h]
    def bb = bScore[h]
    assert aa != null && bb != null
    s.put("A_" + h, aa)
    s.put("B_" + h, bb)
  }
  def probs = rr.probabilities(s)
  return probs
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