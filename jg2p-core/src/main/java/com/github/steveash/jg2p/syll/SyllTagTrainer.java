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

package com.github.steveash.jg2p.syll;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.phoseq.Graphemes;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Trains a CRF to predict synonym structure from aligned.  We have two kinds of structure we are
 * modelling here -- the sonority of the syllable as S = {Onset, Nucleus, Coda} and the boundaries
 * for where graphemes should be split to map into phoneme substrings as T = {B, X} (b is begin, x
 * is continuation -- like the BIO scheme)
 * Note that the Y side is not necessarily individual phonemes -- but the phoneme substrings
 * that the aligner identified.
 *
 * Thus we are learning the labels S x T and we are going to disallow illegal transitions to enforce
 * the semantics of each
 * @author Steve Ash
 */
public class SyllTagTrainer {

  private static final Logger log = LoggerFactory.getLogger(SyllTagTrainer.class);

  // syllable only roles
  public static final String Onset = "O";
  public static final String Nucleus = "N";
  public static final String Coda = "C";

  // these mark the _start_ of a phoneme alignemnt
  public static final String OnsetStart = "OB";
  public static final String NucleusStart = "NB";
  public static final String CodaStart = "CB";

  // these mark the _continuation of the phoneme alignment (i.e. until the next _start_)
  public static final String OnsetCont = "OX";
  public static final String NucleusCont = "NX";
  public static final String CodaCont = "CX";

  public static List<String> makeSyllMarksFor(Alignment align, SWord phoneSyll) {
    List<Boolean> starts = align.getXStartMarks();
    List<String> sylls = Lists.newArrayListWithCapacity(align.getWordUnigrams().size());
    int syllState = 0; // 0 onset, 1 nucleus, 2 coda
    int x = 0;
    int y = 0;

    for (Pair<List<String>, List<String>> graphone : align.getGraphonesSplit()) {
      // deal with phonemes, if leading is syllable boundary then great, also assert no breaks

      for (int i = 0; i < graphone.getRight().size(); i++) {
        if (graphone.getRight().get(i).equals(Grams.EPSILON)) continue;

        if (i == 0) {
          if (phoneSyll.isStartOfSyllable(y)) {
            syllState = 0; // back to onset
          }
        } else {
          if (phoneSyll.isStartOfSyllable(y)) {
//            if (log.isDebugEnabled()) {
//              log.debug("Malformed alignment, cant start syllable in middle " + phoneSyll +
//                        " with graphones " + align.toString() + " x = " + x + ", y = " + y);
//            }
            syllState = 0;
          }
        }
        y += 1;
      }
      // we've updated our syll state so just emit symbols now
      for (int i = 0; i < graphone.getLeft().size(); i++) {
        String graph = graphone.getLeft().get(i);
        // skip epsilons
        if (graph.equals(Grams.EPSILON)) continue;

        boolean isVowel = Graphemes.isVowel(graph);
        if (syllState == 0) {
          if (isVowel) syllState = 1; // this is the nucleus
        } else if (syllState == 1) {
          if (!isVowel) syllState = 2; // this is the coda
        } else {
          Preconditions.checkState(syllState == 2, "should always be coda");
//          if (isVowel && !knownIssue(graph, align.getWordUnigrams(), x)) {
//            if (log.isDebugEnabled()) {
//              log.debug("Malformed syllable, vowels in coda " + phoneSyll +
//                        " with graphones " + align.toString() + " x = " + x + ", y = " + y);
//            }
//          }
        }
        String coded = getCodeFor(syllState, starts.get(x));
        sylls.add(coded);
        x += 1;
      }
    }
    Preconditions.checkState(x == align.getWordUnigrams().size(), "ended up with diff graphone graph count ", align, phoneSyll);
    Preconditions.checkState(y == phoneSyll.unigramCount(), "ended up with different phone count ", align, phoneSyll);
    return sylls;
  }

  private static CharMatcher trailingVowel = CharMatcher.anyOf("eEyY");
  private static boolean knownIssue(String vowel, List<String> graphs, int x) {
    if (vowel.equals("e") || vowel.equals("E")) return true; // just skip all the fricken Es

    if (x == (graphs.size() - 1) && trailingVowel.matches(vowel.charAt(0))) {
      return true; // this is the trailing silent vowel
    }
    // cons + vow + cons trailing then
    if (x == (graphs.size() - 2) && Graphemes.isConsonant(graphs.get(x + 1))) {
      return true;
    }
    return false;
  }

  private static String getCodeFor(int state, boolean isStart) {
    if (state == 0) {
      if (isStart) return OnsetStart;
      else return OnsetCont;
    } else if (state == 1) {
      if (isStart) return NucleusStart;
      else return NucleusCont;
    } else {
      Preconditions.checkState(state == 2, "invalid state");
      if (isStart) return CodaStart;
      else return CodaCont;
    }
  }
}
