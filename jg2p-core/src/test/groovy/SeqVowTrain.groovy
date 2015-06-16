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

import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.seqvow.PartialTagging
import com.github.steveash.jg2p.seqvow.PartialPhones
import com.github.steveash.jg2p.seqvow.RetaggerTrainer
import com.github.steveash.jg2p.util.ReadWrite

/**
 * @author Steve Ash
 */
def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
//def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxCrfIterations = 100
opts.useWindowWalker = true
opts.includeXEpsilons = true
opts.maximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE
//opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_f3_100.dat"

def model = ReadWrite.readFromFile(AlignModel, new File("../resources/am_cmudict_22_xeps_ww_A.dat"))
// training the align model

int count = 0;
def partials = train.collect { InputRecord rec ->
  count += 1;
  if (count % 1024 == 0) {
    println "Read $count..."
  }
  def aligns = model.align(rec.left, rec.right, 1)
  if (aligns.isEmpty()) return null

  def align = aligns.first()
  try {
    def graphonePhones = align.allYTokensAsList
    if (!PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(graphonePhones)) {
      return null;
    }
    return PartialTagging.createFromGraphsAndFinalPhoneGrams(align.allXTokensAsList, graphonePhones)
  } catch (Exception e) {
    throw new IllegalArgumentException("Problem trying to make example from $align", e)
  }

}.findAll { it != null }
println "Got " + partials.size() + " inputs to train on"

def trainer = RetaggerTrainer.open(opts)
trainer.printEval = false;
trainer.trainFor(partials)
trainer.writeModel(new File("../resources/sv_A.dat"))
double selfAccuracy = trainer.accuracyFor(partials)
println "Got accuracy $selfAccuracy"