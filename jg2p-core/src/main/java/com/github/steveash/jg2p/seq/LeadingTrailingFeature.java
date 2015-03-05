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
 * @author Steve Ash
 */
public class LeadingTrailingFeature extends Pipe {

  @Override
  public Instance pipe(Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    if (ts.size() >= 2) {
      Token first = ts.get(0);
      Token last = ts.get(ts.size() - 1);
      String firstShape = TokenSeqUtil.convertShape(StringUtils.left(first.getText(), 1));
      String lastShape = TokenSeqUtil.convertShape(StringUtils.right(last.getText(), 1));
      first.setFeatureValue("FIRST", 1.0);
      if (isNotBlank(firstShape)) {
        first.setFeatureValue("FIRST-" + firstShape, 1.0);
      }
      last.setFeatureValue("LAST", 1.0);
      if (isNotBlank(lastShape)) {
        last.setFeatureValue("LAST-" + lastShape, 1.0);
      }
    }
    return carrier;
  }
}
