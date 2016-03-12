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

package com.github.steveash.jg2p.align

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.util.ReadWrite
import org.junit.Test

/**
 * @author Steve Ash
 */
class AlignerTrainerMainTest {


  public static final String outFile = "target/output.model.dat"

  @Test
  void testMain() {
    String[] args = [
        "--infile", "target/test-classes/sample.txt",
        "--outfile", this.outFile
    ]

    AlignerTrainer.main(args)


    def outFile = new File(outFile)
    outFile.deleteOnExit()
    def model = ReadWrite.readFromFile(AlignModel, outFile)
    printExample(model, "fresh", "F R EH SH")
    printExample(model, "wrinkling", "R IH NG K L IH NG")
  }

  private printExample(AlignModel v, String left, String right) {
    def results = v.align(Word.fromNormalString(left), Word.fromSpaceSeparated(right), 3)
    println "$left to $right got ${results.size()}"
    results.each { println it }
  }
}
