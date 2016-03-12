import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.AlignerTrainer
import com.github.steveash.jg2p.align.AlignerTrainerMainTest
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.align.XyWalker
import groovy.transform.CompileStatic

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

// construct a Word subclass that records the syllable boundaries
// build input records using that
// build a XyWalker that excludes any phones that would split a syllable boundary

class SWord extends Word {

  private List<Integer> boundaries = [] // records the indexes of the phones right after a syll break

  SWord(String sstring) {
    super(convertToPhones(sstring))
    // need to record the syllable boundaries
    def phones = sstring.split("\\|")
    int realIndex = 0
    for (int i = 0; i < phones.size(); i++) {
      if (phones[i].contains("-")) {
        boundaries << realIndex
      } else {
        realIndex += 1
      }
    }
  }

  static List<String> convertToPhones(String entry) {
    entry.split("\\|").findAll {!it.contains("-")}.toList()
  }

  @Override
  public String toString() {
    return "SWord-${boundaries}-${super.toString()}";
  }
}

def recs = new File("../resources/syllables.train.txt").readLines()
    .findAll {it.trim().size() > 0}
    .collect { line ->
  def fields = line.split("\t")
  if (fields[1].trim().contains(" ") || fields[1].trim().contains("-")) {
    return null
  } else {
    return new InputRecord(Word.fromNormalString(fields[1].trim()), new SWord(fields[2].trim()))
  }
}.findAll {it != null}

//recs.take(10).each {println it}

@CompileStatic
class SyllPreserving implements XyWalker {

  private final XyWalker delegate;

  @Override
  void forward(Word x, Word y, XyWalker.Visitor visitor) {

  }

  @Override
  void backward(Word x, Word y, XyWalker.Visitor visitor) {

  }
}

//def to = new TrainOptions()
//def at = new AlignerTrainer(to)