import com.github.steveash.jg2p.rerank.Rerank2Model
import com.github.steveash.jg2p.rerank.Rerank2Trainer
import com.github.steveash.jg2p.rerank.RerankExample
import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.collect.Lists
import com.google.common.collect.Maps

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

/**
 * @author Steve Ash
 */
def filePath = "../resources/psaur_rerank_train.txt"
//def filePath = "/home/steve/Downloads/psaur_rerank_train_50k.txt"
def outPath = "../resources/dt_rerank_F7_retag_1.dat"

def exs = []
new File(filePath).withReader { r ->
  def deser = CsvFactory.make().createDeserializer()
  def count = 0;
  deser.open(r)
  while (deser.hasNext()) {
    RerankExample ex = deser.next()
    if (ex.encodingA.phones == null || ex.encodingB.phones == null || ex.encodingA.phones.isEmpty() || ex.encodingB.phones.isEmpty()) {
      println "Problem with example on line $count got $ex skipping..."
    } else {
      exs.add(ex)
    }

    count += 1
    if (count % 5000 == 0) {
      println "Parsed $count input records..."
    }

  }
  println "Got ${exs.size()} inputs to train on from many lines of input"

  def trainer = new Rerank2Trainer()
  def model = trainer.trainFor(exs)
  ReadWrite.writeTo(model, new File(outPath))
  println "done"
}
