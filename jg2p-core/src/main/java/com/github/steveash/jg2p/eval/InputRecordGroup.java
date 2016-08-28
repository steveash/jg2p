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

package com.github.steveash.jg2p.eval;

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.Word;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an input group where there is an input and possibly multiple acceptable pronunciations
 *
 * @author Steve Ash
 */
public class InputRecordGroup {

  private final Word xWord;
  private final Set<Word> acceptableYWords;

  public InputRecordGroup(Word xWord, Set<Word> acceptableYWords) {
    this.xWord = checkNotNull(xWord);
    xWord.throwIfNotUnigram();

    this.acceptableYWords = acceptableYWords;
    for (Word word : acceptableYWords) {
      word.throwIfNotUnigram();
    }
  }

  public Word getTestWord() {
    return xWord;
  }

  public Set<Word> getAcceptableYWords() {
    return acceptableYWords;
  }

  public boolean isMatching(List<String> phones) {
    if (phones.isEmpty()) {
      return false;
    }
    Word word = Word.fromGrams(phones);
    word.throwIfNotUnigram();
    return acceptableYWords.contains(word);
  }
}
