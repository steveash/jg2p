import com.github.steveash.jg2p.rerank.Rerank2Model
import com.github.steveash.jg2p.rerank.Rerank2Trainer
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
//def filePath = "../resources/psaur_rerank_train.txt"
def filePath = "/home/steve/Downloads/psaur_rerank_train_50k.txt"
def outPath = "../resources/dt_rerank2_4.dat"

new File(filePath).withReader { r ->
  def header = r.readLine().split("\t")
  int count = 0
  def indexes = []
  indexes << header.findIndexOf { it == "label" }
  indexes.addAll(header.findIndexValues { Rerank2Model.featureHeaders.contains(it) })
  println "Found ${indexes.size()} headers"
  assert indexes.size() == Rerank2Model.featureHeaders.size() + 1

  def inputs = Lists.newArrayListWithCapacity(1180000)
  String line
  while ((line = r.readLine()) != null) {

    def fields = line.split("\t")
    def inp = Maps.newHashMapWithExpectedSize(Rerank2Model.featureHeaders.size() + 1)
    assert header.length == fields.length

    indexes.each { Long ii ->
      def idx = ii as int
      String maybe = fields[idx]
      if (maybe == "0") {
        return
      }  // skip it
      inp.put(header[idx], maybe.intern())
    }

    count += 1
    if (count % 5000 == 0) {
      println "Parsed $count input records..."
      if (count == 10000) {
        println "Last was $inp"
      }
    }

    inputs << inp
  }
  println "Got ${inputs.size()} inputs to train on from many lines of input"

  def trainer = new Rerank2Trainer()
  def model = trainer.trainFor(inputs)
  ReadWrite.writeTo(model, new File(outPath))
  println "done"
}
