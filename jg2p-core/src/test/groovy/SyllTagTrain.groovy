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
import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.syll.SyllTagModel
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.util.ReadWrite

/**
 * Takes the syllable input file, trains an aligner, then uses that to assign graphone -> syllable
 * tags and writes those to an output file for inspection
 * @author Steve Ash
 */

def recs = InputReader.makePSaurusReader().readFromFile(new File("../resources/g014b2b.train.syll"))
def testrecs = InputReader.makePSaurusReader().readFromFile(new File("../resources/g014b2b.test.syll"))
def model = ReadWrite.readFromFile(AlignModel, new File("../resources/syllalignmodel.dat"))

def trainAligns = align(recs, model)
def testAligns = align(testrecs, model)

public align(List<InputRecord> recs, AlignModel model) {
  println "aligning records..."
// now get the 1-best and let's tag it
  def aligns = []
  for (InputRecord rec : recs) {
//    if (rec.left.asSpaceString != "s q u a r e n e s s") continue
    def aligned = model.align(rec.left, rec.right, 1)
    if (aligned.empty) {
      continue
    };
    aligns << aligned.first()
  }
  return aligns
}

println "aligned all of the input"

def inModel = ReadWrite.readFromFile(SyllTagModel, new File("../resources/sylltagmodel_orig.dat"))
def trainer = new SyllTagTrainer()
trainer.initFrom = inModel
def smodel = trainer.train(trainAligns, testAligns, true)
ReadWrite.writeTo(smodel, new File("../resources/sylltagmodel.dat"))
println "done training and writing the sylltag model"

//recs.drop(1).take(10).each {
//  def result = smodel.inferAlignments(it.left, 1)
//  println "--> " + result.first()
//}