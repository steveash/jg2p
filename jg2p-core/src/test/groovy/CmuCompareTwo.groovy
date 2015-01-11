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
import com.github.steveash.jg2p.util.ReadWrite
import groovy.transform.Field

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
@Field private Closure examplePrinter = { Encoding old, Encoding neww, InputRecord input ->
  println "  New " + neww
  println "  Old " + old
  println "  Exp " + input.yWord.asSpaceString
  println "-----------------------------"
}

//def trainFile = "cmudict.5kA.txt"
//def testFile = "cmudict.5kB.txt"
def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
//def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)
//def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile)
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def enc = ReadWrite.readFromClasspath(PhoneticEncoder.class, "cmu_all_jt_2eps_winB.model.dat")
//def alignTag = ReadWrite.readFromClasspath(AlignTagModel, "aligntag.dat")
//def enc2 = enc.withAligner(alignTag)

def newWins = []
def oldWins = []
def bothLost = []
int bothWin = 0;
int total = 0;

for (InputRecord input : test) {

  List<PhoneticEncoder.Encoding> ans = enc.encode(input.xWord);
  List<PhoneticEncoder.Encoding> ans2 = ans // enc2.encode(input.xWord);

  total += 1;
  def exp = input.yWord.value

  def old = ans.get(0)
  def neww = ans2.get(0)

  def oldWin = old.phones == exp;
  def newWin = neww.phones == exp;

  if (oldWin && newWin) {
    bothWin += 1;
  } else if (oldWin) {
    oldWins << [old, neww, input]
  } else if (newWin) {
    newWins << [old, neww, input]
  } else {
    bothLost << [old, neww, input];
    println "old " + old + " from " + input
  }

  if (bothLost.size() > 15) break;
}

println " ---- Old Wins ---- "
oldWins.take(15).each examplePrinter

println " ---- New Wins ---- "
newWins.take(15).each examplePrinter

println " ---- Both Lost ---- "
bothLost.take(15).each examplePrinter

println "Both win ${bothWin}"
println "Old win ${oldWins.size()}"
println "New win ${newWins.size()}"
println "Both lost ${bothLost.size()}"
println "Total $total"
