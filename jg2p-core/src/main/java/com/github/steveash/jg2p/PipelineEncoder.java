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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.rerank.RerankExample;
import com.github.steveash.jg2p.rerank.RerankableEncoder;
import com.github.steveash.jg2p.rerank.RerankableResult;
import com.github.steveash.jg2p.rerank.RerankerResult;

import java.util.List;

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
    List<RerankExample> rre = RerankExample.makeExamples(result, input, null);
    List<RerankerResult> reranked = model.getRerankerModel().probabilities(rre);
    return Lists.transform(reranked, RerankerResult.SelectEncoding);
  }
}
