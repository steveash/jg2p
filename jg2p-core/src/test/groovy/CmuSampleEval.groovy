import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.EncoderEval
import com.github.steveash.jg2p.train.JointEncoderTrainer
import com.github.steveash.jg2p.train.SimpleEncoderTrainer
import com.github.steveash.jg2p.util.ReadWrite

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

/**
 * Driver for the whole end to end training and eval process so that I can play with the alignment code to improve
 * overall performance by measuring the actual error rates for the overall process
 * @author Steve Ash
 */
def trainFile = "cmudict.2kA.txt"
def testFile = "cmudict.2kB.txt"
def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)
def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile) //.take(200)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.useWindowWalker = true
opts.includeXEpsilons = true 
opts.maximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = -150
opts.initCrfFromModelFile = "../resources/pe_cmudct2ka_2_2_A.dat"

//def t = new SimpleEncoderTrainer(false)
def t = new SimpleEncoderTrainer()
def model = t.trainNoEval(train, test, opts)
ReadWrite.writeTo(model, new File("../resources/pe_cmudct2ka_2_2_B.dat"))
t.eval(model, "FINAL", EncoderEval.PrintOpts.ALL)

println "Wrote model"
