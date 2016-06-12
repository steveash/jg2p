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

import com.github.steveash.jg2p.SimpleEncoder;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Steve Ash
 */
public class CmuEncoderFactoryTest {

  private static final Logger log = LoggerFactory.getLogger(CmuEncoderFactoryTest.class);

  @Test
  public void shouldLoadEncoder() throws Exception {
    SimpleEncoder encoder = CmuEncoderFactory.createSimple();
    String stephen = encoder.encodeBestAsSpaceString("stephen");
    assertEquals("S T IY V AH N", stephen);

    List<String> pumps = encoder.encodeAsSpaceString("pumpernickel", 5);
    for (String pump : pumps) {
      log.info("Pump : " + pump);
    }

  }
}