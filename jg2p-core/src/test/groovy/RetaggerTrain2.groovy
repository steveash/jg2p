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
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.seqvow.PartialPhones
import com.github.steveash.jg2p.seqvow.PartialTagging
import com.github.steveash.jg2p.seqvow.RetaggerTrainer
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.util.concurrent.RateLimiter
import groovy.transform.Field
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to create training data for the reranking model
 * this will use the extra generated candidates but will use a fairly large feature set
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))

def file = "g014b2b-results.train"
//def file = "g014b2b.test"
//def inps = InputReader.makePSaurusReader().readFromClasspath(file)
def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)

@Field PhoneticEncoder enc = ReadWrite.
    readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_F7_pe1.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(25)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

def limiter = RateLimiter.create(1.0 / 5.0)
Stopwatch watch = Stopwatch.createStarted()
def total = new AtomicInteger(0)
def topContainsRight = new AtomicInteger(0)
def topContainsRightPartials = new AtomicInteger(0)
def notCovered = new AtomicInteger(0)
def exs = new ConcurrentLinkedDeque()

GParsPool.withPool {
  inps.everyParallel { InputRecord input ->

    def newTotal = total.incrementAndGet()
    def cans = enc.complexEncode(input.xWord)
    def ans = cans.overallResults

    assert ans.size() > 0

    def bestAns = ans.find { input.yWord.value == it.phones }

    if (bestAns != null) {
      topContainsRight.incrementAndGet()
      if (PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(bestAns.graphones)) {
        // this is what the model is already getting right
        def pt = PartialTagging.createFromGraphsAndOriginalPredictedPhoneGrams(bestAns.alignment, bestAns.graphones)
        pt.setExpectedPhonesGrams(bestAns.graphones)
        exs.add(pt)
      }
    } else {
      def rightPartial = PartialPhones.phoneGramsToPartialPhoneGrams(input.yWord.value)
      def matchingPartial = ans.find {PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(it.graphones) &&
                                      PartialPhones.phoneGramsToPartialPhoneGrams(it.graphones) == rightPartial}
      if (matchingPartial != null) {
        topContainsRightPartials.incrementAndGet()

        def pt = PartialTagging.createFromGraphsAndOriginalPredictedPhoneGrams(matchingPartial.alignment, matchingPartial.graphones)
        pt.setExpectedPhonesGrams(input.yWord.value)
        exs.add(pt)
      } else {
        notCovered.incrementAndGet()
      }
    }

    if (newTotal % 256 == 0) {
      if (limiter.tryAcquire()) {
        println "Completed " + newTotal + " of " + inps.size() + " " + Percent.print(newTotal, inps.size())
      }
    }
    return true;
  }
}

watch.stop()
GParsConfig.shutdown()


def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxCrfIterations = 130
opts.useWindowWalker = true
opts.includeXEpsilons = true
opts.maximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE
def trainer = RetaggerTrainer.open(opts)
trainer.printEval = false;
trainer.trainFor(exs)
enc.setRetagger(trainer.buildModel());
ReadWrite.writeTo(enc, new File("../resources/psaur_22_xEps_ww_F7_retag_pe1.dat"))

double selfAccuracy = trainer.accuracyFor(exs)
println "Got accuracy $selfAccuracy"

println "Total ${total.get()}"
println "Total top contained good ${topContainsRight.get()}"
println "Total top contains right partial ${topContainsRightPartials.get()}"
println "Not covered ${notCovered.get()}"
println "Eval took " + watch

println "Got " + exs.size() + " inputs to train on"
