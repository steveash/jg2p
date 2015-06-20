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

package com.github.steveash.jg2p.util

import cc.mallet.types.Token
import org.junit.Test

import static com.github.steveash.jg2p.util.TokenSeqUtil.getWindow

/**
 * @author Steve Ash
 */
class TokenSeqUtilTest {

  @Test
  public void shouldMakeBackwardsWindow() throws Exception {
    def t = make("S T", "E", "V E")
    assert getWindow(t, 1, -1, 1) == "T"
    assert getWindow(t, 1, -2, 1) == "S"
    assert getWindow(t, 1, -2, 2) == "ST"
    assert getWindow(t, 2, -3, 3) == "STE"
    assert getWindow(t, 0, -3, 3) == null
    assert getWindow(t, 1, -3, 3) == null
    assert getWindow(t, 0, -1, 1) == null
  }

  @Test
  public void shouldMakeForwardsWindow() throws Exception {
    def t = make("S T", "E", "V E")
    assert getWindow(t, 1, 1, 1) == "V"
    assert getWindow(t, 1, 2, 1) == "E"
    assert getWindow(t, 0, 2, 2) == "VE"
    assert getWindow(t, 0, 3, 1) == "E"
    assert getWindow(t, 0, 3, 2) == null
    assert getWindow(t, 1, 2, 2) == null
    assert getWindow(t, 2, 1, 1) == null
    assert getWindow(t, 0, 1, 1) == "E"
    assert getWindow(t, 0, 1, 2) == "EV"
    assert getWindow(t, 0, 1, 3) == "EVE"
  }

  private List<Token> make(String... s) {
    return s.collect { return new Token(it)}
  }
}
