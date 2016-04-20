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

package com.github.steveash.jg2p.phoseq;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Steve Ash
 */
public class Phonemes {

  // M = monothong
    // D = dipthong
    // R = rcolor
    // --- Consonants ---
    // S = stops
    // A = affricates
    // F = fricatives
    // N = nasal
    // L = liquids
    // I = semivowels
  public enum PhoneClass {
    M, D, R, S, A, F, N, L, I;

    public String code() {
      return name();
    }
  }

  public static boolean isVowel(String phone) {
    PhoneClass pc = getClassSymbolForPhone(phone);
    switch (pc) {
      case M:
      case D:
      case R:
        return true;
      default:
        return false;
    }
  }

  public static boolean isConsonant(String phone) {
    return !isVowel(phone);
  }

  public static boolean isSimpleConsonantGraph(String graph) {
    return simpleConsonantGraphs.contains(graph.toUpperCase());
  }

  public static final Predicate<String> whereOnlyVowels = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return isVowel(input);
    }
  };

  public static String getClassForPhone(String phone) {
    return checkNotNull(phoneToPhoneClass.get(phone.toUpperCase()), "invalid phone", phone);
  }

  public static PhoneClass getClassSymbolForPhone(String phone) {
    return checkNotNull(codeToEnum.get(getClassForPhone(phone)), "cant map to symbol", phone);
  }

  public static ImmutableSet<String> getPhonesForClass(String phoneClass) {
    return checkNotNull(phoneClassToPhone.get(phoneClass), "invalid phone class", phoneClass);
  }

  // key is phone, value is mono, dipthong, r-color, stop, affricate, fricative, nasal, liquid, semivowels
  // --- VOWELS ---
  // M = monothong
  // D = dipthong
  // R = rcolor
  // --- Consonants ---
  // S = stops
  // A = affricates
  // F = fricatives
  // N = nasal
  // L = liquids
  // I = semivowels
  private static final ImmutableMap<String, String> phoneToPhoneClass = ImmutableMap.<String, String>builder()
      .put("HH", "F")
      .put("B", "S")
      .put("D", "S")
      .put("DH", "F")
      .put("F", "F")
      .put("G", "S")
      .put("K", "S")
      .put("L", "L")
      .put("M", "N")
      .put("N", "N")
      .put("P", "S")
      .put("R", "L")
      .put("S", "F")
      .put("UH", "M")
      .put("T", "S")
      .put("SH", "F")
      .put("V", "F")
      .put("W", "I")
      .put("Y", "I")
      .put("Z", "F")
      .put("IH", "M")
      .put("AA", "M")
      .put("UW", "M")
      .put("EH", "M")
      .put("AE", "M")
      .put("CH", "A")
      .put("AH", "M")
      .put("OW", "D")
      .put("OY", "D")
      .put("ER", "R")
      .put("AO", "M")
      .put("ZH", "F")
      .put("IY", "M")
      .put("EY", "D")
      .put("TH", "F")
      .put("AW", "D")
      .put("AY", "D")
      .put("NG", "N")
      .put("JH", "A")
      .build();

  private static final ImmutableBiMap<String,PhoneClass> codeToEnum;
  private static final ImmutableSetMultimap<String,String> phoneClassToPhone;
  private static final ImmutableSet<String> simpleConsonantGraphs;
  static {
    // construct a list of "simple" consonant sounds (i.e. those that are usually 1-1 like stops and fricatives
    ImmutableSetMultimap.Builder<String, String> classBuilder = ImmutableSetMultimap.builder();
    ImmutableSet<String> phoneClassToInclude = ImmutableSet.of("S", "F");
    Set<String> simpleCons = Sets.newHashSet();
    for (Map.Entry<String, String> entry : phoneToPhoneClass.entrySet()) {
      if (phoneClassToInclude.contains(entry.getValue())) {
        // just the first letter as that's the corresponding "simple consonant" but note that this only works because
        // the fricative symbols above that have two chars, the leading one is the "simple cons" that I care about
        simpleCons.add(entry.getKey().substring(0, 1));
      }
      classBuilder.put(entry.getValue(), entry.getKey());
    }
    simpleConsonantGraphs = ImmutableSet.copyOf(simpleCons);
    phoneClassToPhone = classBuilder.build();
    ImmutableBiMap.Builder<String, PhoneClass> codeBuilder = ImmutableBiMap.builder();
    for (PhoneClass phoneClass : PhoneClass.values()) {
      codeBuilder.put(phoneClass.code(), phoneClass);
    }
    codeToEnum = codeBuilder.build();
  }
}
