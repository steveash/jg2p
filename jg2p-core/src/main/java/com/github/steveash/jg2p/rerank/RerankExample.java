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

package com.github.steveash.jg2p.rerank;

import com.github.steveash.jg2p.PhoneticEncoder;

import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

import java.util.List;

/**
 * one example to train the pipes on the reranker
 * @author Steve Ash
 */
@CsvDataType
public class RerankExample {

  public static final String A = "A";
  public static final String B = "B";

  @CsvField(pos = 1, required = true)
  private PhoneticEncoder.Encoding encodingA;
  @CsvField(pos = 2)
  private boolean uniqueMatchingModeA;
  @CsvField(pos = 3)
  private int dupCountA;
  @CsvField(pos = 4)
  private double languageModelScoreA;

  @CsvField(pos = 5, required = true)
  private PhoneticEncoder.Encoding encodingB;
  @CsvField(pos = 6)
  private boolean uniqueMatchingModeB;
  @CsvField(pos = 7)
  private int dupCountB;
  @CsvField(pos = 8)
  private double languageModelScoreB;

  @CsvField(pos = 9, required = true)
  private List<String> wordGraphs;
  @CsvField(pos = 10, required = true)
  private String label;

  public RerankExample() {
    // no arg constructor for the csv library
  }

  public RerankExample(PhoneticEncoder.Encoding encodingA, boolean uniqueMatchingModeA, int dupCountA,
                       PhoneticEncoder.Encoding encodingB, boolean uniqueMatchingModeB, int dupCountB,
                       List<String> wordGraphs, String label) {
    this.encodingA = encodingA;
    this.uniqueMatchingModeA = uniqueMatchingModeA;
    this.dupCountA = dupCountA;
    this.encodingB = encodingB;
    this.uniqueMatchingModeB = uniqueMatchingModeB;
    this.dupCountB = dupCountB;
    this.wordGraphs = wordGraphs;
    this.label = label;
  }

  public RerankExample flip() {
    RerankExample r = new RerankExample();
    r.setEncodingA(this.encodingB);
    r.setEncodingB(this.encodingA);
    r.setUniqueMatchingModeA(this.uniqueMatchingModeB);
    r.setUniqueMatchingModeB(this.uniqueMatchingModeA);
    r.setDupCountA(this.dupCountB);
    r.setDupCountB(this.dupCountA);
    r.setLanguageModelScoreA(this.languageModelScoreB);
    r.setLanguageModelScoreB(this.languageModelScoreA);
    r.setWordGraphs(this.wordGraphs);
    if (this.label.equalsIgnoreCase(A)) {
      r.setLabel(B);
    } else if (this.label.equalsIgnoreCase(B)) {
      r.setLabel(A);
    } else {
      throw new IllegalArgumentException("Unknown labels " + this.label);
    }
    return r;
  }

  public PhoneticEncoder.Encoding getEncodingA() {
    return encodingA;
  }

  public void setEncodingA(PhoneticEncoder.Encoding encodingA) {
    this.encodingA = encodingA;
  }

  public boolean isUniqueMatchingModeA() {
    return uniqueMatchingModeA;
  }

  public void setUniqueMatchingModeA(boolean uniqueMatchingModeA) {
    this.uniqueMatchingModeA = uniqueMatchingModeA;
  }

  public int getDupCountA() {
    return dupCountA;
  }

  public void setDupCountA(int dupCountA) {
    this.dupCountA = dupCountA;
  }

  public double getLanguageModelScoreA() {
    return languageModelScoreA;
  }

  public void setLanguageModelScoreA(double languageModelScoreA) {
    this.languageModelScoreA = languageModelScoreA;
  }

  public PhoneticEncoder.Encoding getEncodingB() {
    return encodingB;
  }

  public void setEncodingB(PhoneticEncoder.Encoding encodingB) {
    this.encodingB = encodingB;
  }

  public boolean isUniqueMatchingModeB() {
    return uniqueMatchingModeB;
  }

  public void setUniqueMatchingModeB(boolean uniqueMatchingModeB) {
    this.uniqueMatchingModeB = uniqueMatchingModeB;
  }

  public int getDupCountB() {
    return dupCountB;
  }

  public void setDupCountB(int dupCountB) {
    this.dupCountB = dupCountB;
  }

  public double getLanguageModelScoreB() {
    return languageModelScoreB;
  }

  public void setLanguageModelScoreB(double languageModelScoreB) {
    this.languageModelScoreB = languageModelScoreB;
  }

  public List<String> getWordGraphs() {
    return wordGraphs;
  }

  public void setWordGraphs(List<String> wordGraphs) {
    this.wordGraphs = wordGraphs;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return "RerankExample{" +
           "encodingA=" + encodingA +
           ", uniqueMatchingModeA=" + uniqueMatchingModeA +
           ", dupCountA=" + dupCountA +
           ", encodingB=" + encodingB +
           ", uniqueMatchingModeB=" + uniqueMatchingModeB +
           ", dupCountB=" + dupCountB +
           ", wordGraphs=" + wordGraphs +
           ", label='" + label + '\'' +
           '}';
  }
}
