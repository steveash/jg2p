import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.phoseq.Perplexity
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.kylm.model.ngram.NgramLM
import com.github.steveash.kylm.model.ngram.smoother.KNSmoother

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

/**
 * @author Steve Ash
 */
out = new GroovyLogger()

def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def smoother = new KNSmoother()
smoother.setSmoothUnigrams(true)//???
def lm = new NgramLM(2, smoother)
//lm.debug = 1

def trainData = train.collect {it.right.value.toArray(new String[0])}
def testData = test.collect {it.right.value.toArray(new String[0])}
lm.trainModel(trainData)
println "Trained the model, now calculating"
Perplexity perpTrain = calcPerp(trainData, lm)
Perplexity perpTest = calcPerp(testData, lm)

println "Overall sentence perplexity (train) " + perpTrain.calculate()
println "Overall average log prob    (train) " + perpTrain.averageNormalLogProb()
println "Overall sentence perplexity  (test) " + perpTest.calculate()
println "Overall average log prob     (test) " + perpTest.averageNormalLogProb()
ReadWrite.writeTo(lm, new File("../resources/lm_2_kn.dat"))


private calcPerp(List<String[]> data, NgramLM lm) {
  def perp = new Perplexity()

  data.each {
    def log10 = lm.getSentenceProb(it)
    if (!Double.isFinite(log10)) {
      println "Got bad value for $it got $log10"
    }
    def sentProb = Math.pow(10.0, log10)

//  println("For " + it.join(" ") + " got log10 $log10 and prob $sentProb")
    perp.addSentenceProb(sentProb, it.length)
  }
  return perp
}

