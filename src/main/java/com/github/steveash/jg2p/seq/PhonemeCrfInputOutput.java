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

package com.github.steveash.jg2p.seq;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cc.mallet.fst.CRF;

/**
 * @author Steve Ash
 */
public class PhonemeCrfInputOutput {

  public static void writeModelToFile(PhonemeCrfModel model, File output) throws IOException {
    ByteSink sink = Files.asByteSink(output);
    try (ObjectOutputStream oos = new ObjectOutputStream(sink.openBufferedStream())) {
      oos.writeObject(model.getCrf());
    }
  }

  public static PhonemeCrfModel readFromClasspath(String resourceName) throws IOException, ClassNotFoundException {
    ByteSource source = Resources.asByteSource(Resources.getResource(resourceName));
    try (ObjectInputStream ois = new ObjectInputStream(source.openBufferedStream())) {
      CRF crf = (CRF) ois.readObject();
      return new PhonemeCrfModel(crf);
    }
  }
}
