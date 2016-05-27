import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.PhoneSyllTagModel
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Joiner
import com.google.common.collect.HashMultimap
import com.google.common.collect.HashMultiset

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

/**
 * Takes the trained syll phone model and runs through the input cmu data in order to assign syllable
 * boundaries based on that
 * @author Steve Ash
 */

//def model = ReadWrite.readFromFile(PhoneSyllTagModel, new File("../resources/syllphonetag.dat"))
def test = InputReader.makePSaurusReader().readFromClasspath("cmu7b.train")
def sylls = HashMultiset.create()
int zero = 0;
int many = 0;
test.each { rec ->
  def sword = rec.yWord as SWord
  int thisFirst = sword.syllableStress.count { it == 1 }
  int firstIndex = sword.syllableStress.findIndexOf {it == 1}
  sylls.add(firstIndex)
  if (thisFirst == 0) {
    zero+= 1;
  }
  if (thisFirst > 1) {
    many += 1
  }
}
println "zero stressses $zero"
println "many stresses $many"
sylls.entrySet().each {
  println "$it"
}