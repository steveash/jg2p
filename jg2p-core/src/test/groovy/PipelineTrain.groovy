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

import com.github.steveash.jg2p.PipelineEncoder
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.eval.BulkEval
import com.github.steveash.jg2p.eval.EvalPrinter
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import org.apache.commons.lang3.time.DateFormatUtils
import org.slf4j.LoggerFactory

/**
 * Driver for the whole end-to-end training process
 * @author Steve Ash
 */
//def inputFile = "g014b2b.train.syll"
def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
//def inputFile = "cmudict.2kA.txt"
//def inputFile = "g014b2b.test"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
//def inputs = InputReader.makeDefaultFormatReader().readFromClasspath(inputFile)

def testImmediately = true
def opts = new TrainOptions()
opts.maxXGram = 4
opts.maxYGram = 3
opts.onlyOneGrams = false
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
opts.useSyllableTagger = true
opts.maxPronouncerTrainingIterations = 400
def inFile = "../resources/pipe_43sy_cmu7_F11_5.dat"
opts.initTrainingAlignerFromFile = inFile
opts.initTestingAlignerFromFile = inFile
opts.initCrfFromModelFile = inFile
opts.initPhoneSyllModelFromFile = "../resources/syllphonetag.dat"
//opts.initRerankerFromFile = "../resources/pip_rr.dat"
opts.graphoneLanguageModelOrder = 8
opts.graphoneLanguageModelOrderForTraining = 8
opts.trainTrainingAligner = false
//opts.trainTestingAligner = false
//opts.trimFeaturesByGradientGain = 2.1

opts.trainAll();
//opts.useInputRerankExampleCsv = "../resources/pip_rre.csv"
//opts.trainReranker = true
//opts.writeOutputRerankExampleCsv = "../resources/rerank_inputs.csv"
def outFile = "../resources/pipe_43sy_cmu7_F11_6.dat"


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
watch.stop()
println "Wrote to $outFile"
println "Entire training process took $watch"
log.info("***********************************Finished*************************************")

if (testImmediately) {
  println "testing right now..."
  def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
  pm.makeSparse()
  def enc = new PipelineEncoder(pm)

  println "Starting eval with encoder $enc"
  def eval = new BulkEval(enc)
  def stats = eval.groupAndEval(test)
//  def ts = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss")
//  EvalPrinter.writeExamples(new File("target/badexamples-${ts}.txt"), stats)
  EvalPrinter.printTo(out, stats, outFile)
}
