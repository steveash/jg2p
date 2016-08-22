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

import com.github.steveash.jopenfst.io.Convert;

import org.junit.Test;

import java.io.File;

/**
 * @author Steve Ash
 */
public class LangModelToFstTest {

  @Test
  public void shouldConvertAndWrite() throws Exception {
    SeqTransducer transducer = new LangModelToFst()
        .fromArpa(new File("/home/steve/Documents/phonetisaurus-0.7.8/script/g_0_a/data.arpa"));
    Convert.export(transducer.getFst(), "testfst");
  }
}