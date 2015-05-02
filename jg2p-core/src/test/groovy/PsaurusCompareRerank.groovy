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
import com.github.steveash.jg2p.util.Fibonacci
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.HashMultiset
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool
import kylm.model.ngram.NgramLM
import org.apache.commons.lang3.StringUtils

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
def file = "g014b2b-results.train"
//def inps = InputReader.makePSaurusReader().readFromClasspath(file)
def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)

def enc = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f3_B.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(15)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

def lm = ReadWrite.readFromFile(NgramLM.class, new File("../resources/lm_7_kn.dat"))

Stopwatch watch = Stopwatch.createStarted()
def counts = ConcurrentHashMultiset.create()
def total = new AtomicInteger(0)
println "Starting..."
new File("../resources/psaur_rerank_out.txt").withPrintWriter { pw ->
  pw.println(
      "word\tphone\tlabel\tA\tB\tA_alignScore\tB_alignScore\tA-B_alignScore\tA_tagProb\tB_tagProb\tA-B_tagProb\tA_lmScore\tB_lmScore\tA-B_lmScore\tA_slmScore\tB_slmScore\tA-B_slmScore\tbigger\tA_dupCount\tB_dupCount\tA-B_dupCount")
  GParsPool.withPool {
    inps.take(250).everyParallel { InputRecord input ->

      def newTotal = total.incrementAndGet()

      List<Encoding> ans = enc.encode(input.xWord);
      counts.add("output_" + Fibonacci.prevFibNumber(ans.size()))
      def dups = HashMultiset.create()
      ans.each { dups.add(it.phones) }
      def modeEntry = dups.entrySet().max { it.count }
      int candidatesSameAsMode = dups.entrySet().count { it.count == modeEntry.count }
      if (candidatesSameAsMode == 1) {
        counts.add("unique_mode")
        counts.add("unique_mode_count_" + modeEntry.count)
        if (modeEntry.element == input.yWord.value) {
          counts.add("unique_mode_correct")
        }
      } else {
        counts.add("nonunique_mode_" + candidatesSameAsMode)
        counts.add("nonunique_mode_count_" + modeEntry.count)
      }
      ans = pruneDups(ans)
      counts.add("dedup_output_" + Fibonacci.prevFibNumber(ans.size()))

      def gg = ans.first()
      def gg2 = ans[1]
      def alreadyGood = gg.phones == input.yWord.value
      def already2ndGood = gg2.phones == input.yWord.value

      // resort by LM
      def totalPerp = 0
      def perpAndEnc = ans.collect {
        def sortOfPerplex = lm.getSentenceProbNormalized(it.phones.toArray(new String[0]))
        assert sortOfPerplex >= 0
        totalPerp += sortOfPerplex
        return [sortOfPerplex, it, sortOfPerplex]
      }
      perpAndEnc = perpAndEnc.collect {
        [((double) it[0]) / ((double) totalPerp), it[1], it[2]]
      }
      perpAndEnc = perpAndEnc.sort { it[0] }
      def lmResults = perpAndEnc

      def pp = perpAndEnc.first()
      def pp2 = perpAndEnc.get(1)
      def lmBestGood = pp[1].phones == input.yWord.value
      def lm2ndGood = pp2[1].phones == input.yWord.value

      // now try rescoring based on the perplexity proportion
      perpAndEnc = perpAndEnc.collect {
        [(1.0 - it[0]) * ((Encoding) it[1]).tagProbability(), it[1], perpAndEnc[2]]
      }
      perpAndEnc = perpAndEnc.sort { it[0] }.reverse()
      def slmResults = perpAndEnc
      //println "scaled sort: "
      //perpAndEnc.each { println it }

      def slm = perpAndEnc.first()
      def slm2 = perpAndEnc.get(1)
      def scaledLmBestGood = slm[1].phones == input.yWord.value


      if (scaledLmBestGood ^ lmBestGood) {
        Encoding aa = pp[1]
        Encoding bb = slm[1]
        def aalm = lmResults.find { it[1].phones == aa.phones }.get(0)
        def bblm = lmResults.find { it[1].phones == bb.phones }.get(0)
        def aapb = slmResults.find { it[1].phones == aa.phones }.get(0)
        def bbpb = slmResults.find { it[1].phones == bb.phones }.get(0)

        // word\tphone\tlabel\tA\tB\tA_alignScore\tB_alignScore\tA-B_alignScore\tA_tagProb\tB_tagProb\tA-B_tagProb\tA_lmScore\tB_lmScore\tA-B_lmScore\tA_slmScore\tB_slmScore\tA-B_slmScore\tbigger\tA_dupCount\tB_dupCount\tA-B_dupCount

        def msg = input.xWord.asSpaceString + "\t" + input.yWord.value.join("|") + "\t" +
                  (lmBestGood ? "LM" : scaledLmBestGood ? "SLM" : "XXX") + "\t" +
                  aa.phones.join("|") + "\t" + bb.phones.join("|") + "\t" +
                  aa.alignScore + "\t" + bb.alignScore + "\t" +
                  (aa.alignScore - bb.alignScore) + "\t" +
                  aa.tagProbability() + "\t" + bb.tagProbability() + "\t" +
                  (aa.tagProbability() - bb.tagProbability()) + "\t" +
                  aalm + "\t" + bblm + "\t" + (aalm - bblm) + "\t" +
                  aapb + "\t" + bbpb + "\t" + (aapb - bbpb) + "\t" +
                  (aa.phones.size() > bb.phones.size() ? "AA_BIGGER" : "BB_BIGGER") + "\t" +
                  dups.count(aa.phones) + "\t" + dups.count(bb.phones) + "\t" +
                  (dups.count(aa.phones) - dups.count(bb.phones)) + "\t"

        aa.phones.toSet().each { msg += "\t" + "A_" + it }
        aa.phones.toSet().each { msg += "\t" + "B_" + it }
        pw.println(msg)
      }

      counts.add("BINNED_" + bin(alreadyGood, "ENC") + "_" + bin(lmBestGood, "LM") +
                 "_" + bin(scaledLmBestGood, "SCALED"))

      if (alreadyGood) {
        counts.add("ENC")
      }
      if (lmBestGood) {
        counts.add("LM")
      }
      if (already2ndGood) {
        counts.add("ENC2")
      }
      if (lm2ndGood) {
        counts.add("LM2")
      }
      if (scaledLmBestGood) {
        counts.add("SCALED")
      }

      if (newTotal % 5000 == 0) {
        println "Completed " + newTotal + " of " + inps.size()
      }
      return true;
    }
  }
}
watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Eval took " + watch
Collection<String> elems = counts.entrySet().collect { it.element }.sort()
def max = elems.max { it.length() }.length()
for (Object elem : elems) {
  def thisCount = counts.count(elem)
  println StringUtils.leftPad(elem, max, ' ' as String) + "  -  " + thisCount + "  " + Percent.print(thisCount, tot)
}

Object bin(boolean cond, String label) {
  if (cond) {
    return label
  }
  return "NOT" + label
}

List<Encoding> pruneDups(List<Encoding> encodings) {
  def result = []
  def seen = [].toSet()
  result << encodings.first()
  seen.add(encodings.first().phones)

  for (int i = 1; i < encodings.size(); i++) {
    def cand = encodings.get(i)
    if (seen.add(cand.phones)) {
      result << cand
    }
  }
  return result
}