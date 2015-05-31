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

import com.github.steveash.jg2p.rerank.RerankExample;

import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;

/**
 * @author Steve Ash
 */
public class CsvFactory {

  public static CsvIOFactory make() {
    CsvConfiguration config = new CsvConfiguration();
    config.getSimpleTypeConverterProvider().registerConverterType(Double.class, DoubleConverter.class);
    config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
    return CsvIOFactory.createFactory(config, RerankExample.class);
  }

}
