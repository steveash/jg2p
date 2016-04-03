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

import com.google.common.base.Throwables;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.rerank.Rerank3Model;
import com.github.steveash.jg2p.rerank.RerankableEncoder;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Overall model that has each of the pieces
 *
 * @author Steve Ash
 */
public class PipelineModel implements Serializable {

  private static final Logger log = LoggerFactory.getLogger(PipelineModel.class);

  private static final long serialVersionUID = 863270402760625113L;

  private AlignModel trainingAlignerModel = null;
  private Aligner testingAlignerModel = null;
  private PhonemeCrfModel pronouncerModel = null;
  private LangModel graphoneModel = null;
  private Rerank3Model rerankerModel = null;

  public AlignModel getTrainingAlignerModel() {
    return trainingAlignerModel;
  }

  public void setTrainingAlignerModel(AlignModel trainingAlignerModel) {
    this.trainingAlignerModel = trainingAlignerModel;
  }

  public Aligner getTestingAlignerModel() {
    return testingAlignerModel;
  }

  public void setTestingAlignerModel(Aligner testingAlignerModel) {
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

  public Rerank3Model getRerankerModel() {
    return rerankerModel;
  }

  public void setRerankerModel(Rerank3Model rerankerModel) {
    this.rerankerModel = rerankerModel;
  }

  public PhoneticEncoder getPhoneticEncoder() {
    return new PhoneticEncoder(testingAlignerModel, pronouncerModel, 5, Double.NEGATIVE_INFINITY,
                               Double.NEGATIVE_INFINITY);
  }

  public RerankableEncoder getRerankEncoder() {
    return new RerankableEncoder(getPhoneticEncoder(), checkNotNull(getGraphoneModel(), "must have a graphone mode"));
  }

  private Object writeReplace() {
    return new PipelineModelProxy(this);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException {
    log.info("Reading the old version...");
    // the old models might still use this entry point until they are re-written using the proxy, so for backwards
    PipelineModel model = null;
    try {
      this.graphoneModel = (LangModel) stream.readObject();
      this.pronouncerModel = (PhonemeCrfModel) stream.readObject();
      this.rerankerModel = (Rerank3Model) stream.readObject();
      this.testingAlignerModel = (AlignTagModel) stream.readObject();
      this.trainingAlignerModel = (AlignModel) stream.readObject();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
