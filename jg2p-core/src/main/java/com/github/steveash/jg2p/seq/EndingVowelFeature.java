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

package com.github.steveash.jg2p.seq;

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.syll.SyllStructure;
import com.github.steveash.jg2p.syll.SyllTagTrainer;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * If the last char is E or Y then it goes through and tags the vowels leading up to that with
 *
 * @author Steve Ash
 */
public class EndingVowelFeature extends Pipe {

  @Override
  public Instance pipe(Instance inst) {

    TokenSequence ts = (TokenSequence) inst.getData();
    String lastToken = ts.get(ts.size() - 1).getText();
    char lastChar = lastToken.charAt(lastToken.length() - 1);
    if (lastChar != 'y' && lastChar != 'e') {
      return inst;
    }

    SyllStructure struct = (SyllStructure) ts.getProperty(PhonemeCrfTrainer.PROP_STRUCTURE);
    checkNotNull(struct, "no sylls", inst);
    int lastSyllIndex = struct.getLastSyllIndex();
    for (int i = 0; i < ts.size(); i++) {
      String tag = null;
      Token t = ts.get(i);
      String s = struct.oncGramForGraphoneIndex(i);
      int thisSyllIndex = struct.getSyllIndexForGraphoneGramIndex(i);
      String text = t.getText();
      Preconditions.checkState(text.length() == s.length(), "grams doesnt match syll grams");
      for (int j = 0; j < text.length(); j++) {
        char textChar = text.charAt(j);
        char syllChar = s.charAt(j);
        if (Character.isWhitespace(textChar) || Character.isWhitespace(syllChar)) {
          Preconditions.checkState(textChar == syllChar, "mismatched whitespace in gram");
          continue;
        }
        if (Graphemes.isVowel(String.valueOf(textChar)) && syllChar == SyllTagTrainer.NucleusChar) {
          // we care about nucleus vowels because those are the ones influence by trailing letters
          if (tag == null) {
            tag = "TE_VOWEL_" + textChar + "_" + (thisSyllIndex < lastSyllIndex ? "BEFORE" : "END");
          }
        }
      }
      if (tag != null) {
        t.setFeatureValue(tag, 1.0);
      }
    }
    return inst;
  }
}
