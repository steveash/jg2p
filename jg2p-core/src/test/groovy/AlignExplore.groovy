/*
 * Copyright 2015 Steve Ash
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

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.syll.SyllCounter
import com.github.steveash.jg2p.syll.SyllStructure
import com.github.steveash.jg2p.syll.SyllTagTrainer
import com.github.steveash.jg2p.util.Histogram
import com.github.steveash.jg2p.util.JenksBreaks
import com.github.steveash.jg2p.util.ModelReadWrite
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.math3.stat.descriptive.rank.Percentile

/**
 * @author Steve Ash
 */
def model = ModelReadWrite.readTrainAlignerFrom("../resources/pipe_43sy_F11_5.dat")
def model2 = ModelReadWrite.readTestAlignerFrom("../resources/pipe_43sy_F11_5.dat")

def graphs = Word.fromNormalString("CASUALLY")
def phones = new SWord("K AE ZH AH W AH L IY", "0 2 4 6")
def result = model.align(graphs, phones, 1)
def first = result.first()
def sylls = SyllTagTrainer.makeSyllMarksFor(first)
def testFirst = model2.inferAlignments(graphs, 1).first()

println "train aligner = $first"
println "syll marks from train aligner = $sylls"
println "test aligner = $testFirst"
println "syll marks from test aligner = ${testFirst.graphoneSyllableGrams}"
def ss = new SyllCounter()
println "test gets $ss."
println "Done"