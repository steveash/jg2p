import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.syll.PhoneSyllTagModel
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Joiner
import com.sun.corba.se.spi.orbutil.fsm.Input

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
 * @author Steve Ash
 */

def model = ReadWrite.readFromFile(PhoneSyllTagModel, new File("../resources/syllphonetag.dat"))
def test = InputReader.makePSaurusReader().readFromClasspath("g014b2b.train")
def joiner = Joiner.on(" ")
new File("../resources/g014b2b.train.syll").withPrintWriter { pw ->
  for (InputRecord record : test) {
    def starts = model.syllStarts(record.yWord)
    pw.println(record.xWord.asNoSpaceString + "\t" + record.yWord.asSpaceString + "\t" + joiner.join(starts))
  }
}
println "done"