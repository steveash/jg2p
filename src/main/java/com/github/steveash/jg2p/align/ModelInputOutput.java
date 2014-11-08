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

package com.github.steveash.jg2p.align;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Steve Ash
 */
public class ModelInputOutput {

  public static G2PModel readFromClasspath(String resourceName) throws IOException, ClassNotFoundException {
    ByteSource bs = Resources.asByteSource(Resources.getResource(resourceName));
    return readFromSource(bs);
  }

  public static G2PModel readFromFile(File inputFile) throws IOException, ClassNotFoundException {
    ByteSource source = Files.asByteSource(inputFile);
    return readFromSource(source);
  }

  private static G2PModel readFromSource(ByteSource bs) throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(bs.openBufferedStream())) {
      return (G2PModel) ois.readObject();
    }
  }

  public static void writeTo(G2PModel model, File outputFile) throws IOException {
    ByteSink sink = Files.asByteSink(outputFile);
    try (ObjectOutputStream oos = new ObjectOutputStream(sink.openBufferedStream())) {
      oos.writeObject(model);
    }
  }

}
