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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.util.ListEditDistance;
import com.github.steveash.jg2p.util.Percent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Steve Ash
 */
public class EncoderEval {
  private static final Logger log = LoggerFactory.getLogger(EncoderEval.class);

  private final PhoneticEncoder encoder;
  private long totalPhones;
  private long totalRightPhones;
  private long totalWords;
  private long totalRightWords;
  private long noCodes;

  public EncoderEval(PhoneticEncoder encoder) {
    this.encoder = encoder;
  }

  public void evalAndPrint(List<InputRecord> inputs) {

    totalPhones = 0;
    totalRightPhones = 0;
    totalWords = 0;
    totalRightWords = 0;
    noCodes = 0;

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
      if (phonesDiff == 0) {
        totalRightWords += 1;
      }

      if (totalWords % 500 == 0) {
        log.info("Processed " + totalWords + " ...");
        if (totalWords % 10_000 == 0) {
          printStats();
        }
      }
    }
    printStats();
  }

  private void printStats() {
    log.info("No phones words that were skipped " + noCodes);
    log.info("Total words " + totalWords + ", total right " + totalRightWords + " - " + Percent
        .print(totalRightWords, totalWords));
    log.info("Total phones " + totalPhones + ", total right " + totalRightPhones + " - " +
             Percent.print(totalRightPhones, totalPhones));
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
}
