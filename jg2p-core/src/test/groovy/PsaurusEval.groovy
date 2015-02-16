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

import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.JointEncoderTrainer
import com.github.steveash.jg2p.util.ReadWrite

/**
 * Driver for the whole end to end training and eval process so that I can play with the alignment code to improve
 * overall performance by measuring the actual error rates for the overall process
 * @author Steve Ash
 */
def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.includeXEpsilons = true
opts.onlyOneGrams = true
opts.maxCrfIterations = 100
opts.maximizer = Maximizer.JOINT

//def t = new SimpleEncoderTrainer()
def t = new JointEncoderTrainer()
def model = t.trainAndEval(train, test, opts)
ReadWrite.writeTo(model, new File("../resources/encoder.dat"))
println "Wrote model"