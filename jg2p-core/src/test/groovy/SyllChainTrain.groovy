import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.syllchain.SyllChainTrainer
import com.github.steveash.jg2p.util.ModelReadWrite
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
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
println "reading model..."
def aligner = ModelReadWrite.readTrainAlignerFrom("../resources/pipe_43sy_F11_5.dat")
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
int countSylls = 0;
int correctSylls = 0;
aligns.each { rec ->
  def predict = model.sylls(rec)
  def expected = SyllTagTrainer.makeSyllMarksFor(rec)
  countWords += 1
  if (predict.graphoneSyllableGrams.equals(expected)) {
    correctWords += 1
  }
//  countSylls += expected.size()
//  correctSylls += Sets.intersection(predict.toSet(), expected.toSet()).size()
}
println "Checked $countWords and got $correctWords completely right"
//println "Checked $countSylls sylls and got $correctSylls right"