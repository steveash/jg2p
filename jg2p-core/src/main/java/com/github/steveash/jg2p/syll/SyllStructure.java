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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Grams;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.phoseq.Graphemes;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.cycle;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.right;

/**
 * Type that describes a lot of substructure for a word
 * @author Steve Ash
 */
public class SyllStructure {


  private final List<String> syllText; // one entry per syllable with the text of that syll (no spaces) |Sylls|
  private final List<String> syllCodes; // one matching entry with onc codes (no spaces) |Sylls|
  private final List<String> oncGrams; // one space separate onc coding per graphone |Graphones|
  private final List<Integer> graphoneIndexToSyllableIndex; // one per graphone with syll index |Graphones|
  private final List<Integer> graphemeIndexToSyllableIndex; // one per grapheme with syll index |Graphemes|
  private final List<Integer> graphemeIndexToSyllableSequence; // sequences the graphemes in each syllable as 0, 1 |Graphemes|
  private final List<Boolean> graphoneIndexContainsVowel; // one per graphone with true if it contains nucleus vowel |Graphones|
  private final int syllCount;
  private final int graphemeCount;

  public SyllStructure(Alignment align) {
    this(align.getAllXTokensAsList(),
         checkNotNull(align.getGraphoneSyllableGrams()),
         checkNotNull(align.getGraphemeSyllStarts()));
  }

  public SyllStructure(List<String> textGraphones, List<String> syllGraphones, Set<Integer> graphemeSyllStarts) {
    Preconditions.checkArgument(textGraphones.size() == syllGraphones.size(), "mismatched arg lists");
    this.syllCount = graphemeSyllStarts.size();
    syllText = Lists.newArrayListWithCapacity(syllCount);
    syllCodes = Lists.newArrayListWithCapacity(syllCount);
    oncGrams = ImmutableList.copyOf(syllGraphones);
    graphoneIndexToSyllableIndex = Lists.newArrayListWithCapacity(textGraphones.size());
    graphoneIndexContainsVowel = Lists.newArrayList(from(cycle(false)).limit(textGraphones.size()));
    graphemeIndexToSyllableIndex = Lists.newArrayListWithExpectedSize(textGraphones.size());
    graphemeIndexToSyllableSequence = Lists.newArrayListWithExpectedSize(textGraphones.size());

    StringBuilder tb = new StringBuilder();
    StringBuilder cb = new StringBuilder();
    int xx = 0;
    int syllIndex = -1;
    int syllSeq = 0;
    for (int i = 0; i < textGraphones.size(); i++) {
      String textGram = textGraphones.get(i);
      String syllGram = syllGraphones.get(i);
      Preconditions.checkState(textGram.length() == syllGram.length(), "bad gram in", textGraphones, syllGraphones);
      for (int j = 0; j < textGram.length(); j++) {
        char textChar = textGram.charAt(j);
        char syllCode = syllGram.charAt(j);
        if (Character.isWhitespace(textChar) || Character.isWhitespace(syllCode)) {
          Preconditions.checkState(textChar == syllCode, "mismatched whitespace");
          continue;
        }
        if (Graphemes.isVowel(String.valueOf(textChar)) && syllCode == SyllTagTrainer.NucleusChar) {
          graphoneIndexContainsVowel.set(i, true);
        }
        if (graphemeSyllStarts.contains(xx)) {
          breakSylls(syllText, tb, syllCodes, cb);
          syllIndex += 1;
          syllSeq = 0;
        }
        safeSet(graphemeIndexToSyllableIndex, xx, syllIndex);
        safeSet(graphemeIndexToSyllableSequence, xx, syllSeq);
        safeSet(graphoneIndexToSyllableIndex, i, syllIndex); // since syll indexes are always increasing this works
        tb.append(textChar);
        cb.append(syllCode);
        xx += 1;
        syllSeq += 1;
      }
    }
    breakSylls(syllText, tb, syllCodes, cb);
    this.graphemeCount = xx;
    Preconditions.checkState(syllText.size() == syllCount);
    Preconditions.checkState(syllText.size() == syllCodes.size());
    Preconditions.checkState(graphoneIndexToSyllableIndex.size() == textGraphones.size());
  }

