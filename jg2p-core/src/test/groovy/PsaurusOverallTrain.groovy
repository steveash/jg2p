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
import com.github.steveash.jg2p.PhoneticEncoderFactory
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer
import com.github.steveash.jg2p.train.SimpleEncoderTrainer
import com.github.steveash.jg2p.util.ReadWrite
import org.slf4j.LoggerFactory

/**
 * driver that trains everything from start to finish and saves each thing
 * @author Steve Ash
 */
def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
def startingIter = 50
def maxIter = 300
def outPrefix = "../resources/psaur_22_xEps_ww_f9A_"

opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxPronouncerTrainingIterations = 100
opts.useWindowWalker = true
opts.includeXEpsilons = true 
opts.trainingAlignerMaximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE
opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_f8A_300.dat"
//opts.alignAllowedFile = new File("../resources/possible-aligns.txt")
def log = LoggerFactory.getLogger("psaurus")
log.info("Starting training with $trainFile and $testFile with opts $opts")

def t = new SimpleEncoderTrainer()
//def t = new JointEncoderTrainer()
def model = t.trainNoEval(train, test, opts)

def trainInps = SimpleEncoderTrainer.makeCrfExamples(train, t.alignModel, opts);

def iters = opts.maxPronouncerTrainingIterations + startingIter
while (iters < maxIter) {
  def temp = new File(outPrefix + iters + ".dat")
  ReadWrite.writeTo(model, temp)
  def aligner = model.aligner
  model = null; // need to clear this memory to get more
  // now create new trainer initing from previous model
  opts.initCrfFromModelFile = temp.canonicalPath
  def trainer = PhonemeCrfTrainer.open(opts)
  trainer.trainFor(trainInps)
  def phoneModel = trainer.buildModel()
  model = PhoneticEncoderFactory.make(aligner, phoneModel)

  iters += opts.maxPronouncerTrainingIterations
}

def temp = new File(outPrefix + iters + ".dat")
ReadWrite.writeTo(model, temp)

log.info("***********************************Finished*************************************")