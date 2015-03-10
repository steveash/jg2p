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

package com.github.steveash.jg2p.seqbin;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.PropertyList;

/**
 * Takes all of the feature vectors for each token in the sequence and squashes them all in to one feature vector
 * @author Steve Ash
 */
public class TokenSequence2FeatureVector extends Pipe {

  public TokenSequence2FeatureVector(Alphabet dataDict) {
    super(dataDict, null);
  }

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence ts = (TokenSequence) inst.getData();
    if (ts.isEmpty()) {
      // its going to be an empty feature vector; we can't use this convenience method normally because
      // it doesn't include any of the token features -- only the text as a feature
      inst.setData(ts.toFeatureVector(this.getDataAlphabet()));
      return inst;
    }

    Token first = ts.get(0);
    PropertyList result = first.getFeatures();
    PropertyList tail = result.last();
    for (int i = 1; i < ts.size(); i++) {
      tail = tail.append(ts.get(i).getFeatures());
    }
    inst.setData(new FeatureVector(this.getDataAlphabet(), result, true, true));
    return inst;
  }
}
