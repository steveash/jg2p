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

import com.google.common.collect.Lists;

import com.github.steveash.jg2p.syllchain.Syllabifier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Steve Ash
 */
public class CmuSyllabifierFactoryTest {

  private static final Logger log = LoggerFactory.getLogger(CmuSyllabifierFactoryTest.class);

  @Test
  public void shouldCreate() throws Exception {
    Syllabifier syllabifier = CmuSyllabifierFactory.create();
    assertEquals(Lists.newArrayList("ste", "phen"), syllabifier.splitIntoSyllables("stephen"));
    assertEquals(Lists.newArrayList("STE", "PHEN"), syllabifier.splitIntoSyllables("STEPHEN"));
    assertEquals(Lists.newArrayList("car", "ner", "ie"), syllabifier.splitIntoSyllables("carnerie"));
    List<String> karoneous = syllabifier.splitIntoSyllables("karoneous");
    log.info("Karoneous got " + karoneous);

    int count = syllabifier.syllableCount("stephen");
    assertEquals(2, count);
  }

}