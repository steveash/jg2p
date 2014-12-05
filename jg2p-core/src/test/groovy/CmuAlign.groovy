/*
 * Copyright 2014 Steve Ash
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

import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.jg2p.util.Histogram
import groovy.transform.Field

/*
Script the builds the CMU alignment model and generates some stats
 */
def cmuFile = "cmudict.0.7a"
def model = ReadWrite.readFromClasspath(AlignModel, "cmua_2eps.model.dat")
def training = InputReader.makeCmuReader().readFromClasspath(cmuFile)
def out = new File("../../../target/cmua_2eps.align.txt")

@Field def top1Histo = new Histogram(-200, 0, 25)
@Field def top2Histo = new Histogram(-200, 0, 25)
@Field def top3Histo = new Histogram(-200, 0, 25)
@Field def top4Histo = new Histogram(-200, 0, 25)
@Field def countHisto = new Histogram(0, 5, 5)

out.withPrintWriter { pw ->
  int count = 0
  int noPaths = 0
  training.each {
    def results = model.align(it.left, it.right, 5)
    updateStats(results)

    boolean wroteOne = false
    results.each { res ->
      if (res.score < -130) return;
      pw.printf("%d^%.4f^%s^%s^%s\n", count, res.score, res.wordAsSpaceString, res.getXAsPipeString(), res.getYAsPipeString())
      wroteOne = true
    }
    if (!wroteOne) {
      noPaths += 1
    }

    if (++count % 500 == 0) {
      println "Finished $count alignments"
    }
  }
  println "done with everything!"
  println "No paths for $noPaths entries"
  printHisto("pathCounts", countHisto)
  printHisto("score at position1", top1Histo)
  printHisto("score at position2", top2Histo)
  printHisto("score at position3", top3Histo)
  printHisto("score at position4", top4Histo)
}

private updateStats(List<Alignment> results) {
  countHisto.add(results.size())
  if (results.size() >= 1) {
    top1Histo.add(results[0].score)
  }
  if (results.size() >= 2) {
    top2Histo.add(results[1].score)
  }
  if (results.size() >= 3) {
    top3Histo.add(results[2].score)
  }
  if (results.size() >= 4) {
    top4Histo.add(results[3].score)
  }
}

def printHisto(String label, Histogram histo) {
  println label
  println "-------------------------------------"
  histo.iteratorNonEmptyBins().each {
    println it
  }
  println "-------------------------------------"
}