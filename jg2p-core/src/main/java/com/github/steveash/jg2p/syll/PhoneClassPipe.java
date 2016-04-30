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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.phoseq.Phonemes;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.TokenWindow;

import java.util.ArrayList;
import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * adds feature for how many spots after me is the next vowel adds feature for how many spots before me is the prev
 * vowel
 *
 * @author Steve Ash
 */
public class PhoneClassPipe extends PhoneNeighborPipe {
  private static final long serialVersionUID = -1379086431132835321L;

  public PhoneClassPipe(boolean includeCurrent, List<TokenWindow> windows) {
    super(includeCurrent, windows);
  }

  @Override
  protected List<String> xformInputSequence(TokenSequence ts) {
    List<String> phones = super.xformInputSequence(ts);
    ArrayList<String> classes = Lists.newArrayListWithCapacity(phones.size());
    for (int i = 0; i < phones.size(); i++) {
      classes.add(Phonemes.getClassForPhone(phones.get(i)));
    }
    return classes;
  }

  @Override
  protected String prefix() {
    return "PHCLS_";
  }
}
