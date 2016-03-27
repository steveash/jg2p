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

import cc.mallet.classify.Trial
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.syll.PhoneSyllTagModel
import com.github.steveash.jg2p.syll.PhoneSyllTagTrainer
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.collect.Sets
import org.apache.commons.lang3.ArrayUtils

/**
 * @author Steve Ash
 */

// construct a Word subclass that records the syllable boundaries
// build input records using that
// build a XyWalker that excludes any phones that would split a syllable boundary

Collection<SWord> trainRecs = loadFile("../resources/syllables.train.txt")
Collection<SWord> testRecs = loadFile("../resources/syllables.test.txt")

public loadFile(String filePath) {
  def recs = new File(filePath).readLines()
      .findAll { it.trim().size() > 0 }
      .collect { line ->
    def fields = line.split("\t")
    if (fields[1].trim().contains(" ") || fields[1].trim().contains("-")) {
      return null
    } else {
      return new SWord(fields[2].trim())
    }
  }.findAll { it != null }
  return recs
}

//recs.take(10).each {println it}

def old = ReadWrite.readFromFile(PhoneSyllTagModel, new File("../resources/syllphonetag.dat"))
def trainer = new PhoneSyllTagTrainer()
trainer.setPullFrom(old.getCrf())
def model = trainer.train(trainRecs)
ReadWrite.writeTo(model, new File("../resources/syllphonetag.dat"))
//def model = ReadWrite.readFromFile(AlignModel, new File("../resources/syllalignmodel.dat"))

println "On training recs..."
int good = 0, goodCount = 0, total = 0, printed = 0
trainRecs.each {
  def predicted = model.syllStarts(it)
  def pp = Sets.newHashSet(predicted)
  def gg = Sets.newHashSet(ArrayUtils.toObject(it.bounds))
  if (pp.size() == gg.size()) goodCount += 1
  if (gg.equals(pp)) {
    good += 1
  } else {
    if (printed++ < 20) {
      println "$it predicted $pp"
    }
  }
  total += 1
}
println "Training set: " + good + " of total " + total + " " + Percent.print(good, total)
println "Training set syll counts: " + goodCount + " of total " + total + " " + Percent.print(goodCount, total)

println "On testing recs..."
good = 0; goodCount = 0; total = 0; printed = 0;
testRecs.each {
  def predicted = model.syllStarts(it)
  def pp = Sets.newHashSet(predicted)
  def gg = Sets.newHashSet(ArrayUtils.toObject(it.bounds))
  if (pp.size() == gg.size()) {
    goodCount += 1
  } else {
    if (printed++ < 10) {
      println " test count wrong  $it predicted $pp"
    }
  }
  if (gg.equals(pp)) {
    good += 1
  }
  total += 1
}
println "Testing set: " + good + " of total " + total + " " + Percent.print(good, total)
println "Testing set syll counts: " + goodCount + " of total " + total + " " + Percent.print(goodCount, total)