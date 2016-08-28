import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.eval.BulkEval
import com.github.steveash.jg2p.eval.EvalPrinter
import com.github.steveash.jg2p.util.GroovyLogger
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.jg2p.wfst.SeqTransducer
import com.github.steveash.jg2p.wfst.SeqTransducerPhoneticEncoderAdapter
import org.slf4j.LoggerFactory

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

//def model = new LangModelToFst().fromArpa(new File("/home/steve/Documents/phonetisaurus-0.7.8/script/g014b2b/g014b2b.arpa"))

def log = LoggerFactory.getLogger("psaurus")
out = new GroovyLogger(log)

//// single test
//def results = model.translate(Word.fromNormalString("SPORTSING"), 3)
//println "Got " + results.size() + " results "
//results.each {
//  println ">> " + it
//}
//println "DONE"

// test slice test
def testFile = "cmu7b.test"
//def testFile = "g014b2b.test"
def tests = InputReader.makePSaurusReader().readFromClasspath(testFile)
def be = new BulkEval(new SeqTransducerPhoneticEncoderAdapter(5, model))
def scoreResults = be.groupAndEval(tests)

EvalPrinter.printTo(out, scoreResults, testFile)
println "done scoring"
