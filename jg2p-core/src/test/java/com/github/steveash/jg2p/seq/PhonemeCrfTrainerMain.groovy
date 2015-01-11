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

package com.github.steveash.jg2p.seq

import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * @author Steve Ash
 */
class PhonemeCrfTrainerMain {

  public static void main(String[] args) {
    def input = new SeqInputReader().
        readInput(Resources.asCharSource(Resources.getResource("cmua_2eps.align.txt"), Charsets.UTF_8))
    def trainer = PhonemeCrfTrainer.openAndTrain(input)
    trainer.writeModel()
  }
}