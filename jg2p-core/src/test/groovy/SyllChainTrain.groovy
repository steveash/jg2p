import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.syllchain.SyllChainTrainer
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.Percent
import com.google.common.collect.Sets
import com.google.common.primitives.Ints

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

/**
 * @author Steve Ash
 */

def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
def testInputs = InputReader.makePSaurusReader().readFromClasspath(testFile)
println "reading model..."
def aligner = ModelReadWrite.readTrainAlignerFrom("../resources/pipe_43sy_cmu7_orig_oncpsyll.dat")
def aligns = inputs.collectMany { rec ->
  def res = aligner.align(rec.xWord, rec.yWord, 1)
  if (!res.isEmpty()) {
    return [res.first().withSyllWord((SWord) rec.yWord)]
  }
  return []
}
def trainer = new SyllChainTrainer()

println "Training..."
def model = trainer.train(aligns)
println "done training, checking..."

int countWords = 0;
int correctWords = 0;
testInputs.each { rec ->
  def predictStarts = model.tagSyllStarts(rec.left.value)
  def expectWord = rec.right as SWord
  def expect = expectWord.bounds.toSet()
  countWords += 1
  if (predictStarts.equals(expect)) {
    correctWords += 1
  }
}
println "Checked $countWords and got $correctWords completely right " + Percent.print(correctWords, countWords)
//println "Checked $countSylls sylls and got $correctSylls right"