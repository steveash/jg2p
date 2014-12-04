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

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.util.ReadWrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Steve Ash
 */
public class AlignmentExampleMain {

  private static final Logger log = LoggerFactory.getLogger(AlignmentExampleMain.class);

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    AlignModel model = ReadWrite.readFromClasspath(AlignModel.class, "cmu2eps.model.dat");
    Word x = Word.fromNormalString("AMAZING");
    Word y = Word.fromSpaceSeparated("AH M EY Z IH NG");
    List<Alignment> results = model.align(x, y, 5);
    log.info("Got {} results", results.size());
    for (Alignment result : results) {
      log.info(" " + result.toString() + " g: " + result.getGraphones());
    }
  }

}
