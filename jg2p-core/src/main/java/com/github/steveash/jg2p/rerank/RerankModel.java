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

import com.google.common.collect.Maps;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;

/**
 * @author Steve Ash
 */
public class RerankModel {

  private final Evaluator evaluator;

  public RerankModel(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  public static RerankModel from(File file) throws SAXException, JAXBException, IOException {
    InputStream is = new FileInputStream(file);
    PMML pmml;
    try {
      Source source = ImportFilter.apply(new InputSource(is));
      pmml = JAXBUtil.unmarshalPMML(source);
    } finally {
      is.close();
    }

    PMMLManager pmmlManager = new PMMLManager(pmml);
    Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(ModelEvaluatorFactory.getInstance());
    return new RerankModel(evaluator);
  }

  public String label(Map<String,Object> values) {
    Map<FieldName, FieldValue> inputs = Maps.newHashMap();
    for (FieldName field : evaluator.getActiveFields()) {
      Object value = values.get(field.getValue());
      if (value == null) {
        throw new IllegalArgumentException("cant find value for " + field.getValue());
      }

      FieldValue prepVal = evaluator.prepare(field, value);
      inputs.put(field, prepVal);
    }

    Map<FieldName, ?> result = evaluator.evaluate(inputs);
    Object label = result.get(evaluator.getTargetField());
    if (label instanceof Computable) {
      label = ((Computable) label).getResult();
    }
    return (String) label;
  }

}
