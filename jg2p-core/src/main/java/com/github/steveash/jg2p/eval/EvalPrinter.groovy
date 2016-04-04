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

package com.github.steveash.jg2p.eval

import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.SimpleWriter
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Steve Ash
 */
class EvalPrinter {

  private static final Logger log = LoggerFactory.getLogger(EvalPrinter.class);

  public static void writeExamples(File output, EvalStats stats) {
    output.withPrintWriter() { pw ->
      stats.examples.keySet().sort().each { edits ->
        def exs = stats.examples.get(edits)
        exs.each {
          pw.println(edits + "\t" + it.toString())
        }
      }
    }
  }

  public static void printTo(SimpleWriter pw, EvalStats stats, String label) {
    def totalWords = stats.words.get()
    pw.println(StringUtils.center(" " + label + " ", 80, '*'));
    // histo of word pronunciations
    pw.println("* Histogram of how many pronunciations per word");
    stats.wordOptionsHisto.entrySet().sort { it.element }.each {
      pw.println(" * Words with ${it.element} variants = " + it.count + "  " + Percent.print(it.count, totalWords))
    }
    pw.println(StringUtils.repeat('*', 20))
    stats.resultsSizeHisto.entrySet().sort{it.element}.each {
      pw.println(" * Queries with ${it.element} results returned = " + it.count + "  " + Percent.print(it.count, totalWords))
    }
    pw.println(StringUtils.repeat('*', 20))
    pw.println("* Counters")
    stats.counters.entrySet().sort {it.element}.each {
      pw.println(" * " + it.element + " = " + it.count)
    }
    pw.println(StringUtils.repeat('*', 20))
    pw.println("IR metrics for various top-k configurations")
    stats.irConfigSetup.asMap().entrySet().sort {it.key}.each {
      pw.println(String.format(" * " + it.key + " = Prec  %.3f (Max %.3f), Recall  %.3f (Max %.3f)",
                               it.value.precision(), it.value.precisionMax(),
                               it.value.recall(), it.value.recallMax()))
    }
    pw.println(StringUtils.repeat('*', 20))
    // final stats at the bottom
    pw.println(String.format("* Word  Accuracy: %.4f   (WER %.4f)", stats.wordAccuracy(), stats.wordErrorRate()));
    pw.println(String.format("* Phone Accuracy: %.4f   (PER %.4f)", stats.phoneAccuracy(), stats.phoneErrorRate()));
    pw.println(String.format(" * Word top 1 matched %d of %d", stats.top1CorrectWords.get(), totalWords));
    pw.println(String.format(" * Words the produced zero results %d", stats.zeroResultWords.get()))
  }
}
