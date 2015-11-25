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
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ash
 */
public class GraphoneSortingEncoder implements Encoder {

  private final PipelineModel model;
  private final PhoneticEncoder phoneticEncoder;

  public GraphoneSortingEncoder(PipelineModel model) {
    this.model = model;
    this.phoneticEncoder = model.getPhoneticEncoder();
  }

  @Override
  public List<PhoneticEncoder.Encoding> encode(Word input) {
    List<PhoneticEncoder.Encoding> results = phoneticEncoder.encode(input);
    ArrayList<EncodingHolder> holders = Lists.newArrayListWithCapacity(results.size());
    for (PhoneticEncoder.Encoding result : results) {
      double lmScore = model.getGraphoneModel().score(result);
      holders.add(new EncodingHolder(result, lmScore));
    }
    Collections.sort(holders, Ordering.natural()); // lm scores are neg to pos?
    return Lists.transform(holders, EncodingHolder.selectEncoding);
  }
}
