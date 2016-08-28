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

package com.github.steveash.jg2p.rerank

import com.github.steveash.jg2p.PhoneticEncoder.Encoding
import com.github.steveash.jg2p.PhoneticEncoder.Result
import com.github.steveash.jg2p.lm.LangModel
import com.google.common.collect.HashMultiset

import javax.annotation.Nullable

/**
 * Container for phonetic encoder result that calculates all of the info that we need for reranking (at train and
 * test time)
 * @author Steve Ash
 */
class RerankableResult {

  private final Result encoderResult;
  private final Map<Encoding, Double> encodingToLangModelScore;
  private final HashMultiset phoneSequenceCount;
  private final List<String> modePhones;
  private final boolean hasUniqueMode;
  final boolean isValid;

  RerankableResult(Result encoderResult, LangModel langModel) {
    this.encoderResult = encoderResult;
    this.encodingToLangModelScore = new IdentityHashMap<>(encoderResult.overallResults.size())

    // calculate the lang model score for all of the entries
    encoderResult.overallResults.each {
      encodingToLangModelScore.put(it, langModel.score(it))
    }

    def overall = encoderResult.overallResults
    if (overall.size() <= 0) {
      this.isValid = false
      return
    }
    this.phoneSequenceCount = HashMultiset.create()
    overall.each { phoneSequenceCount.add(it.phones) }
    def modeEntry = phoneSequenceCount.entrySet().max { it.count }
    assert modeEntry != null
    int candidatesSameAsMode = phoneSequenceCount.entrySet().count { it.count == modeEntry.count }
    this.modePhones = modeEntry.element
    this.hasUniqueMode = (candidatesSameAsMode == 1)
    this.isValid = true
  }

  Result encoderResult() {
    return this.encoderResult
  }

  int overallResultCount() {
    return encoderResult.overallResults.size()
  }

  Encoding encodingAtIndex(int index) {
    return encoderResult.overallResults.get(index)
  }

  @Nullable
  RerankableEntry firstEntryFor(List<String> phones) {
    def matchingIndex = encoderResult.overallResults.findIndexOf { phones == it.phones }
    if (matchingIndex < 0) {
      return null;
    }
    return entryAtOverallIndex(matchingIndex)
  }

  RerankableEntry entryAtOverallIndex(int matchingIndex) {
    def matching = encoderResult.overallResults[matchingIndex]
    assert matching != null
    def bestDupCount = phoneSequenceCount.count(matching.phones)
    def bestUniqueMode = hasUniqueMode && matching.phones == modePhones
    def bestLm = encodingToLangModelScore.get(matching)
    assert bestLm != null // how is there an encoding now that we didn't have before?
    return new RerankableEntry(matching, bestUniqueMode, bestDupCount, bestLm, matchingIndex)
  }

  @Override
  public String toString() {
    return "RerankableResult{" +
           "encoderResult=" + encoderResult +
           ", encodingToLangModelScore=" + encodingToLangModelScore +
           ", phoneSequenceCount=" + phoneSequenceCount +
           ", modePhones=" + modePhones +
           ", hasUniqueMode=" + hasUniqueMode +
           ", isValid=" + isValid +
           '}';
  }
}
