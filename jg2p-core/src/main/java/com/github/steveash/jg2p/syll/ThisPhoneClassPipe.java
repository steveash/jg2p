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

package com.github.steveash.jg2p.syll;

import com.github.steveash.jg2p.phoseq.Phonemes;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * @author Steve Ash
 */
public class ThisPhoneClassPipe extends Pipe {

  private static final long serialVersionUID = -9203630918687349251L;

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence seq = (TokenSequence) inst.getData();
    for (int i = 0; i < seq.size(); i++) {
      Token token = seq.get(i);
      String classForPhone = Phonemes.getClassForPhone(token.getText());
      token.setFeatureValue("THISPHCLS_" + classForPhone, 1.0);
    }
    return inst;
  }
}
