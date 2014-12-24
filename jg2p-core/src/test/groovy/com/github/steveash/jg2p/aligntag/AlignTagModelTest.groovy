/*
 * Copyright 2014 Steve Ash
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

package com.github.steveash.jg2p.aligntag

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.util.ReadWrite
import org.junit.Before
import org.junit.Test

/**
 * @author Steve Ash
 */
class AlignTagModelTest {

  AlignTagModel model

  @Before
  public void setUp() throws Exception {
    model = ReadWrite.readFromClasspath(AlignTagModel, "aligntag.dat")
  }

  @Test
  public void shouldAlign() throws Exception {
    def aligns = model.inferAlignments(Word.fromNormalString("steve"), 2)
    aligns.each { println it }
  }
}
