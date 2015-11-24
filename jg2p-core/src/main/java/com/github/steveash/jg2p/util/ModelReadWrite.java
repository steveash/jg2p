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

package com.github.steveash.jg2p.util;

import com.google.common.base.Throwables;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PipelineModel;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.rerank.Rerank2Model;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Helper that pulls models out of files given as parameters; unfortunately things have evolved without good
 * direction on IO stuff so this encapsulates the various files that would've existed at different times
 *
 * @author Steve Ash
 */
public class ModelReadWrite {

  public static AlignModel readTrainAlignerFrom(String file) {
    Object model = read(file);
    if (model instanceof PhoneticEncoder) {
      return checkNotNull(((PhoneticEncoder) model).getAlignModel());
    } else if (model instanceof AlignModel) {
      return (AlignModel) model;
    } else if (model instanceof PipelineModel) {
      return checkNotNull(((PipelineModel) model).getTrainingAlignerModel());
    }
    throw badModel(file, model);
  }

  public static AlignTagModel readTestAlignerFrom(String file) {
    Object model = read(file);
    if (model instanceof AlignTagModel) {
      return (AlignTagModel) model;
    }
    if (model instanceof PhoneticEncoder) {
      Aligner aligner = ((PhoneticEncoder) model).getAligner();
      if (aligner instanceof AlignTagModel) {
        return (AlignTagModel) aligner;
      }
      throw new IllegalArgumentException("Cant get an AlignTagModel from a phonetic encoder using aligner " + aligner);
    }
    if (model instanceof PipelineModel) {
      return checkNotNull(((PipelineModel) model).getTestingAlignerModel());
    }
    throw badModel(file, model);
  }

  public static Rerank2Model readRerankerFrom(String file) {
    Object model = read(file);
    if (model instanceof Rerank2Model) {
      return (Rerank2Model) model;
    }
    if (model instanceof PipelineModel) {
      return checkNotNull(((PipelineModel) model).getRerankerModel());
    }
    throw badModel(file, model);
  }

  protected static Object read(String file) {
    if (isBlank(file)) {
      throw new IllegalArgumentException("Can't read a model from " + file);
    }
    try {
      return ReadWrite.readFromFile(Object.class, new File(file));
    } catch (IOException | ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  public static PhonemeCrfModel readPronouncerFrom(String file) {
    Object model = read(file);
    if (model instanceof PhonemeCrfModel) {
      return ((PhonemeCrfModel) model);
    }
    if (model instanceof PhoneticEncoder) {
      return checkNotNull(((PhoneticEncoder) model).getPhoneTagger());
    }
    if (model instanceof PipelineModel) {
      return checkNotNull(((PipelineModel) model).getPronouncerModel(),
                          "cant use null pronouncer model from system model");
    }
    throw badModel(file, model);
  }

  public static LangModel readGraphoneFrom(String file) {
    Object model = read(file);
    if (model instanceof LangModel) {
      return (LangModel) model;
    }
    if (model instanceof PipelineModel) {
      return checkNotNull(((PipelineModel) model).getGraphoneModel(), "cant use null graphone from system model");
    }
    throw badModel(file, model);
  }

  protected static IllegalArgumentException badModel(String file, Object model) {
    return new IllegalArgumentException("Dont know how to get the right model out of " + file + " from type " + model);
  }
}
