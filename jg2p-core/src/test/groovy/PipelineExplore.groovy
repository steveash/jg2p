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

import com.github.steveash.jg2p.GraphoneSortingEncoder
import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.PipelineEncoder
import com.github.steveash.jg2p.PipelineModel
import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.aligntag.AlignTagModel
import com.github.steveash.jg2p.eval.BulkEval
import com.github.steveash.jg2p.eval.EvalPrinter
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.util.concurrent.RateLimiter
import groovy.transform.Field
import groovyx.gpars.GParsPool
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

def trainFile = "g014b2b.train"
//def testFile = "cmudict.2kA.txt"
def testFile = "g014b2b.test"
@Field def modelFile = "../resources/pipe_22_F9_1.dat"

//def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile)
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)
def watch = Stopwatch.createStarted()
log.info("Starting explore with $testFile with $modelFile")

@Field PipelineModel model = ReadWrite.readFromFile(PipelineModel, new File(modelFile))

procFor("train", train)
procFor("test", test)

watch.stop()
println "Done in $watch"

public procFor(String label, List<InputRecord> input) {
  AtomicInteger total = new AtomicInteger(0), totalRight = new AtomicInteger(0)
  def aligner = model.trainingAlignerModel
  def pron = model.pronouncerModel
  RateLimiter limiter = RateLimiter.create(1.0 / 3.0)
  GParsPool.withPool {
    input.everyParallel() { InputRecord record ->
      def aligns = aligner.align(record.left, record.right, 1)
      if (aligns.size() == 0)
        return true;

      total.incrementAndGet()
      def tags = pron.tag(aligns[0].allXTokensAsList, 1)
      if (tags.isEmpty())
        return true;
      if (tags[0].phoneGrams().equals(aligns[0].allYTokensAsList)) {
        totalRight.incrementAndGet()
      }
      if (limiter.tryAcquire()) {
        println "$label Finished ${total.get()}..."
      }
      return true;
    }
  }
  def tt = total.get()
  def ttr = totalRight.get()
  println "$label records got $ttr / $tt = " + Percent.print(ttr, tt)
}
