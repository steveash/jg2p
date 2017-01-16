/*
 * Copyright 2017 Steve Ash
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

package com.github.steveash.jg2p.syllchain;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ash
 */
public class RuleSyllabifierTest {

  @Test
  public void shouldMatchPerlTestCases() throws Exception {
    ImmutableMap<String, Integer> tests = ImmutableMap.<String, Integer>builder()
        .put("hoopty        ", 2)
        .put("bah           ", 1)
        .put("I             ", 1)
        .put("A             ", 1)
        .put("organism      ", 4)
        .put("organisms     ", 4)
        .put("antagonisms   ", 5)
        .put("schisms       ", 2)
        .put("monisms       ", 3)
        .put("puritanisms   ", 5)
        .put("criticisms    ", 4)
        .put("microorganisms", 6)
        .put("surrealisms   ", 4)
        .put("isms          ", 2)
        .put("aphorisms     ", 4)
        .put("prisms        ", 2)
        .put("anachronisms  ", 5)
        .put("dualisms      ", 4)
        .put("euphemisms    ", 4)
        .put("mechanisms    ", 4)
        .put("mannerisms    ", 4)
        .put("yogiisms      ", 4)
        .put("metabolisms   ", 5)
        .put("baptisms      ", 3)
        .put("embolisms     ", 4)
        .put("methodisms    ", 4)
        .put("executed      ", 4)
        .put("accused       ", 2)
        .put("dosed         ", 1)
        .build();
    for (Map.Entry<String, Integer> entry : tests.entrySet()) {
      String word = entry.getKey().trim();
      assertEquals(word + " bad", (int)entry.getValue(), RuleSyllabifier.syllable(word));
    }
  }

  @Test
  public void shouldCheckSomeWords() throws Exception {
    assertEquals(2, RuleSyllabifier.syllable("special"));
  }
}