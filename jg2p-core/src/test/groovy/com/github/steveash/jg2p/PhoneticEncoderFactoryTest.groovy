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

package com.github.steveash.jg2p

import com.github.steveash.jg2p.seq.PhonemeCrfModel
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer
import com.google.common.collect.ImmutableList
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * @author Steve Ash
 */
class PhoneticEncoderFactoryTest {

  private PhoneticEncoder encoder

  @Ignore // not sure whats up with this
  @Test
  public void shouldSpotCheckAFew() throws Exception {
    this.encoder = PhoneticEncoderFactory.makeDefault()
    def results = encoder.encode("MIGHTY")
    results = results.sort(true) {it.tagProbability() }.reverse(true)
    results.each { println it.tagProbability() + " " + it.toString() }
  }
}
