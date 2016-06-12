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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.syllchain.ChainSyllabifierAdapter;
import com.github.steveash.jg2p.syllchain.SyllChainModel;
import com.github.steveash.jg2p.syllchain.Syllabifier;
import com.github.steveash.jg2p.util.ReadWrite;

/**
 * @author Steve Ash
 */
public class ModelFactory {

  public static PipelineEncoder createFromClasspath(String resourceName) {
    try {
      PipelineModel model = ReadWrite.readFromClasspath(PipelineModel.class, resourceName);
      model.makeSparse();
      return new PipelineEncoder(model);
    } catch (Exception e) {
      throw new ModelException("Problem loading the cmu default model from the classpath for " +
          resourceName, e);
    }
  }

  public static Syllabifier createSyllFromClasspath(String resourceName) {
    try {
      SyllChainModel model = ReadWrite.readFromClasspath(SyllChainModel.class, resourceName);
      model.getCrf().makeParametersHashSparse();
      return new ChainSyllabifierAdapter(model);
    } catch (Exception e) {
      throw new ModelException("Problem loading the syllabifier from " + resourceName, e);
    }
  }
}
