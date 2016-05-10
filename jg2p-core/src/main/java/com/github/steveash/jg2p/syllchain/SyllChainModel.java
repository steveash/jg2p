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

package com.github.steveash.jg2p.syllchain;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.PathXTable;
import com.github.steveash.jg2p.align.ProbTable;
import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.kylm.model.immutable.ImmutableLM;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;

/**
 * @author Steve Ash
 */
public class SyllChainModel implements Serializable {

  private static final long serialVersionUID = -2809305117637940696L;
  private static final int MAX_SYLL_LEN = 6;

  private final ImmutableLM model;

  public SyllChainModel(ImmutableLM lm) {
    model = lm;
  }

  public List<Integer> syllBreaksForGrams(Word x) {
    PathXTable t = new PathXTable(x.unigramCount() + 1, 5);
    t.offer(0, t.make(0.0, -1, -1));
    // i is coords into the memoisation table
    for (int xx = 1; xx < x.unigramCount() + 1; xx++) {
      for (int i = 1; (i <= MAX_SYLL_LEN) && (xx - i >= 0); i++) {
        // returns perplexity so we need to negate it to recover log-prob
        double log2p = model.sentenceProb(makeChars(x, xx, i));
        t.extendPath(xx, xx - i, PathXTable.Entry.sample(log2p, i));
      }
    }
    return bestPath(x, t);
  }

  private List<String> makeChars(Word x, int xx, int i) {
    List<String> chars = Lists.newArrayListWithCapacity(i + 1);
    for (int j = xx - i; j < xx; j++) {
      chars.add(x.gramAt(j));
    }
    return chars;
  }

  private List<Integer> bestPath(Word x, PathXTable t) {
    List<Integer> bestSylls = null;
    double bestSyllScore = Double.NEGATIVE_INFINITY;
    Iterable<PathXTable.Entry> lasts = t.get(x.unigramCount());
    for (PathXTable.Entry lastEntry : lasts) {
      if (lastEntry.score < ProbTable.minLogProb) {
        continue;
      }
      List<Integer> thisSolution = decodePath(x, t, lastEntry);
      if (bestSylls != null && !isValid(x, thisSolution)) {
        continue;
      }
      if (lastEntry.score > bestSyllScore) {
        bestSylls = thisSolution;
        bestSyllScore = lastEntry.score;
      }
    }
    return checkNotNull(bestSylls);
  }

  private boolean isValid(Word x, List<Integer> thisSolution) {
    // make sure that each syllable would include a vowel
    if (thisSolution.size() <= 1) {
      return true; // can't do much here
    }
    boolean sawVowel = false;
    int nextSyll = 1;
    for (int i = 0; i < x.unigramCount(); i++) {
      boolean thisVowel = Graphemes.isVowel(x.gramAt(i));
      if (nextSyll < thisSolution.size()) {
        if (i == thisSolution.get(nextSyll)) {
          // we just crossed a syllable boundary
          if (!sawVowel) {
            return false;
          }
          sawVowel = false;
          nextSyll += 1;
        }

      }
      if (thisVowel) sawVowel = true;
    }
    // at the end we still need to verify that we saw a vowel in the last syllable
    return sawVowel;
  }

  private List<Integer> decodePath(Word x, PathXTable t, PathXTable.Entry entry) {
    ArrayList<Integer> syllIndexes = Lists.newArrayList();
    checkNotNull(entry, "couldnt recon path for " + x);
    int xx = x.unigramCount();
    while (xx > 0) {
      xx -= entry.xBackRef;
      syllIndexes.add(xx);
      entry = t.get(xx, entry.pathBackRef);
    }
    Collections.reverse(syllIndexes);
    return syllIndexes;
  }
}
