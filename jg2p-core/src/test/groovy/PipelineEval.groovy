import com.github.steveash.jg2p.GraphoneSortingEncoder
import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.PipelineEncoder
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.eval.BulkEval
import com.github.steveash.jg2p.eval.EvalPrinter
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import org.slf4j.LoggerFactory

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

//def testFile = "g014b2b.train"
//def testFile = "cmudict.2kA.txt"
def testFile = "g014b2b.test"
def modelFile = "../resources/pipe_22_F9_1.dat"

//def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)
def watch = Stopwatch.createStarted()
log.info("Starting eval of with $testFile with $modelFile")

def model = ReadWrite.readFromFile(PipelineModel, new File(modelFile))
//def enc = new GraphoneSortingEncoder(model)
//def enc = PhoneticEncoder.adapt(model.phoneticEncoder)
def enc = new PipelineEncoder(model)

println "Starting eval with encoder $enc"
def eval = new BulkEval(enc)
def stats = eval.groupAndEval(test)
watch.stop()
log.info("Finished eval took $watch")
EvalPrinter.printTo(out, stats, modelFile)