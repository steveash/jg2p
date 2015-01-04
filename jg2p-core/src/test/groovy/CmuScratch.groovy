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



import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.train.EncoderEval
import com.github.steveash.jg2p.util.ReadWrite

/**
 * Used to play with the failing examples to try and figure out some areas for improvement
 * @author Steve Ash
 */
def trainFile = "cmudict.5kA.txt"
def testFile = "cmudict.5kB.txt"
def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)
def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile)
def enc = ReadWrite.readFromClasspath(PhoneticEncoder.class, "encoder.dat")

def eval = new EncoderEval(enc, true)
eval.evalAndPrint(train, com.github.steveash.jg2p.train.EncoderEval.PrintOpts.ALL)
println "Examples"
eval.examples.asMap().entrySet().each { entry ->
  println " --- Examples at edit " + entry.key + " --- "
  entry.value.take(10).each {
    println "  " + it.left.xWord.asSpaceString + " -> " + it.right.first().phones.join(" ") + " expected " +
            it.left.yWord.asSpaceString + " align " + it.right.first().alignment.join("|")
  }
}
