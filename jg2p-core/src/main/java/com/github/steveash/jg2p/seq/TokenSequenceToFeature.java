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

package com.github.steveash.jg2p.seq;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * Takes the text of the current token sequence and adds the text itself as a binary feature.  This is similar to
 * running the TokenSequenceParseFeatureString pipe except that pip splits on spaces, which is bad for us when we
 * have a grapheme alignment that put two graphemes together: we use a space to delimit the adjacent graphemes
 * @author Steve Ash
 */
public class TokenSequenceToFeature extends Pipe {

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    for (int i = 0; i < ts.size(); i++) {
      Token token = ts.get(i);
      token.setFeatureValue(token.getText(), 1.0);
    }
    return carrier;
  }
}
