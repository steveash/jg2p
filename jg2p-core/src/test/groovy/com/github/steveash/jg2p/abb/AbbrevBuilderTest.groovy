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

package com.github.steveash.jg2p.abb

/**
 * @author Steve Ash
 */
class AbbrevBuilderTest extends GroovyTestCase {

  void testGramBuilderDup() {
    def ab = new AbbrevBuilder(true)
    ab.append("EY EY")
    ab.append("EY EH ")
    ab.append("ER EH")
    ab.append("EH EH EH EY")
    assert "EY EY EH ER EH EH EH EY" == ab.build()
  }

  void testGramBuilderNoDup() {
    def ab = new AbbrevBuilder(false)
    ab.append("EY EY")
    ab.append("EY EH ")
    ab.append("ER EH")
    ab.append("EH EH EH EY")
    assert "EY EY EY EH ER EH EH EH EH EY" == ab.build()
  }
}
