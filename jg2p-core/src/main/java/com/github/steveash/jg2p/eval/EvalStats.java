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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.util.ListEditDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

/**
 * @author Steve Ash
 */
public class EvalStats {
  private static final int MAX_EXAMPLES = 50;
  private static final int MAX_EDITS_EXAMPLES = 5;

  // histogram of the count of pronunciations per word
  final Multiset<Integer> wordOptionsHisto = ConcurrentHashMultiset.create();
  final Multiset<Integer> resultsSizeHisto = ConcurrentHashMultiset.create();
  final ConcurrentMap<Integer,AtomicInteger> exampleCounter = Maps.newConcurrentMap();
  final ConcurrentMap<Integer,List<String>> examples = Maps.newConcurrentMap();
  final AtomicLong words = new AtomicLong(0);
  final AtomicLong zeroResultWords = new AtomicLong(0);
  final AtomicLong top1CorrectWords = new AtomicLong(0);

  final AtomicLong phones = new AtomicLong(0);
  final AtomicLong top1PhoneEdits = new AtomicLong(0);

  final Multiset<String> counters = ConcurrentHashMultiset.create();
  final LoadingCache<String,IrStats> irConfigSetup = CacheBuilder.newBuilder()
      .concurrencyLevel(32)
      .build(new CacheLoader<String, IrStats>() {
        @Override
        public IrStats load(String key) throws Exception {
          return new IrStats();
        }
      });

  {
    for (int i = 0; i <= MAX_EDITS_EXAMPLES; i++) {
      exampleCounter.put(i, new AtomicInteger(0));
      examples.put(i, new ArrayList<String>(MAX_EXAMPLES));
    }
  }

  long onNewResult(InputRecordGroup test, @Nullable PhoneticEncoder.Encoding topResult) {
    long newTotal = words.incrementAndGet();
    if (topResult == null) {
      zeroResultWords.incrementAndGet();
      return newTotal;
    }
    Word resultPhones = Word.fromGrams(topResult.phones);
    resultPhones.throwIfNotUnigram();

    int minEdits = Integer.MAX_VALUE;
    int minPhonesForEdits = Integer.MAX_VALUE;
    String rightPhones = "";
    for (Word good : test.getAcceptableYWords()) {
      if (resultPhones.equals(good)) {
        top1CorrectWords.getAndIncrement();
        phones.addAndGet(good.unigramCount());
        // no edits
        return newTotal; // found the best
      }
      int edits = ListEditDistance.editDistance(good.getValue(), resultPhones.getValue(), minEdits);
      if (edits >= 0) {
        Preconditions.checkArgument(edits != 0, "why wasnt this handled earlier?");
        if (edits < minEdits) {
          minEdits = edits;
          minPhonesForEdits = good.unigramCount();
          rightPhones = good.getAsSpaceString();
        }
      }
    }
    Preconditions.checkArgument(minEdits != Integer.MAX_VALUE);
    phones.addAndGet(minPhonesForEdits);
    top1PhoneEdits.addAndGet(minEdits);
    int editsForExamples = Math.min(MAX_EDITS_EXAMPLES, minEdits);
    int totalExamples = exampleCounter.get(editsForExamples).getAndIncrement();
    if (totalExamples < MAX_EXAMPLES) {
      List<String> exs = examples.get(editsForExamples);
      synchronized (exs) {
        exs.add(topResult.toString() + " expected " + rightPhones);
      }
    }
    return newTotal;
  }

  public double wordAccuracy() {
    long totalWords = this.words.get();
    if (totalWords == 0) return 0;

    return ((double) top1CorrectWords.get()) / totalWords;
  }

  public double wordErrorRate() {
    return 1.0 - wordAccuracy();
  }

  public double phoneErrorRate() {
    long totalPhones = this.phones.get();
    if (totalPhones == 0) return 0;

    return ((double) top1PhoneEdits.get()) / totalPhones;
  }

  public double phoneAccuracy() {
    return 1.0 - phoneErrorRate();
  }

}
