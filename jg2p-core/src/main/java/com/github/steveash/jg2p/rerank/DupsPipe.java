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

package com.github.steveash.jg2p.rerank;

import com.google.common.math.DoubleMath;

import com.github.steveash.jg2p.util.Scaler;

import org.apache.commons.math3.util.MathUtils;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

/**
 * @author Steve Ash
 */
public class DupsPipe extends Pipe {
  private static final long serialVersionUID = 309406321461606325L;

  private static final double DUP_SCALE_BASE = 3.0;

  public DupsPipe(Alphabet dataDict, Alphabet targetDict) {
    super(dataDict, targetDict);
  }

  @Override
  public Instance pipe(Instance inst) {
    RerankFeature data = (RerankFeature) inst.getData();
    data.setFeature("A_dupCount", Scaler.scaleLog(data.getExample().getDupCountA(), DUP_SCALE_BASE));
    data.setFeature("B_dupCount", Scaler.scaleLog(data.getExample().getDupCountB(), DUP_SCALE_BASE));
    return inst;
  }
}