  private static void safeSet(List<Integer> list, int index, int value) {
    if (index < list.size()) {
      list.set(index, value);
      return;
    }
    if (index == list.size()) {
      list.add(value);
      return;
    }
    throw new IllegalStateException("Cannot set past the end " + list + " index " + index);
  }

  public int getGraphemeCount() {
    return graphemeCount;
  }

  public int getSyllIndexForGraphoneGramIndex(int graphoneGramIndex) {
    return graphoneIndexToSyllableIndex.get(graphoneGramIndex);
  }

  public int getSyllSequenceForGraphemeIndex(int graphemeIndex) {
    return graphemeIndexToSyllableSequence.get(graphemeIndex);
  }

  public String getSyllPart(int syllIndex) {
    return getSyllPart(syllIndex, -1, -1, -1);
  }

  public String getOncCodeAtGraphoneAndSequence(int graphoneIndex, int sequenceInGraphone) {
    String oncGram = oncGrams.get(graphoneIndex);
    // these are space separated so...
    return Iterables.get(Grams.iterateSymbols(oncGram), sequenceInGraphone);
  }

  public int getSyllCount() {
    return syllCount;
  }

  public int getLastSyllIndex() {
    return syllCount - 1;
  }

  public String oncGramForGraphoneIndex(int graphoneGramIndex) {
    return oncGrams.get(graphoneGramIndex);
  }

  public List<String> getOncGrams() {
    return oncGrams;
  }

  public int getSyllIndexForGraphemeIndex(int graphemeIndex) {
    return graphemeIndexToSyllableIndex.get(graphemeIndex);
  }

  public boolean graphoneGramIndexContainsNucleus(int graphoneGramIndex) {
    return graphoneIndexContainsVowel.get(graphoneGramIndex);
  }

  public String getSyllGraphsForSyllIndex(int syllIndex) {
    return syllText.get(syllIndex);
  }

  public String getSyllPart(int syllIndex, int maxOnset, int maxNucleus, int maxCoda) {
    String text = syllText.get(syllIndex);
    String codes = syllCodes.get(syllIndex);
    String onset = "";
    String nucli = "";
    String coda = "";
    for (int i = 0; i < codes.length(); i++) {
      char code = codes.charAt(i);
      char txt = text.charAt(i);
      if (code == SyllTagTrainer.OnsetChar) {
        onset += String.valueOf(txt);
      } else if (code == SyllTagTrainer.NucleusChar) {
        nucli += String.valueOf(txt);
      } else if (code == SyllTagTrainer.CodaChar) {
        coda += String.valueOf(txt);
      } else {
        throw new IllegalStateException("unknown code " + code);
      }
    }
    if (maxOnset < 0) maxOnset = onset.length();
    if (maxNucleus < 0) maxNucleus = nucli.length();
    if (maxCoda < 0) maxCoda = coda.length();
    return right(onset, maxOnset).toLowerCase() +
           left(nucli, maxNucleus).toUpperCase() +
           left(coda, maxCoda).toLowerCase();
  }

  private static void breakSylls(List<String> syllText, StringBuilder tb, List<String> syllCodes, StringBuilder cb) {
    if (tb.length() > 0) {
      syllText.add(tb.toString());
      Preconditions.checkState(tb.length() == cb.length());
      syllCodes.add(cb.toString());
      tb.delete(0, tb.length());
      cb.delete(0, cb.length());
    }
  }

  @Override
  public String toString() {
    return "SyllStructure{" +
           "syllText=" + syllText +
           ", syllCodes=" + syllCodes +
           ", graphoneIndexToSyllableIndex=" + graphoneIndexToSyllableIndex +
           ", syllCount=" + syllCount +
           '}';
  }
}
