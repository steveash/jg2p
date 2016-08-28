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

package com.github.steveash.jg2p.wfst;

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Encoder;
import com.github.steveash.jg2p.EncodingResult;
import com.github.steveash.jg2p.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ash
 */
public class SeqTransducerPhoneticEncoderAdapter implements Encoder {

  private final int topk;
  private final SeqTransducer transducer;

  public SeqTransducerPhoneticEncoderAdapter(int topk, SeqTransducer transducer) {
    this.topk = topk;
    this.transducer = transducer;
  }

  @Override
  public List<EncodingResult> encode(Word input) {
    List<WordResult> translate = transducer.translate(input, topk);
    ArrayList<EncodingResult> results = Lists.newArrayListWithCapacity(translate.size());
    int rank = 0;
    for (WordResult result : translate) {
      results.add(new SeqTransducerPhoneticEncoderResult(result.getWord().getValue(), rank++));
    }
    return results;
  }

  private static class SeqTransducerPhoneticEncoderResult implements EncodingResult {

    private final List<String> phones;
    private int rank;

    private SeqTransducerPhoneticEncoderResult(List<String> phones, int rank) {
      this.phones = phones;
      this.rank = rank;
    }

    @Override
    public List<String> getPhones() {
      return phones;
    }

    @Override
    public int getRank() {
      return rank;
    }

    @Override
    public void setRank(int rank) {
      this.rank = rank;
    }
  }
}
