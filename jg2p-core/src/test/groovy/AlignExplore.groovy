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
import com.github.steveash.jg2p.util.Histogram
import com.github.steveash.jg2p.util.JenksBreaks
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.math3.stat.descriptive.rank.Percentile

/**
 * @author Steve Ash
 */
AlignModel mOld = ReadWrite.readFromFile(AlignModel.class, new File("../resources/am_cmudict_22_xeps_ww_A.dat"))
AlignModel mNew = ReadWrite.readFromFile(AlignModel.class, new File("../resources/am_cmudict_22_xeps_ww_aa_A.dat"))

def graphs = Word.fromNormalString("ABOARD")
def phones = Word.fromSpaceSeparated("AH B AO R D")
def rOld = mOld.align(graphs, phones, 5)
def rNew = mNew.align(graphs, phones, 5)

rOld.take(1).each {println "OLD " + it.XAsPipeString + " (" + it.score +")"}
rNew.take(1).each {println "NEW " + it.XAsPipeString + " (" + it.score +")"}
println "Done"