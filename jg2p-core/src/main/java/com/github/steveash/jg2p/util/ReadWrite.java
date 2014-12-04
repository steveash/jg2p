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

package com.github.steveash.jg2p.util;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Reads/writes single externalizeable files to disk
 * @author Steve Ash
 */
public class ReadWrite {

  public static <T> T readFromClasspath(Class<T> clazz, String resourceName) throws IOException, ClassNotFoundException {
    ByteSource bs = Resources.asByteSource(Resources.getResource(resourceName));
    return (T) readFromSource(bs);
  }

  public static <T> T readFromFile(Class<T> clazz, File inputFile) throws IOException, ClassNotFoundException {
    ByteSource source = Files.asByteSource(inputFile);
    return (T) readFromSource(source);
  }

  private static <T> T readFromSource(ByteSource bs) throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(bs.openBufferedStream())) {
      return (T) ois.readObject();
    }
  }

  public static <T> void writeTo(T model, File outputFile) throws IOException {
    ByteSink sink = Files.asByteSink(outputFile);
    try (ObjectOutputStream oos = new ObjectOutputStream(sink.openBufferedStream())) {
      oos.writeObject(model);
    }
  }

}
