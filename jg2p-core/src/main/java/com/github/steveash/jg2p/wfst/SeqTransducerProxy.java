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

import com.github.steveash.jopenfst.FstInputOutput;
import com.github.steveash.jopenfst.ImmutableFst;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

/**
 * serialization proxy for a g2p transducer
 * @author Steve Ash
 */
class SeqTransducerProxy implements Externalizable {

  private static final long serialVersionUID = 3325458446685260859L;

  private ImmutableFst fst;
  private int order;

  public SeqTransducerProxy() {
  }

  public SeqTransducerProxy(SeqTransducer transducer) {
    this.fst = transducer.getFst();
    this.order = transducer.getOrder();
  }

  private Object readResolve() throws ObjectStreamException {
    return new SeqTransducer(this.fst, this.order);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(order);
    FstInputOutput.writeFstToBinaryStream(this.fst, out);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.order = in.readInt();
    this.fst = new ImmutableFst(FstInputOutput.readFstFromBinaryStream(in));
  }
}
