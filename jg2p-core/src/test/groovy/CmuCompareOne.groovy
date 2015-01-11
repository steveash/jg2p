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

import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.PhoneticEncoder.Encoding
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.aligntag.AlignTagModel
import com.github.steveash.jg2p.util.ListEditDistance
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Stopwatch
import com.google.common.collect.HashMultiset
import groovy.transform.Field
import groovy.transform.ToString

import java.util.concurrent.ThreadLocalRandom

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
def oneEditCounts = HashMultiset.create()

Closure examplePrinter = { String name, Encoding neww, InputRecord input ->
  def edits = ListEditDistance.editDistance(neww.phones, input.yWord.value, 10)
  def more = ""
  if (edits == 1 && neww.phones.size() == input.yWord.unigramCount()) {
    for (int i = 0; i < neww.phones.size(); i++) {
      def newp = neww.phones.get(i)
      def yp = input.yWord.value.get(i)

      if (!newp.equals(yp)) {
        more = " : " + newp + " -> " + yp
        oneEditCounts.add(newp + " -> " + yp)
        break;
      }
    }
  }
  println "  New " + neww
  println "  Exp " + input.yWord.asSpaceString
  println "      " + name + ", edits " + edits + more
  println "-------------------------------------------"
}

//def file = "cmudict.5kA.txt"
//def file = "cmudict.5kB.txt"
def file = "g014b2b.train"
//def file = "g014b2b.test"
//def inps = InputReader.makeDefaultFormatReader().readFromClasspath(file)
def inps = InputReader.makePSaurusReader().readFromClasspath(file)
Collections.shuffle(inps, new Random(0xCAFEBABE))
inps = inps.subList(0, (int)(inps.size() / 4));

def enc = ReadWrite.readFromClasspath(PhoneticEncoder.class, "cmu_all_jt_2eps_winB.model.dat")
//def alignTag = ReadWrite.readFromClasspath(AlignTagModel, "aligntag.dat")
//def enc2 = enc.withAligner(alignTag)

class Entry {
  int count = 0
  List examples = []

  void addExample(Encoding enc, InputRecord input) {
    count += 1

    if (examples.size() < 333) {
      examples << [enc, input]
    } else {
      def next = ThreadLocalRandom.current().nextInt(0, count)
      if (next < examples.size()) {
        examples[next] = [enc, input]
      }
    }

  }

}

class Counts {
  int wins = 0;
  int total = 0;
  def winExamples = []
  def lostRightAlign = new Entry()
  def lostWrongAlign = new Entry()
  def lostWrongAlignGgtP = new Entry()
  def lostWrongAlignGltP = new Entry()
  def lostWrongAlignGeqP = new Entry()

  // this closure should take 3 args: fieldName, Encoding, Input
  void eachExample(Closure c) {
    this.getProperties().each {k,v ->
      if (v instanceof Entry) {
        v.examples.each { c(k, it[0], it[1]) }
      }
    }
  }

  boolean isDone() {
    winExamples.size() >= 666 &&
            lostRightAlign.examples.size() >= 333 &&
            lostWrongAlignGgtP.examples.size() >= 333 &&
            lostWrongAlignGeqP.examples.size() >= 333 &&
            lostWrongAlignGltP.examples.size() >= 333
  }

  @Override
  String toString() {
    StringBuilder s = new StringBuilder()
    s.append("wins = $wins\ntotal = $total")
    this.getProperties().each {k,v ->
      if (v instanceof Entry) {
        s.append("\n").append(k).append(" = ").append(v.count)
      }
    }
    return s.toString()
  }
}
def c = new Counts()
Stopwatch watch = Stopwatch.createStarted()

for (InputRecord input : inps) {

//  if (c.total > 50) break;
  if (c.isDone()) {
    break;
  }

  List<PhoneticEncoder.Encoding> ans = enc.encode(input.xWord);
  c.total += 1;

  if (c.total % 5000 == 0) {
    println "Completed " + c.total + " of " + inps.size()
  }

  def exp = input.yWord.value
  def neww = ans.get(0)

  if (neww.phones == exp) {
    c.wins += 1
    if (c.winExamples.size() < 333) {
      c.winExamples << [neww, input]
    }
    continue;
  }

  def expc = exp.size()
  def algc = neww.alignment.size()
  def gc = input.xWord.unigramCount()

  if (expc == algc) {
    c.lostRightAlign.addExample(neww, input)
  } else {
    c.lostWrongAlign.addExample(neww, input)
    if (gc > expc) {
      c.lostWrongAlignGgtP.addExample(neww, input)
    } else if (gc < expc) {
      c.lostWrongAlignGltP.addExample(neww, input)
    } else {
      c.lostWrongAlignGeqP.addExample(neww, input)
    }

  }
}
watch.stop()

/*c.eachExample examplePrinter
println "Counts of errors with 1 phoneme error"
oneEditCounts.entrySet().each { println it.element + " = " + it.count}
println "Done! Counts=\n" + c.toString()
println "Eval took " + watch
*/
def ex = []
ex.addAll(c.winExamples.subList(0, 333))
ex.addAll(c.lostRightAlign.examples.subList(0, 333))
ex.addAll(c.lostWrongAlignGeqP.examples.subList(0, 333))
ex.addAll(c.lostWrongAlignGgtP.examples.subList(0, 333))
ex.addAll(c.lostWrongAlignGltP.examples.subList(0, 333))

Collections.shuffle(ex)
new File("cmubad.2kA.txt").withPrintWriter { pw ->
  ex.each {
    InputRecord inp = it[1]
    pw.println(inp.left.asSpaceString + "\t" + inp.right.asSpaceString)
  }
}
println "done!"