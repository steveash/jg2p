import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.jg2p.wfst.SeqTransducer

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

def outFile = new File("../resources/fsttran_1.dat")
def model = ReadWrite.readFromFile(SeqTransducer, outFile)
def results = model.translate(Word.fromNormalString("SPORTSING"), 3)
println "Got " + results.size() + " results "
results.each {
  println ">> " + it
}
println "DONE"