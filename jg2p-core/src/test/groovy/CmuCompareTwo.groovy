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
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.jg2p.util.GroovyLogger
import groovy.transform.Field

out = new GroovyLogger()
println "Starting compare two..."

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
def enc = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f3_B.dat"))
def enc2 = ReadWrite.readFromFile(PhoneticEncoder.class, new File("../resources/psaur_22_xEps_ww_f3_aa_A.dat"))
//def alignTag = ReadWrite.readFromClasspath(AlignTagModel, "aligntag.dat")
//def enc2 = enc.withAligner(alignTag)

def newWins = []
def oldWins = []
def bothLost = []
int bothWin = 0;
int total = 0;

for (InputRecord input : train) {

  List<PhoneticEncoder.Encoding> ans = enc.encode(input.xWord);
  List<PhoneticEncoder.Encoding> ans2 = enc2.encode(input.xWord);

  total += 1;
  if (total % 10000 == 0) {
     println "Completed $total"
  }

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
//    println "old " + old + " from " + input
  }

//  if (bothLost.size() > 15) break;
}

println " ---- Old Wins ---- "
oldWins.take(50).each examplePrinter

println " ---- New Wins ---- "
newWins.take(50).each examplePrinter

println " ---- Both Lost ---- "
bothLost.take(50).each examplePrinter

def outFile = new File("../resources/cmubad.3kC.txt")
outFile.withPrintWriter { pw ->
  def ex = []
  Collections.shuffle(oldWins)
  Collections.shuffle(bothLost)
  Collections.shuffle(newWins)
  ex.addAll(oldWins.take(1000))
  ex.addAll(bothLost.take(1000))
  ex.addAll(newWins.take(1000))
  ex.each { Encoding old, Encoding neww, InputRecord input ->
    pw.println(input.xWord.asSpaceString + "\t" + input.yWord.asSpaceString)
  }
  println "Printed ${ex.size()} examples to $outFile"
}

println "Both win ${bothWin}"
println "Old win ${oldWins.size()}"
println "New win ${newWins.size()}"
println "Both lost ${bothLost.size()}"
println "Total $total"
