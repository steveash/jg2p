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

import cc.mallet.types.Labeling
import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.PhoneticEncoder.Encoding
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.aligntag.AlignTagModel
import com.github.steveash.jg2p.seqbin.SeqBinModel
import com.github.steveash.jg2p.util.ListEditDistance
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.HashMultiset
import groovy.transform.Field
import groovy.transform.ToString
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool
import kylm.model.ngram.NgramLM

import java.util.concurrent.ThreadLocalRandom
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
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

def lm = ReadWrite.readFromFile(NgramLM.class, new File("../resources/lm_6_kn.dat"))

Stopwatch watch = Stopwatch.createStarted()
def counts = ConcurrentHashMultiset.create()
def total = new AtomicInteger(0)
println "Starting..."
GParsPool.withPool {
  inps.everyParallel { InputRecord input ->

    def newTotal = total.incrementAndGet()

    List<Encoding> ans = enc.encode(input.xWord);

    def gg = ans.first()
    def alreadyGood = gg.phones == input.yWord.value
	//println "Should be " + input.yWord.value
    // resort by LM
    def totalPerp = 0
    def perpAndEnc = ans.collect {
      def sortOfPerplex = lm.getSentenceProbNormalized(it.phones.toArray(new String[0]))
      assert sortOfPerplex >= 0
      totalPerp += sortOfPerplex
      return [sortOfPerplex, it]
    }
    perpAndEnc = perpAndEnc.collect {
      [((double) it[0]) / ((double) totalPerp), it[1]]
    }
    perpAndEnc = perpAndEnc.sort {it[0]}
	//println "LM sort: "
	//perpAndEnc.each { println it }
	
    def pp = perpAndEnc.first()
    def lmBestGood = pp[1].phones == input.yWord.value
	
    // now try rescoring based on the perplexity proportion
    perpAndEnc = perpAndEnc.collect {
      [(1.0 - it[0]) * ((Encoding)it[1]).tagProbability(), it[1]]
    }
	perpAndEnc = perpAndEnc.sort {it[0]}.reverse()
	//println "scaled sort: "
	//perpAndEnc.each { println it }

    def scaledLmBestGood = perpAndEnc.first()[1].phones == input.yWord.value

    counts.add("BINNED_" + bin(alreadyGood, "ENC") + "_" + bin(lmBestGood, "LM") +
    "_" + bin(scaledLmBestGood, "SCALED"))

    if (alreadyGood) counts.add("ENC")
    if (lmBestGood) counts.add("LM")
    if (scaledLmBestGood) counts.add("SCALED")

    if (newTotal % 5000 == 0) {
      println "Completed " + newTotal + " of " + inps.size()
    }
	return true;
  }
}
watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Eval took " + watch
def elems = counts.entrySet().collect {it.element}.sort()
for (Object elem : elems) {
  def thisCount = counts.count(elem)
  println elem + "  -  " + thisCount + "  " + Percent.print(thisCount, tot)
}

Object bin(boolean cond, String label) {
  if (cond) return label
  return "NOT" + label
}