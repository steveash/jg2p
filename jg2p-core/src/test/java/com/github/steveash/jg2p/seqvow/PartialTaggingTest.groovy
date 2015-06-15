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

package com.github.steveash.jg2p.seqvow

import org.junit.Test

/**
 * @author Steve Ash
 */
class PartialTaggingTest {

  @Test
  public void shouldMakePartialForFinal() throws Exception {
    assert ["!D T", "!M !D", "K S"] ==
           PartialTagging.createFromGraphsAndFinalPhoneGrams(["A", "T", "X"], ["EY T", "IY EY", "K S"]).partialPhoneGrams

  }
}
