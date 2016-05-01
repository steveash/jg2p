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

import com.google.common.base.Preconditions;

import com.github.steveash.jg2p.Word;

import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

/**
 * @author Steve Ash
 */
public class AppendEndPipe extends Pipe {
  private static final long serialVersionUID = -2205334436343455510L;

  @Override
  public Instance pipe(Instance inst) {
    TokenSequence seq = (TokenSequence) inst.getData();
    seq.add("<END>");
    TokenSequence maybe = (TokenSequence) inst.getTarget();
    if (maybe != null) {
      maybe.add("<END>");
      Preconditions.checkState(maybe.size() == seq.size());
    }
    return inst;
  }
}
