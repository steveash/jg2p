import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.seqvow.PartialTagging
import com.github.steveash.jg2p.seqvow.PartialPhones

import com.github.steveash.jg2p.util.ReadWrite

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
def inpFile = "g014b2b.train"
//def inpFile = "g014b2b.test"
def inps = InputReader.makePSaurusReader().readFromClasspath(inpFile)
//inps = inps.findAll {it.left.asSpaceString == "A F F L U E N T"}

def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxPronouncerTrainingIterations = 130
opts.useWindowWalker = true
opts.includeXEpsilons = true
opts.trainingAlignerMaximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE
//opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_f3_100.dat"

//def model = ReadWrite.readFromFile(AlignModel, new File("../resources/am_cmudict_22_xeps_ww_A.dat"))
def pe = ReadWrite.readFromFile(PhoneticEncoder, new File("../resources/psaur_22_xEps_ww_F6_pe1.dat"))
def model = pe.alignModel
// training the align model

def partials = inps.collect { InputRecord rec ->

  def aligns = model.align(rec.left, rec.right, 1)
  if (aligns.isEmpty()) return null

  def align = aligns.first()
  try {
    def graphonePhones = align.allYTokensAsList
    if (!PartialPhones.doesAnyGramContainPhoneEligibleAsPartial(graphonePhones)) {
      return null;
    }
    return PartialTagging.createFromGraphsAndOriginalPredictedPhoneGrams(align.allXTokensAsList, graphonePhones)
  } catch (Exception e) {
    throw new IllegalArgumentException("Problem trying to make example from $align", e)
  }

}.findAll { it != null }
println "Got " + partials.size() + " inputs to train on"

def trainer = RetaggerTrainer.open(opts)
trainer.printEval = false;
trainer.trainFor(partials)
pe.setRetagger(trainer.buildModel());
ReadWrite.writeTo(pe, new File("../resources/psaur_22_xEps_ww_F6_retag_pe1.dat"))

double selfAccuracy = trainer.accuracyFor(partials)
println "Got accuracy $selfAccuracy"