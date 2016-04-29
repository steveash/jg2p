import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.syll.PhoneSyllTagModel
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Joiner

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

def model = ReadWrite.readFromFile(PhoneSyllTagModel, new File("../resources/syllphonetag.dat"))
def test = InputReader.makeCmuReader().readFromClasspath("cmudict-0.7b")
def grouped = test.groupBy { it.left }
def entries = grouped.entrySet().toList()
Collections.shuffle(entries)
def joiner = Joiner.on(" ")
int toTrain = (int) ((entries.size() as double) * 0.90)
int count = 0
new File("../resources/cmu7b.train").withPrintWriter { pw1 ->
  new File("../resources/cmu7b.test").withPrintWriter { pw2 ->
    entries.each { entry ->
      def pw = (count < toTrain ? pw1 : pw2)
      entry.value.each { record ->
        def starts = model.syllStarts(record.yWord)
        pw.println(record.xWord.asNoSpaceString + "\t" + record.yWord.asSpaceString + "\t" + joiner.join(starts))
      }
      count += 1
      if (count % 5000 == 0) {
        println "did $count"
      }
    }
  }
}
println "done $toTrain in training and ${entries.size() - toTrain} in test"
