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

/**
 * Samples the cmu file and converts it in to the "samples" format
 * @author Steve Ash
 */
def cmuFile = "cmudict.0.7a"
def training = InputReader.makeCmuReader().readFromClasspath(cmuFile)
def out = new File("../resources/cmudict.2kB.txt")
Collections.shuffle(training)
out.withPrintWriter { pw ->
  training.take(2000).each {
    pw.println(it.left.asSpaceString + "\t" + it.right.asSpaceString)
  }
}
println "Done!"