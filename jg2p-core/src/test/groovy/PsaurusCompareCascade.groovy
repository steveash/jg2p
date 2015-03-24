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

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
def file = "g014b2b-results.train"
def inps = InputReader.makePSaurusReader().readFromClasspath(file)

def enc1 = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_GB_G2.dat"))
def enc2 = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_GB_B2.dat"))
def seqbin = ReadWrite.readFromFile(SeqBinModel.class, new File("../resources/cmu_gb_seqbin_A.dat"))

Stopwatch watch = Stopwatch.createStarted()
def counts = ConcurrentHashMultiset.create()
def total = new AtomicInteger(0)
GParsPool.withPool {
  inps.everyParallel { InputRecord input ->

    def newTotal = total.incrementAndGet()
    def binlabel = seqbin.classify(input.getLeft())
    List<PhoneticEncoder.Encoding> ans1 = enc1.encode(input.xWord);
    List<PhoneticEncoder.Encoding> ans2 = enc2.encode(input.xWord);

    def gg = ans1.first()
    def bb = ans2.first()
    def goodG = gg.phones == input.yWord.value
    def goodB = bb.phones == input.yWord.value
    def shouldG = input.memo == "G"

    def binned = binlabel.bestLabel.getEntry()
    counts.add("BINNED_" + binned + "_" + goodG + "_" + goodB)

    // how often does classifier match our classifier training data
    if (input.memo == binned) {
      counts.add("SEQBIN_TRAINING_GOOD")
    }

    // let classifier choose and go with it
    if ((binned == "G" && goodG) || (binned == "B" && goodB)) {
      counts.add("BINCHOICE_GOOD")
    }

    // just take the max prob score
    def ggProb = gg.tagProbability()
    def bbProb = bb.tagProbability()
    if (ggProb >= bbProb) {
      if (goodG) counts.add("MAXSCORE_GOOD")
    } else {
      if (goodB) counts.add("MAXSCORE_GOOD")
    }

    // shade the probability based on the classifiers confidence that it belongs in one bin or the other
    double binGProb = (binned == "G" ? binlabel.bestValue : 1.0 - binlabel.bestValue)
    double binBProb = 1.0 - binGProb
    if ((binGProb * ggProb) >= (binBProb * bbProb)) {
      if (goodG) counts.add("WEIGHTED_SCORE_GOOD")
    } else {
      if (goodB) counts.add("WEIGHTED_SCORE_GOOD")
    }

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
