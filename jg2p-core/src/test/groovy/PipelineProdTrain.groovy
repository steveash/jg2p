/*
 * Copyright 2016 Steve Ash
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

import com.github.steveash.jg2p.PipelineEncoder
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.eval.BulkEval
import com.github.steveash.jg2p.eval.EvalPrinter
import com.github.steveash.jg2p.syllchain.SyllTagAlignerAdapter
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import org.apache.commons.lang3.time.DateFormatUtils
import org.slf4j.LoggerFactory

/**
 * Driver for generating full models to use in packaging for "production" use of this
 * tool
 * @author Steve Ash
 */
def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inputs1 = InputReader.makePSaurusReader().readFromClasspath(inputFile)
def inputs2 = InputReader.makePSaurusReader().readFromClasspath(testFile)
def inputs = []
inputs.addAll(inputs1)
inputs.addAll(inputs2)

def opts = new TrainOptions()
opts.maxXGram = 4
opts.maxYGram = 3
opts.onlyOneGrams = false
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
opts.useSyllableTagger = true
opts.maxPronouncerTrainingIterations = 400
def inFile = "../resources/pipe_43sy_cmu7_fixsg_1.dat"
opts.initTestingAlignerFromFile = inFile
opts.initCrfFromModelFile = inFile
opts.initPhoneSyllModelFromFile = "../resources/syllphonetag_cmu.dat"
opts.graphoneLanguageModelOrder = 8
opts.graphoneLanguageModelOrderForTraining = 8
opts.trimFeaturesByGradientGain = 1.9
opts.trainAll();
def outFile = "../resources/pipe_43sy_cmu7full_1.dat"
def outFilePipe = "../../../../jg2p-pipe-cmu/src/main/resources/pipeline_cmu_default.dat"
def outFileSyG = "../../../../jg2p-syllg-cmu/src/main/resources/syllg_cmu_default.dat"

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)
def watch = Stopwatch.createStarted()
log.info("Starting training with $inputFile with opts $opts")

def pt = new PipelineTrainer()
def pm = new PipelineModel()
try {
  pt.train(inputs, opts, pm)
} catch (Exception e) {
  log.error("Problem trying to train model ", e)
  // go ahead and continue so that you can save current progress
}
ReadWrite.writeTo(pm, new File(outFile))
pm.trainingAlignerModel = null
ReadWrite.writeTo(pm, new File(outFilePipe))
def aligner = pm.testingAlignerModel
assert aligner instanceof SyllTagAlignerAdapter
def syllg = aligner.syllTagger
ReadWrite.writeTo(syllg, new File(outFileSyG))

watch.stop()
println "Wrote to $outFile"
println "Entire training process took $watch"
log.info("***********************************Finished*************************************")
