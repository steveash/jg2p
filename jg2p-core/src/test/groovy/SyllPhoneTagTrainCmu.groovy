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

import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.PhoneSyllTagModel
import com.github.steveash.jg2p.syll.PhoneSyllTagTrainer
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.collect.Sets
import org.apache.commons.lang3.ArrayUtils

/**
 * This loads syllable data from the cmu dataset that has alrady been tagged with syllable boundaries
 * and then learns a phone syllable model from that.  Since the cmudict set was tagged using some
 * constraints the taggings arent EXACTLY that of the celex one so maybe this will be better for cmu phones
 * @author Steve Ash
 */

def trainInps1 = InputReader.makePSaurusReader().readFromFile(new File("../resources/cmu7b.train"))
def testInps1 = InputReader.makePSaurusReader().readFromFile(new File("../resources/cmu7b.test"))
def trainInps = trainInps1.collect {it.right as SWord}
def testInps = testInps1.collect {it.right as SWord}

def old = ReadWrite.readFromFile(PhoneSyllTagModel, new File("../resources/syllphonetag.dat"))
def trainer = new PhoneSyllTagTrainer()
trainer.setPullFrom(old.getCrf())
def model = trainer.train(trainInps)
//def model = old
ReadWrite.writeTo(model, new File("../resources/syllphonetag_cmu.dat"))

println "On training recs..."
int good = 0, goodCount = 0, total = 0, printed = 0
trainInps.each {
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
testInps.each {
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