import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.ReadWrite
import com.github.steveash.jg2p.wfst.G2pFstTrainer
import com.github.steveash.jg2p.wfst.SeqTransducer
import com.github.steveash.jopenfst.io.Convert
import com.github.steveash.kylm.model.ngram.writer.ArpaNgramWriter
import com.github.steveash.kylm.model.ngram.writer.ArpaNgramWriter2
import groovyx.gpars.GParsPool

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
 * Trains the WFST approach to tranduction
 * @author Steve Ash
 */
//def inputFile = "g014b2b.train.syll"
def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inFile = "../resources/pipe_43sy_cmu7_fixsg_1.dat"
def outFile = new File("../resources/fsttran_1.dat")
//def inputFile = "cmudict.2kA.txt"
//def inputFile = "g014b2b.test"
def inputsO = InputReader.makePSaurusReader().readFromClasspath(inputFile)
List<InputRecord> inputs = []
def seenWords = [].toSet()
inputsO.each {
  if (seenWords.add(it.left)) {
    inputs << it
  }
}
println "reading trained aligner from $inFile"
def aligner = ModelReadWrite.readTrainAlignerFrom(inFile)
def aligns = []
GParsPool.withPool {
  aligns = inputs.collectParallel { InputRecord ir ->
    def bests = aligner.align(ir.xWord, ir.yWord, 1)
    if (bests.size() > 0) {
      return bests[0]
    }
    return null;
  }.findAll {it != null}
}
def trainer = new G2pFstTrainer()
println "training..."
def seqtran = trainer.trainWithAligned(aligns, 6)
ReadWrite.writeTo(seqtran, outFile)
println "done writing"
new ArpaNgramWriter().write(trainer.lastLm, "langmodel.arpa")
println "wrote lm arpa"
ReadWrite.readFromFile(SeqTransducer, outFile)
println "check reading good!"
Convert.export(seqtran.fst, "g2pfst")
println "wrote fst text file"