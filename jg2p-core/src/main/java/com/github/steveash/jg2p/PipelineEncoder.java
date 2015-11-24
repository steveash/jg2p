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

package com.github.steveash.jg2p;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.github.steveash.jg2p.rerank.RerankExample;
import com.github.steveash.jg2p.rerank.RerankableEncoder;
import com.github.steveash.jg2p.rerank.RerankableEntry;
import com.github.steveash.jg2p.rerank.RerankableResult;
import com.github.steveash.jg2p.rerank.RerankerResult;
import com.github.steveash.jg2p.util.DoubleTable;

import java.util.Collections;
import java.util.List;

import static com.github.steveash.jg2p.EncodingHolder.orderedResultsFrom;

/**
 * The encoder that uses the phoneticEncoder to produce a candidate list and then reranks it using the reranking
 * algorithm and delegating to the Rerank Model
 * @author Steve Ash
 */
public class PipelineEncoder implements Encoder {

  private final PipelineModel model;
  private final RerankableEncoder rerankEncoder;

  public PipelineEncoder(PipelineModel model) {
    this.model = model;
    this.rerankEncoder = model.getRerankEncoder();
  }

  @Override
  public List<PhoneticEncoder.Encoding> encode(Word input) {
    RerankableResult result = rerankEncoder.encode(input);
    DoubleTable graph = new DoubleTable(result.overallResultCount(), result.overallResultCount());
    for (int i = 0; i < result.overallResultCount(); i++) {

      RerankableEntry entryA = result.entryAtOverallIndex(i);
      for (int j = i + 1; j < result.overallResultCount(); j++) {
        RerankableEntry entryB = result.entryAtOverallIndex(j);
        RerankExample rre = new RerankExample();
        rre.setDupCountA(entryA.getDupPhonesCount());
        rre.setDupCountB(entryB.getDupPhonesCount());
        rre.setEncodingA(entryA.getEncoding());
        rre.setEncodingB(entryB.getEncoding());
        rre.setLanguageModelScoreA(entryA.getLangModelScore());
        rre.setLanguageModelScoreB(entryB.getLangModelScore());
        rre.setUniqueMatchingModeA(entryA.getHasMatchingUniqueModePhones());
        rre.setUniqueMatchingModeB(entryB.getHasMatchingUniqueModePhones());
        rre.setWordGraphs(input.getValue());
        RerankerResult abResult = model.getRerankerModel().probabilities(rre);
        graph.put(i, j, abResult.logOddsAOverB());
        graph.put(j, i, abResult.logOddsBOverA());
      }
    }
    List<EncodingHolder> holders = Lists.newArrayListWithCapacity(result.overallResultCount());
    for (int i = 0; i < result.overallResultCount(); i++) {
      holders.add(new EncodingHolder(result.encodingAtIndex(i), graph.sumOfRow(i)));
    }
    return orderedResultsFrom(holders);
  }
}
