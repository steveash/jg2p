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

package com.github.steveash.jg2p.rerank;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.Word;

import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * The new re-ranker attempt that doesn't do the silly pairwise deal
 *
 * @author Steve Ash
 */
@CsvDataType
public class RerankExample {

  private static final Logger log = LoggerFactory.getLogger(RerankExample.class);

  @CsvField(pos = 1)
  private int sequence; // just used when reading/writing examples for training
  @CsvField(pos = 2)
  private boolean isRelevant; // set when training, otherwise not
  @CsvField(pos = 3)
  private PhoneticEncoder.Encoding encoding;
  @CsvField(pos = 4)
  private boolean uniqueMatchingMode;
  @CsvField(pos = 5)
  private int dupCount;
  @CsvField(pos = 6)
  private double languageModelScore;
  @CsvField(pos = 7)
  private List<String> wordGraphs;

  public int getSequence() {
    return sequence;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  public PhoneticEncoder.Encoding getEncoding() {
    return encoding;
  }

  public void setEncoding(PhoneticEncoder.Encoding encoding) {
    this.encoding = encoding;
  }

  public boolean isUniqueMatchingMode() {
    return uniqueMatchingMode;
  }

  public void setUniqueMatchingMode(boolean uniqueMatchingMode) {
    this.uniqueMatchingMode = uniqueMatchingMode;
  }

  public int getDupCount() {
    return dupCount;
  }

  public void setDupCount(int dupCount) {
    this.dupCount = dupCount;
  }

  public double getLanguageModelScore() {
    return languageModelScore;
  }

  public void setLanguageModelScore(double languageModelScore) {
    this.languageModelScore = languageModelScore;
  }

  public List<String> getWordGraphs() {
    return wordGraphs;
  }

  public void setWordGraphs(List<String> wordGraphs) {
    this.wordGraphs = wordGraphs;
  }

  public boolean isRelevant() {
    return isRelevant;
  }

  public void setRelevant(boolean relevant) {
    isRelevant = relevant;
  }

  public static List<RerankExample> makeExamples(RerankableResult rrResult, Word xWord,
                                                 @Nullable Set<List<String>> goodPhones) {
    return makeExamples(rrResult, xWord, goodPhones, 0);
  }

  public static List<RerankExample> makeExamples(
      RerankableResult rrResult, Word xWord,
      @Nullable Set<List<String>> goodPhones, int sequence) {

    List<RerankExample> outs = Lists.newArrayListWithCapacity(rrResult.overallResultCount());
    for (int i = 0; i < rrResult.overallResultCount(); i++) {
      RerankableEntry entry = rrResult.entryAtOverallIndex(i);

      if (entry.getEncoding().phones == null || entry.getEncoding().phones.isEmpty()) {
        log.warn("Got bad cand for " + xWord.getAsSpaceString());
        continue;
      }
      if (!Doubles.isFinite(entry.getLangModelScore())) {
        log.warn("Got bad lm score from " + entry.getEncoding().phones + " for " + xWord.getAsSpaceString());
        continue;
      }

      RerankExample rr = new RerankExample();
      rr.setDupCount(entry.getDupPhonesCount());
      rr.setEncoding(entry.getEncoding());
      rr.setLanguageModelScore(entry.getLangModelScore());
      rr.setUniqueMatchingMode(entry.getHasMatchingUniqueModePhones());
      rr.setWordGraphs(xWord.getValue());
      if (goodPhones != null) {
        rr.setRelevant(goodPhones.contains(entry.getEncoding().phones));
      }
      outs.add(rr);
    }
    return outs;
  }
}
