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

package com.github.steveash.jg2p.train;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.util.ListEditDistance;
import com.github.steveash.jg2p.util.Percent;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Multisets.copyHighestCountFirst;

/**
 * @author Steve Ash
 */
public class EncoderEval {
  private static final Logger log = LoggerFactory.getLogger(EncoderEval.class);
  private static final Joiner spaceJoin = Joiner.on(' ');
  private static final Joiner pipeJoin = Joiner.on('|');

  private static final int EXAMPLE_COUNT = 100;
  private static final int MAX_EXAMPLE_TO_PRINT = 15;

  public enum PrintOpts { ALL, SIMPLE }

  private final PhoneticEncoder encoder;
  private final boolean collectExamples;
  private long totalPhones;
  private long totalRightPhones;
  private long totalWords;
  private long totalRightWords;
  private long noCodes;
  private final Multiset<Integer> phoneEditHisto = HashMultiset.create();
  private final Multiset<Integer> rightAnswerInTop = HashMultiset.create();
  private final ListMultimap<Integer,Pair<InputRecord,List<PhoneticEncoder.Encoding>>> examples = ArrayListMultimap.create();
  private final Random rand = new Random(0xFEEDFEED);

  public EncoderEval(PhoneticEncoder encoder) {
    this(encoder, false);
  }

  public EncoderEval(PhoneticEncoder encoder, boolean collectExamples) {
    this.encoder = encoder;
    this.collectExamples = collectExamples;
  }

  public void evalAndPrint(List<InputRecord> inputs, PrintOpts opts) {

    totalPhones = 0;
    totalRightPhones = 0;
    totalWords = 0;
    totalRightWords = 0;
    noCodes = 0;
    rightAnswerInTop.clear();
    examples.clear();
    phoneEditHisto.clear();

    for (InputRecord input : inputs) {

      List<PhoneticEncoder.Encoding> encodings = encoder.encode(input.xWord);
      if (encodings.isEmpty()) {
        noCodes += 1;
        continue;
      }

      totalWords += 1;
      PhoneticEncoder.Encoding encoding = encodings.get(0);

      List<String> expected = input.yWord.getValue();
      int phonesDiff = ListEditDistance.editDistance(encoding.phones, expected, 100);
      totalPhones += expected.size();
      totalRightPhones += (expected.size() - phonesDiff);
      phoneEditHisto.add(phonesDiff);
      if (phonesDiff == 0) {
        totalRightWords += 1;
        rightAnswerInTop.add(0);
      }
      if (phonesDiff > 0) {
        // find out if the right encoding is in the top-k results
        for (int i = 1; i < encodings.size(); i++) {
          PhoneticEncoder.Encoding attempt = encodings.get(i);
          if (attempt.phones.equals(input.yWord.getValue())) {
            rightAnswerInTop.add(i);
            break;
          }
        }
      }
      if (collectExamples && phonesDiff > 0) {
        Pair<InputRecord, List<PhoneticEncoder.Encoding>> example = Pair.of(input, encodings);
        List<Pair<InputRecord,List<PhoneticEncoder.Encoding>>> examples = this.examples.get(phonesDiff);
        if (examples.size() < EXAMPLE_COUNT) {
          examples.add(example);
        } else {
          int victim = rand.nextInt(Ints.saturatedCast(totalWords));
          if (victim < EXAMPLE_COUNT) {
            examples.set(victim, example);
          }
        }
      }

      if (totalWords % 500 == 0 && opts != PrintOpts.SIMPLE) {
        log.info("Processed " + totalWords + " ...");
        if (totalWords % 10_000 == 0) {
          printStats(opts);
        }
      }
    }
    printStats(opts);
  }

  private void printStats(PrintOpts opts) {
    if (opts != PrintOpts.SIMPLE) {
      if (collectExamples) {
        printExamples();
      }
      log.info("Phone edit distance histo: ");
      int total = 0;
      for (Multiset.Entry<Integer> entry : phoneEditHisto.entrySet()) {
        total += entry.getCount();
        log.info("  " + entry.getElement() + " = " + entry.getCount() + " - " + Percent.print(total, totalWords));
      }
      log.info("No phones words that were skipped " + noCodes);
      log.info("Answer found in top-k answer?");
      total = 0;
      for (Multiset.Entry<Integer> entry : copyHighestCountFirst(rightAnswerInTop).entrySet()) {
        total += entry.getCount();
        log.info(
            "  In top " + entry.getElement() + " - " + entry.getCount() + " - " + Percent.print(total, totalWords));
      }
    }

    log.info("Total words " + totalWords + ", total right " + totalRightWords + " - " + Percent
        .print(totalRightWords, totalWords));
    log.info("Total phones " + totalPhones + ", total right " + totalRightPhones + " - " +
             Percent.print(totalRightPhones, totalPhones));

  }

  private void printExamples() {
    for (Integer phoneEdit : examples.keySet()) {
      log.info(" ---- Examples with edit distance " + phoneEdit + " ----");
      Iterable<Pair<InputRecord, List<PhoneticEncoder.Encoding>>> toPrint =
          limit(examples.get(phoneEdit), MAX_EXAMPLE_TO_PRINT);

      for (Pair<InputRecord, List<PhoneticEncoder.Encoding>> example : toPrint) {
        String got = "<null>";
        if (example.getRight().size() > 0) {
          got = example.getRight().get(0).toString();
        }
        log.info(" Got: " + got + " expected: " + example.getLeft().getRight().getAsSpaceString());
      }
    }
  }

  public long getTotalPhones() {
    return totalPhones;
  }

  public long getTotalRightPhones() {
    return totalRightPhones;
  }

  public long getTotalWords() {
    return totalWords;
  }

  public long getTotalRightWords() {
    return totalRightWords;
  }

  public long getNoCodes() {
    return noCodes;
  }

  public ListMultimap<Integer, Pair<InputRecord, List<PhoneticEncoder.Encoding>>> getExamples() {
    return examples;
  }

  public Multiset<Integer> getPhoneEditHisto() {
    return phoneEditHisto;
  }
}
