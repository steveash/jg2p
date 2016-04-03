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

package com.github.steveash.jg2p;

import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.Aligner;
import com.github.steveash.jg2p.lm.LangModel;
import com.github.steveash.jg2p.rerank.Rerank3Model;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.sun.nio.sctp.InvalidStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

/**
 * @author Steve Ash
 */
public class PipelineModelProxy implements Externalizable {

  private static final Logger log = LoggerFactory.getLogger(PipelineModelProxy.class);

  private static final long serialVersionUID = 5722034722126958295L;
  private static final int VERSION = 42;

  private PipelineModel model;

  public PipelineModelProxy(PipelineModel model) {
    this.model = model;
  }

  public PipelineModelProxy() {
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    log.info("Writing the proxy out...");
    ObjectOutputStream os = (ObjectOutputStream) out;
    os.writeInt(VERSION); // version / magic number starting
    // in version 42 we write out each object independently and flush after each so that if worse came to worse
    // it would be easy to slice and dice the binary file
    writeAndFlush(os, model.getTrainingAlignerModel());
    writeAndFlush(os, model.getTestingAlignerModel());
    writeAndFlush(os, model.getPronouncerModel());
    writeAndFlush(os, model.getGraphoneModel());
    writeAndFlush(os, model.getRerankerModel());
  }

  protected void writeAndFlush(ObjectOutputStream out, Object obj) throws IOException {
    out.writeObject(obj);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    log.info("Reading the proxy in....");
    int version = in.readInt();
    if (version != 42) {
      throw new InvalidStreamException("Dont know how to decode version " + version);
    }
    this.model = new PipelineModel();
    this.model.setTrainingAlignerModel((AlignModel) in.readObject());
    this.model.setTestingAlignerModel((Aligner) in.readObject());
    this.model.setPronouncerModel((PhonemeCrfModel) in.readObject());
    this.model.setGraphoneModel((LangModel) in.readObject());
    this.model.setRerankerModel((Rerank3Model) in.readObject());
  }

  private Object readResolve() throws ObjectStreamException {
    return model;
  }
}
