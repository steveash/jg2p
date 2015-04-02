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

import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.CascadingTrainer
import com.github.steveash.jg2p.train.JointEncoderTrainer
import com.github.steveash.jg2p.train.SimpleEncoderTrainer
import com.github.steveash.jg2p.util.ListEditDistance
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import groovyx.gpars.GParsPool
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicLong

/**
 * Driver for the whole end to end training and eval process so that I can play with the alignment code to improve
 * overall performance by measuring the actual error rates for the overall process
 * @author Steve Ash
 */
def trainFile = "g014b2b-results.train"
def testFile = "g014b2b.test"
def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)

def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxCrfIterations = 100
opts.useWindowWalker = true
opts.includeXEpsilons = true 
opts.maximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE;
opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_f3.dat"

def gFile = new File("../resources/psaur_22_xEps_ww_GB_G1.dat")
def bFile = new File("../resources/psaur_22_xEps_ww_GB_B1.dat")
def sbFile = new File("../resources/cmu_gb_seqbin_A.dat")
assert gFile.exists()
assert bFile.exists()
assert sbFile.exists()

def log = LoggerFactory.getLogger("psaurus")
log.info("Starting training with $trainFile and $testFile with opts $opts")

log.info("Training everything ...")
def t = new CascadingTrainer()
def ce = t.train(train, opts, gFile, bFile, sbFile)
ReadWrite.writeTo(ce, new File("../resources/psaur_22_xEps_ww_CE_A.dat"))

AtomicLong wordTotal = new AtomicLong(0)
AtomicLong wordGood = new AtomicLong(0)
AtomicLong phoneTotal = new AtomicLong(0)
AtomicLong phoneGood = new AtomicLong(0)

GParsPool.withPool {
  test.everyParallel { InputRecord input ->
    def best = ce.encode(input.left)
    def pp = best.first()

    // phone compare
    def edits = ListEditDistance.editDistance(pp.phones, input.yWord.value, 100);

    if (pp.phones == input.yWord.value) {
      wordGood.incrementAndGet()
    }
    wordTotal.incrementAndGet()

    phoneTotal.addAndGet(pp.phones.size())
    phoneGood.addAndGet(pp.phones.size() - edits)
  }
}
log.info("Good words " + wordGood.get() + " of " + wordTotal.get() + " " + Percent.print(wordGood.get(), wordTotal.get()))
log.info("Good phones " + phoneGood.get() + " of " + phoneTotal.get() + " " + Percent.print(phoneGood.get(), phoneTotal.get()))

log.info("***********************************Finished*************************************")
