import com.github.steveash.jg2p.seq.PhonemeCrfInputOutput
import com.github.steveash.jg2p.seq.PhonemeCrfModel
import com.github.steveash.jg2p.train.EncoderEval
import com.github.steveash.jg2p.PhoneticEncoderFactory
import com.github.steveash.jg2p.align.InputReader

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


def cmuFile = "cmudict.0.7a"
def encoder = PhoneticEncoderFactory.makeFrom("cmu3.model.dat", "g2p_crf3.dat")
def training = InputReader.makeCmuReader().readFromClasspath(cmuFile)

new EncoderEval(encoder).evalAndPrint(training)
//PhonemeCrfModel phoneModel = PhonemeCrfInputOutput.readFromClasspath("g2p_crf.dat");
//def crf = phoneModel.crf
//new File("model.txt").withPrintWriter { pw ->
//
//  crf.print(pw)
//}
println "done!"