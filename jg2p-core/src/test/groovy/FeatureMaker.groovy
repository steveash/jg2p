import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.util.TokenSeqUtil

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

/**
 * @author Steve Ash
 */
//def cmuFile = "cmudict.0.7a"
def cmuFile = "cmudict.5kA.txt"
def training = InputReader.makeDefaultFormatReader().readFromClasspath(cmuFile)
new File("../resources/cmudict.5kA.features.shapes.txt").withPrintWriter { pw ->
  training.each { rec ->
    def w = rec.left.toString().replaceAll(/(\s+|,)/, "")
    def grams = rec.left.gramsSize(3).collect {
      def gram = it.replaceAll(/(\s+|,)/, "")
      return TokenSeqUtil.convertShape(gram)
    }
    pw.println("$w,${grams.join(" ")}")
  }
}