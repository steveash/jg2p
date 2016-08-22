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

package com.github.steveash.jg2p.wfst;

import com.google.common.collect.ImmutableSet;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.io.Convert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ash
 */
public class EntryFstMakerTest {

  @Test
  public void shouldCovertNoClusters() throws Exception {
    EntryFstMaker maker = new EntryFstMaker(ImmutableSet.of("A", "B", "C", "D", "E"));
    MutableFst fst1 = maker.inputToFst(Word.fromNormalString("STEVE"), new MutableSymbolTable());
//    Convert.export(fst1, "steve");
    for (int i = 0; i < fst1.getStateCount(); i++) {
      if (i == fst1.getStateCount() - 1) {
        assertEquals(0, fst1.getState(i).getArcCount());
      } else {
        assertEquals(1, fst1.getState(i).getArcCount());
      }
    }
  }

  @Test
  public void shouldConvertWithClusters() throws Exception {
    EntryFstMaker maker = new EntryFstMaker(ImmutableSet.of("S", "T", "E", "V", "E", "S|T", "T|E", "S|T|E", "V|E"));
    MutableFst fst1 = maker.inputToFst(Word.fromNormalString("STEVE"), new MutableSymbolTable());
//        Convert.export(fst1, "steve");
    assertEquals(8, fst1.getStateCount());
  }
}