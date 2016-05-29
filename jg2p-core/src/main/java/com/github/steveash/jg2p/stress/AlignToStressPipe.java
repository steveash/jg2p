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

package com.github.steveash.jg2p.stress;

import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.syll.SWord;
import com.github.steveash.jg2p.syll.SyllStructure;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;

/**
 * @author Steve Ash
 */
public class AlignToStressPipe extends Pipe {

  private static final long serialVersionUID = -2547999621229455638L;
  private final Alphabet alpha;
  private final ImmutableList<StressFeature> features;

  public AlignToStressPipe(Alphabet alpha,
                           LabelAlphabet labelAlpha,
                           Iterable<StressFeature> features) {
    super(alpha, labelAlpha);
    this.alpha = alpha;
    this.features = ImmutableList.copyOf(features);
  }

  @Override
  public Instance pipe(Instance inst) {
    Alignment align = (Alignment) inst.getData();
    SyllStructure ss = new SyllStructure(align);
    AugmentableFeatureVector fv = new AugmentableFeatureVector(alpha, true);
    for (StressFeature feature : features) {
      feature.emit(fv, align, ss);
    }

    inst.setData(fv.toFeatureVector());
    return inst;
  }
}
