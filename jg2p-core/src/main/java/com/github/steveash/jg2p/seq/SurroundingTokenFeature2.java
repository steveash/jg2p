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

package com.github.steveash.jg2p.seq;

import com.github.steveash.jg2p.util.TokenSeqUtil;

import org.apache.commons.lang3.StringUtils;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Puts the letters that surrounds the tokens as features (or if onlyShape then just the shape)
 * @author Steve Ash
 */
public class SurroundingTokenFeature2 extends Pipe {

  private final boolean onlyShape;
  private final String prefix;
  private final int beforeChars;
  private final int beforeOffset;
  private final int afterChars;

  public SurroundingTokenFeature2(boolean onlyShape, int beforeChars, int afterChars) {
    this.onlyShape = onlyShape;
    this.beforeChars = beforeChars;
    this.beforeOffset = beforeChars * -1;
    this.afterChars = afterChars;
    String d = onlyShape ? "$" : "%";
    this.prefix = d + beforeChars + d + afterChars + d;
  }

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    for (int i = 1; i < ts.size() - 1; i++) {
      Token t = ts.get(i);
      String before = TokenSeqUtil.getWindow(ts, i, beforeOffset, beforeChars);
      String after = TokenSeqUtil.getWindow(ts, i, 1, afterChars);

      if (isNotBlank(before) && isNotBlank(after)) {
        String f = prefix + xform(before) + "^" + xform(t.getText()) + "^" + xform(after);
        t.setFeatureValue(f, 1.0);
      }
    }
    return carrier;
  }

  private String xform(String data) {
    if (onlyShape) {
      return TokenSeqUtil.convertShape(data);
    }
    return data;
  }
}
