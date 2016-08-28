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

import com.github.steveash.jg2p.phoseq.Graphemes;
import com.github.steveash.jg2p.phoseq.Phonemes;

import java.util.List;

/**
 * @author Steve Ash
 */
public class PrefixPipe implements RerankFeature {

  private static final long serialVersionUID = 441008478061478926L;

  private void addPrefix(RerankFeatureBag data, String graphChar, List<String> phones) {
    String phoneSymbol = phones.get(0).substring(0, 1);
    if (Graphemes.isConsonant(phoneSymbol) && graphChar.equalsIgnoreCase(phoneSymbol)) {
      data.setBinary("leadCons+");
    } else {
      data.setBinary("leadCons-");
    }
  }

  @Override
  public void emitFeatures(RerankFeatureBag data) {
    String graphChar = data.getExample().getWordGraphs().get(0).substring(0, 1);
    if (Graphemes.isConsonant(graphChar) && Phonemes.isSimpleConsonantGraph(graphChar)) {
      addPrefix(data, graphChar, data.getExample().getEncoding().getPhones());
    }
  }
}
