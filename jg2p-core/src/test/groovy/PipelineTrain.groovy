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
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.train.SimpleEncoderTrainer
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import org.slf4j.LoggerFactory

/**
 * Driver for the whole end-to-end training process
 * @author Steve Ash
 */
//def trainFile = "g014b2b.train"
def trainFile = "cmudict.2kA.txt"
def testFile = "g014b2b.test"
//def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)
//def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxPronouncerTrainingIterations = 50
//def inFile = "../resources/pipe_22_F9.dat"
//opts.initTrainingAlignerFromFile = inFile
//opts.initTestingAlignerFromFile = inFile
//opts.initCrfFromModelFile = inFile
opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_f8A_300.dat"
//opts.initTrainingAlignerFromFile = opts.initCrfFromModelFile = "../resources/pipe_22_F9_1.dat"
//opts.trainTrainingAligner = false
//opts.trainTestingAligner = opts.trainPronouncer = true
def outFile = "../resources/pipe_22_F9_1.dat"

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)
def watch = Stopwatch.createStarted()
log.info("Starting training with $trainFile and $testFile with opts $opts")

def pt = new PipelineTrainer()
def pm = new PipelineModel()
try {
  pt.train(train, opts, pm)
} catch (Exception e) {
  log.error("Problem trying to train model ", e)
  // go ahead and continue so that you can save current progress
}
ReadWrite.writeTo(pm, new File(outFile))
watch.stop()
println "Wrote to $outFile"
println "Entire training process took $watch"
log.info("***********************************Finished*************************************")
