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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.rerank.Rerank2Model;
import com.github.steveash.jg2p.rerank.RerankableEncoder;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Overall model that has each of the pieces
 * @author Steve Ash
 */
public class PipelineModel implements Serializable {

  private static final long serialVersionUID = 863270402760625113L;

  private AlignModel trainingAlignerModel = null;
  private AlignTagModel testingAlignerModel = null;
  private PhonemeCrfModel pronouncerModel = null;
  private LangModel graphoneModel = null;
  private Rerank2Model rerankerModel = null;

  public AlignModel getTrainingAlignerModel() {
    return trainingAlignerModel;
  }

  public void setTrainingAlignerModel(AlignModel trainingAlignerModel) {
    this.trainingAlignerModel = trainingAlignerModel;
  }

  public AlignTagModel getTestingAlignerModel() {
    return testingAlignerModel;
  }

  public void setTestingAlignerModel(AlignTagModel testingAlignerModel) {
    this.testingAlignerModel = testingAlignerModel;
  }

  public PhonemeCrfModel getPronouncerModel() {
    return pronouncerModel;
  }

  public void setPronouncerModel(PhonemeCrfModel pronouncerModel) {
    this.pronouncerModel = pronouncerModel;
  }

  public LangModel getGraphoneModel() {
    return graphoneModel;
  }

  public void setGraphoneModel(LangModel graphoneModel) {
    this.graphoneModel = graphoneModel;
  }

  public Rerank2Model getRerankerModel() {
    return rerankerModel;
  }

  public void setRerankerModel(Rerank2Model rerankerModel) {
    this.rerankerModel = rerankerModel;
  }

  public PhoneticEncoder getPhoneticEncoder() {
    return new PhoneticEncoder(testingAlignerModel, pronouncerModel, 5, Double.NEGATIVE_INFINITY,
                               Double.NEGATIVE_INFINITY);
  }

  public RerankableEncoder getRerankEncoder() {
    return new RerankableEncoder(getPhoneticEncoder(), checkNotNull(getGraphoneModel(), "must have a graphone mode"));
  }

  // then create an evaluator that uses gpars as a class
  // create a script that orchestrates that and prints stuff
}
