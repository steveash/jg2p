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
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.syll.SyllTagTrainer;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import kylmshade.a.a.A;

/**
 * For each vowel in the sequence we're going to emit a sonority pattern that captures the sonority structure around
 * "me" using O-nset, N-ucleus, and C-oda (but collapsing multiples) and putting an X for where i am
 *
 * @author Steve Ash
 */
public class SonorityFeature2 extends Pipe {

  private static final long serialVersionUID = 6589789948528882754L;

  private final boolean before;

  public SonorityFeature2(boolean before) {
    this.before = before;
  }

  @Override
  public Instance pipe(Instance inst) {

    TokenSequence ts = (TokenSequence) inst.getData();

    List<String> sylls = Lists.transform(ts, NeighborSyllableFeature.TokenToSyllGram);
    Preconditions.checkState(sylls.size() == ts.size());

    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      String syll = sylls.get(i);
      String text = t.getText();
      Preconditions.checkState(text.length() == syll.length(), "grams doesnt match syll grams");
      // we only emit when we hit a gram that has a vowel nucleus
      int vowelInGram = findEligibleVowel(text, syll);
      if (vowelInGram < 0) {
        continue;
      }
      StringBuilder sb = new StringBuilder();
      if (before) {
        emitBefore(sb, ts, sylls, i);
      }
      emitMe(sb, text, syll);
      if (!before) {
        emitAfter(sb, ts, sylls, i);
      }
      if (before) {
        String s = trimBefore(sb.toString());
        if (shouldEmit(s)) {
          t.setFeatureValue("SONOB_" + s, 1.0);
        }
      } else {
        String s = trimAfter(sb.toString());
        if (shouldEmit(s)) {
          t.setFeatureValue("SONOA_" + s, 1.0);
        }
      }

    }
    return inst;
  }

  private boolean shouldEmit(String s) {
    return s.charAt(0) != '$' || s.charAt(s.length() - 1) != '$';
  }

  private String trimAfter(String s) {
    int first = s.indexOf('$');
    int second = s.indexOf('$', first + 1);
    boolean inNext = false;
    for (int i = second + 1; i < s.length(); i++) {
      char code = s.charAt(i);
      if (code == SyllTagTrainer.OnsetChar) {
        if (inNext) {
          return s.substring(0, i - 1);
        }
        inNext = true;
      } else if (code == SyllTagTrainer.NucleusChar) {
        inNext = true;
      } else if (code == SyllTagTrainer.CodaChar) {
        if (inNext) {
          return s.substring(0, i);
        }
      } else {
        inNext = true;
      }
    }
    return s;
  }

  private String trimBefore(String s) {
    int start = s.indexOf('$');
    boolean inPrev = false;
    for (int i = start - 1; i >= 0; i--) {
      char code = s.charAt(i);
      if (code == SyllTagTrainer.OnsetChar) {
        if (inPrev) {
          return s.substring(i); // this would be the begin of the previous syll
        }
      } else if (code == SyllTagTrainer.NucleusChar) {
        inPrev = true;
      } else if (code == SyllTagTrainer.CodaChar) {
        if (inPrev) {
          return s.substring(i + 1);
        }
        inPrev = true;
      } else {
        // this is a nucleus
        inPrev = true;
      }
    }
    return s;
  }

  private void emitMe(StringBuilder sb, String text, String gram) {
    boolean inNucleus = false;
    for (int j = 0; j < gram.length(); j++) {
      char code = gram.charAt(j);
      if (Character.isWhitespace(code)) {
        continue;
      }

      char c = text.charAt(j);
      if (code == SyllTagTrainer.NucleusChar && Graphemes.isVowel(String.valueOf(c))) {
        if (!inNucleus) {
          append(sb, '$', false);
          inNucleus = true;
        }
        append(sb, c, true);
      } else {
        if (inNucleus) {
          append(sb, '$', false);
          inNucleus = false;
        }
        append(sb, code, true);
      }
    }
    if (inNucleus) {
      append(sb, '$', false);
    }
  }

  private void append(StringBuilder sb, char code, boolean suppressDups) {
    if (suppressDups) {
      int last = sb.length() - 1;
      if (last >= 0) {
        if (sb.charAt(last) == code) {
          // this is a dup
          return;
        }
      }
    }
    sb.append(code);
  }

  private void emitBefore(StringBuilder sb, TokenSequence ts, List<String> sylls, int beforeMe) {
    for (int i = 0; i < sylls.size() && i < beforeMe; i++) {
      String gram = sylls.get(i);
      String text = ts.get(i).getText();
      appendCodes(sb, text, gram);
    }
  }

  private void emitAfter(StringBuilder sb, TokenSequence ts, List<String> sylls, int afterMe) {
    for (int i = afterMe + 1; i < sylls.size(); i++) {
      String gram = sylls.get(i);
      String text = ts.get(i).getText();
      appendCodes(sb, text, gram);
    }
  }

  private void appendCodes(StringBuilder sb, String text, String gram) {
    for (int j = 0; j < gram.length(); j++) {
      char code = gram.charAt(j);
      if (Character.isWhitespace(code)) {
        continue;
      }

      char c = text.charAt(j);
      if (code == SyllTagTrainer.NucleusChar && Graphemes.isVowel(String.valueOf(c))) {
        append(sb, c, true);
      } else {
        append(sb, code, true);
      }
    }
  }

  private int findEligibleVowel(String text, String syllCodes) {
    for (int i = 0; i < text.length(); i++) {
      char textChar = text.charAt(i);
      char syllChar = syllCodes.charAt(i);
      if (Character.isWhitespace(textChar) || Character.isWhitespace(syllChar)) {
        Preconditions.checkState(textChar == syllChar, "mismatched whitespace in gram");
        continue;
      }
      if (Graphemes.isVowel(String.valueOf(textChar)) && syllChar == SyllTagTrainer.NucleusChar) {
        return i;
      }
    }
    return -1;
  }
}
