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

package com.github.steveash.jg2p.align;

import com.github.steveash.jg2p.Word;

import org.apache.commons.lang3.tuple.Pair;

/**
 * One training exemplar for the aligner
* @author Steve Ash
*/
public class InputRecord extends Pair<Word, Word> {

  public final Word xWord;
  public final Word yWord;

  public InputRecord(Word xWord, Word yWord) {
    this.xWord = xWord;
    this.yWord = yWord;
  }

  @Override
  public Word getLeft() {
    return xWord;
  }

  @Override
  public Word getRight() {
    return yWord;
  }

  @Override
  public Word setValue(Word value) {
    throw new IllegalStateException("Word pairs are immutable");
  }


}
