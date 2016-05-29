import com.github.steveash.jg2p.align.AlignerTrainer
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.align.WindowXyWalker
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllPreserving
import com.github.steveash.jg2p.syllchain.SyllChainTrainer
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import org.slf4j.LoggerFactory

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
 * trains just the syllabifier_g and tests it
 * @author Steve Ash
 */

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)

def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
def testInputs = InputReader.makePSaurusReader().readFromClasspath(testFile)
println "reading model..."
//def aligner = ModelReadWrite.readTrainAlignerFrom("../resources/pipe_43sy_cmu7_orig_1.dat")
def aligner = ModelReadWrite.readTrainAlignerFrom("../resources/syllchainAlignConstrained.dat") 
def syllgmodel = ModelReadWrite.readSyllTagFrom("../resources/pipe_43sy_cmu7_orig_1.dat")
//def syllmodel = ReadWrite.readFromFile(PhoneSyllTagModel.class, new File("../resources/syllphonetag.dat"))

def opts = new TrainOptions()
opts.maxXGram = 4
opts.maxYGram = 3
opts.onlyOneGrams = false
opts.maxPronouncerTrainingIterations = 200
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
def ww = new WindowXyWalker(opts.makeGramOptions())
def sp = new SyllPreserving(ww)

println "training the training aligner with syll constraints"
//def at = new AlignerTrainer(opts, sp)
//at.initFrom = aligner.transitions
//def alignModel = at.train(inputs)
alignModel = aligner
println "writing model out"
//ReadWrite.writeTo(alignModel, new File("../resources/syllchainAlignConstrained.dat"))

println "collecting alignments with the trained model"
def aligns = inputs.collectMany { rec ->
  def res = alignModel.align(rec.xWord, rec.yWord, 1)
  if (!res.isEmpty()) {
    def alg = res.first()
    return [alg]
  }
  return []
}

println "trainig the syll chain model now that i have alignments"
def trainer = new SyllChainTrainer()
trainer.setInitFrom(syllgmodel.getCrf())

//println "Training..."
def model = trainer.train(aligns)
println "done training, checking..."

int countWords = 0;
int correctWords = 0;
int correctSylls = 0;
testInputs.each { rec ->
  def predictStarts = model.tagSyllStarts(rec.left.value)
  def expectWord = rec.right as SWord
  def expect = expectWord.bounds.toSet()
  countWords += 1
  if (predictStarts.equals(expect)) {
    correctWords += 1
  }
//  int ruleCount = RuleSyllabifier.syllable(rec.left.asNoSpaceString)
  if (predictStarts.size() == expect.size()) {
    correctSylls += 1
  }
}
println "Checked $countWords and got $correctWords completely right " + Percent.print(correctWords, countWords)
println "Predicted right count of syllables for $correctSylls words " + Percent.print(correctSylls, countWords)
