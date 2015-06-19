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
import com.github.steveash.jg2p.rerank.RerankExample
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.HashMultiset
import com.google.common.util.concurrent.RateLimiter
import groovy.transform.Field
import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsPool

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to create training data for the reranking model
 * this will use the extra generated candidates (with vowelReplacer) but will use a fairly large feature set
 * @author Steve Ash
 */
//def rr = RerankModel.from(new File("../resources/dt_rerank_2.pmml"))

//def file = "g014b2b-results.train"
def file = "g014b2b.test"
def inps = InputReader.makePSaurusReader().readFromClasspath(file)
//def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)

@Field PhoneticEncoder enc = ReadWrite.
    readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_F5_pe1.dat"))
enc.setBestAlignments(5)
enc.setBestTaggings(5)
enc.setBestFinal(25)
enc.alignMinScore = Double.NEGATIVE_INFINITY
enc.tagMinScore = Double.NEGATIVE_INFINITY

def limiter = RateLimiter.create(1.0 / 5.0)
Stopwatch watch = Stopwatch.createStarted()
def total = new AtomicInteger(0)
def correct = new AtomicInteger(0)
println "Starting..."

// calculate the probability of including any particular record
GParsPool.withPool {
  inps.everyParallel { InputRecord input ->

    def newTotal = total.incrementAndGet()
    def cans = enc.complexEncode(input.xWord)
    def ans = cans.overallResults
    assert ans.size() > 0

    if (ans[0].phones == input.yWord.value) {
      correct.incrementAndGet()
    } else {
      if (newTotal < 10) {
        println "Got " + ans[0].phones.join("|") + " for " + input.left.asSpaceString + " but wanted " +
                input.yWord.value.join("|")
      }
    }

    if (newTotal % 64 == 0) {
      if (limiter.tryAcquire()) {
        println "Completed " + newTotal + " of " + inps.size() + " " + Percent.print(newTotal, inps.size())
      }
    }
    return true;
  }
}

watch.stop()
GParsConfig.shutdown()

def tot = total.get()
println "Total $tot"
println "Total correct ${correct.get()} " + Percent.print(correct.get(), total.get())
println "Eval took " + watch
