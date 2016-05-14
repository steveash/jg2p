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

package com.github.steveash.jg2p.syllchain;

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.align.Alignment;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Steve Ash
 */
public class SyllTagAlignerAdapter implements Aligner, Serializable {

  private static final long serialVersionUID = 5196389530110830700L;

  private final Aligner baseAligner;
  private final SyllChainModel syllTagger;

  public SyllTagAlignerAdapter(Aligner baseAligner, SyllChainModel syllTagger) {
    this.baseAligner = checkNotNull(baseAligner, "cant pass null aligner");
    this.syllTagger = checkNotNull(syllTagger, "cant pass null tagger");
  }

  @Override
  public List<Alignment> inferAlignments(Word x, int nBest) {
    Set<Integer> syllStarts = syllTagger.tagSyllStarts(x.getValue());
    List<Alignment> baseAligns = baseAligner.inferAlignments(x, nBest);
    List<Alignment> result = Lists.newArrayListWithCapacity(baseAligns.size());
    for (Alignment align : baseAligns) {
      result.add(syllTagger.enrichWithSyllStarts(align, syllStarts));
    }
    return result;
  }

  public SyllChainModel getSyllTagger() {
    return syllTagger;
  }
}
