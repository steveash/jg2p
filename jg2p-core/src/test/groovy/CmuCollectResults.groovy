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

import com.github.steveash.jg2p.util.Histogram
import com.github.steveash.jg2p.util.Percent
import groovyx.gpars.GParsConfig
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.operator.PoisonPill
import org.apache.commons.math3.stat.descriptive.SummaryStatistics

import static groovyx.gpars.dataflow.Dataflow.task

import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import groovyx.gpars.GParsPool

import java.util.concurrent.atomic.AtomicInteger

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */


//def file = "cmudict.5kA.txt"
//def file = "cmudict.5kB.txt"
def file = "g014b2b.train"
def outfile = "../resources/g014b2b-results.train"
//def file = "g014b2b.test"
//def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)
def inps = InputReader.makePSaurusReader().readFromClasspath(file)
//Collections.shuffle(inps, new Random(0xCAFEBABE))
//inps = inps.subList(0, (int)(inps.size() / 4));

def enc = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f3.dat"))
//def alignTag = ReadWrite.readFromClasspath(AlignTagModel, "aligntag.dat")
//def enc2 = enc.withAligner(alignTag)

Stopwatch watch = Stopwatch.createStarted()

AtomicInteger total = new AtomicInteger(0)
println "Starting to test..."
GParsPool.withPool {
  final DataflowQueue outq = new DataflowQueue()

  def good = new SummaryStatistics()
  def bad = new SummaryStatistics()
  def goodHisto = new Histogram(0.0, 100.0, 20)
  def badHisto = new Histogram(0.0, 100.0, 20)

  def writer = task {
    new File(outfile).withPrintWriter { pw ->
      while (true) {
        def (InputRecord inp, boolean isGood, double prob) = outq.val
        pw.println(inp.left.asSpaceString + "\t" + inp.right.asSpaceString + "\t" + (isGood ? "G" : "B"))
        synchronized (CmuCollectResults.class) {
          if (isGood) {
            good.addValue(prob)
            goodHisto.add(prob * 100)
          } else {
            bad.addValue(prob)
            badHisto.add(prob * 100)
          }
        }
      }
    }
  }

  inps.everyParallel { InputRecord input ->

    List<PhoneticEncoder.Encoding> ans = enc.encode(input.xWord);
    def newTotal = total.incrementAndGet()

    if (newTotal % 5000 == 0) {
      println "Completed " + newTotal + " of " + inps.size()
    }

    def exp = input.yWord.value
    def neww = ans.get(0)

    def isGood = neww.phones == exp
    outq << [input, isGood, neww.tagProbability()]
    return true;
  }

  println "Done with everything, closing stream..."
  outq << PoisonPill.instance
  writer.join()
  synchronized (CmuCollectResults.class) {
    println "Good stuff\n" + good.toString()
    println "Bad stuff\n" + bad.toString()
    println "-- Histo Range -- Good Perc -- Bad Perc -- "
    for (int i = 0; i < goodHisto.binCount; i++) {
      double goodPerc = Percent.value(goodHisto.getCountAtIndex(i), good.getN())
      double badPerc = Percent.value(badHisto.getCountAtIndex(i), bad.getN())
      println String.format("%s   %.4f   %.4f", goodHisto.getRangeLabelAtIndex(i), goodPerc, badPerc)
    }
  }

}
watch.stop()
GParsConfig.shutdown()
println "done took " + watch.toString()