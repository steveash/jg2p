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
import com.github.steveash.jg2p.seq.PhonemeCrfModel
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer
import com.github.steveash.jg2p.seq.SeqInputReader
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Charsets

import static com.google.common.io.Resources.asCharSource
import static com.google.common.io.Resources.getResource

/**
 * Trains just the phoneme model from the alignment text
 */

def file = "cmubad.2kA.align.txt"
def input = new SeqInputReader().readInput(asCharSource(getResource(file), Charsets.UTF_8))
def aligns = input.take(500).collect{it.alignments}.flatten()
def trainer = PhonemeCrfTrainer.open()
trainer.setPrintEval(true)

def crf = ReadWrite.readFromFile(PhonemeCrfModel, new File("../resources/crf_cmubad2kA_500.dat"))
trainer.setInitFromModel(new File("../resources/crf_cmubad2kA_500.dat"))
trainer.trainFor(aligns)

//new PhonemeACrfTrainer().train(aligns)
//new PhonemeACrfTrainer2().train(aligns)

//trainer.writeModel(new File("../resources/crf_cmubad2kA_500.dat"))

