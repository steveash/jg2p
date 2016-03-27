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

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.align.AlignerTrainer
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.align.WindowXyWalker
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllPreserving
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.util.ReadWrite

/**
 * @author Steve Ash
 */

// construct a Word subclass that records the syllable boundaries
// build input records using that
// build a XyWalker that excludes any phones that would split a syllable boundary


def recs = new File("../resources/syllables.train.txt").readLines()
    .findAll { it.trim().size() > 0 }
    .collect { line ->
  def fields = line.split("\t")
  if (fields[1].trim().contains(" ") || fields[1].trim().contains("-")) {
    return null
  } else {
    return new InputRecord(Word.fromNormalString(fields[1].trim()), new SWord(fields[2].trim()))
  }
}.findAll { it != null }

//recs.take(10).each {println it}

def opts = new TrainOptions()
opts.maxXGram = 4
opts.maxYGram = 2
opts.onlyOneGrams = false
opts.maxPronouncerTrainingIterations = 200
opts.useCityBlockPenalty = true
opts.useWindowWalker = true
def ww = new WindowXyWalker(opts.makeGramOptions())
def sp = new SyllPreserving(ww)

def at = new AlignerTrainer(opts, sp)
def model = at.train(recs)
ReadWrite.writeTo(model, new File("../resources/syllalignmodel.dat"))
//def model = ReadWrite.readFromFile(AlignModel, new File("../resources/syllalignmodel.dat"))

int skipCount = 0
new File("../resources/syllables.align.txt").withPrintWriter { pw ->
// now get the 1-best and let's tag it
  for (InputRecord rec : recs) {
//    if (rec.left.asSpaceString != "s q u a r e n e s s") continue
    def aligned = model.align(rec.left, rec.right, 1)
    if (aligned.empty) continue;
    def align = aligned.first()
    try {
      def sylls = SyllTagTrainer.makeSyllMarksFor(align, (SWord) rec.right)
      pw.println(align.XAsPipeString + "\t" + align.YAsPipeString + "\t" + align.getAsPipeString(sylls))
    } catch (Exception e) {
      println "Problem getting syllables for " + align + " to " + rec.right + " skipping " + e.message
      skipCount += 1
    }
  }
}
println "done, skipped $skipCount"

//def to = new TrainOptions()
//def at = new AlignerTrainer(to)