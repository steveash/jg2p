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

package com.github.steveash.jg2p.model;

import com.github.steveash.jg2p.Encoder;
import com.github.steveash.jg2p.ModelFactory;
import com.github.steveash.jg2p.PipelineEncoder;
import com.github.steveash.jg2p.SimpleEncoder;

/**
 * Main entry point to create a CMU model
 * @author Steve Ash
 */
public class CmuEncoderFactory {

  public static Encoder create() {
    PipelineEncoder pipe = ModelFactory.createFromClasspath("pipeline_cmu_default.dat");
    return pipe;
  }

  public static SimpleEncoder createSimple() {
    return new SimpleEncoder(create());
  }
}