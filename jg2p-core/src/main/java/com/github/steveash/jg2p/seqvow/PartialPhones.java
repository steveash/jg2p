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

package com.github.steveash.jg2p.seqvow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import com.github.steveash.jg2p.phoseq.Phonemes;
import com.github.steveash.jg2p.util.GramBuilder;

import java.util.Iterator;
import java.util.List;

import static com.github.steveash.jg2p.util.GramBuilder.SPLITTER;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * We distinguish between: (1) phonemes, (2) phoneme classes, (3) partialPhones (1) are actual phonemes (40 as used in
 * CMU Dict) (2) are the classes of phonemes (as in Phonemes.PhoneClasses) (3) are the symbols referring to the subset
 * of PhonemeClasses that we are going to re-tagg
 *
 * Graphones generally refer to the bijection of grouped graphemes to grouped final phonemes (i.e. the aligned X -> Y).
 * The phones here pass through a few layers and thus we need to distinguish between the list of symbols that are input
 * to the retagging process and the output of the retagging process.  Input to retagging will be called "Partial Tags"
 * and will be made up of Phonemes + partialPhones.  Output of retagging will be called "Final Tags"
 *
 * @author Steve Ash
 */
public class PartialPhones {

  public static final ImmutableSet<String> partialPhones = ImmutableSet.of(
      "!" + Phonemes.PhoneClasses.M.code().toUpperCase(),
      "!" + Phonemes.PhoneClasses.D.code().toUpperCase()
  );

  public static final ImmutableSet<String> partialPhoneClasses = ImmutableSet.of(
      Phonemes.PhoneClasses.M.code().toUpperCase(),
      Phonemes.PhoneClasses.D.code().toUpperCase()
  );

  public static String partialPhoneForPhone(String phone) {
    Preconditions.checkArgument(isPhoneEligibleAsPartial(phone));
    return "!" + Phonemes.getClassForPhone(phone);
  }

  public static boolean doesGramContainPartial(String gram) {
    if (GramBuilder.isUnaryGram(gram)) {
      return isPartialPhone(gram);
    }
    for (String s : SPLITTER.split(gram)) {
      if (isPartialPhone(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For the given gram made up of partialPhones and real phones -- return a gram with only the partial phones; strip
   * all symbols that are NOT partialPhones
   */
  public static String extractPartialPhoneGramFromGram(String gram) {
    if (isBlank(gram)) {
      return "";
    }
    if (GramBuilder.isUnaryGram(gram)) {
      if (isPartialPhone(gram)) {
        return gram;
      } else {
        return "";
      }
    }
    GramBuilder sb = new GramBuilder();

    for (String s : SPLITTER.split(gram)) {
      // skip any non vowels in this graphone phone
      if (isPartialPhone(s)) {
        sb.append(s);
      }
    }
    return sb.make();
  }

  /**
   * For the given gram made up of only real phones, returns the gram of real phones such that each included phone is
   * eligible as a partialPhone.  This is useful for making the target field of training examples
   */
  public static String extractEligibleGramFromPhoneGram(String phoneGram) {
    if (isBlank(phoneGram)) {
      return "";
    }
    if (GramBuilder.isUnaryGram(phoneGram)) {
      if (isPhoneEligibleAsPartial(phoneGram)) {
        return phoneGram;
      } else {
        return "";
      }
    }
    GramBuilder sb = new GramBuilder();

    for (String s : SPLITTER.split(phoneGram)) {
      if (isPhoneEligibleAsPartial(s)) {
        sb.append(s);
      }
    }
    return sb.make();
  }

  /**
   * Given a gram made up of partialPhones + finalPhones (i.e. the input to retagging) return the gram containing only
   * real phones; strip all of the partial phones from the gram
   */
  public static String extractNotPartialPhoneGramFromGram(String gram) {
    if (isBlank(gram)) {
      return "";
    }
    if (GramBuilder.isUnaryGram(gram)) {
      if (isPartialPhone(gram)) {
        return "";
      } else {
        return gram;
      }
    }
    GramBuilder sb = new GramBuilder();
    for (String s : SPLITTER.split(gram)) {
      if (!isPartialPhone(s)) {
        sb.append(s);
      }
    }
    return sb.make();
  }

  public static boolean isPartialPhone(String maybePartial) {
    return partialPhones.contains(maybePartial.toUpperCase());
  }

  public static boolean isPhoneEligibleAsPartial(String phone) {
    return partialPhoneClasses.contains(Phonemes.getClassForPhone(phone));
  }

  /**
   * Takes a partial gram containing one or more partialPhones and takes the output gram of final real phones from the
   * retagging process and returns a new grap with the retagged results applied
   */
  public static String partialGramUpdatedWithPredictedPhoneGram(String partialGram, String predictedPhoneGram) {
    // quick case first
    if (GramBuilder.isUnaryGram(partialGram) && GramBuilder.isUnaryGram(predictedPhoneGram)) {
      Preconditions.checkArgument(isPartialPhone(partialGram));
      Preconditions.checkArgument(Phonemes.isVowel(predictedPhoneGram));
      return predictedPhoneGram;
    }
    // if the seq vow model predicts more vowels than needed just leave off the extras; if too few, then just omit them
    Iterator<String> predicts = SPLITTER.split(predictedPhoneGram).iterator();
    GramBuilder sb = new GramBuilder();
    for (String s : SPLITTER.split(partialGram)) {
      if (isPartialPhone(s)) {
        if (predicts.hasNext()) {
          sb.append(predicts.next());
        }
      } else {
        sb.append(s);
      }
    }
    return sb.make();
  }

  // these are final graphones for training
  public static boolean doesAnyGramContainPhoneEligibleAsPartial(List<String> finalPhoneGrams) {
    for (String gram : finalPhoneGrams) {
      if (doesGramContainPhoneEligibleAsPartial(gram)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean doesGramContainPhoneEligibleAsPartial(String gram) {
    if (isBlank(gram)) {
      return false;
    }
    if (!GramBuilder.isUnaryGram(gram)) {
      for (String s : GramBuilder.SPLITTER.split(gram)) {
        if (isPhoneEligibleAsPartial(s)) {
          return true;
        }
      }
    } else {
      if (isPhoneEligibleAsPartial(gram)) {
        return true;
      }
    }
    return false;
  }

  // these are final graphones for training
  public static boolean doesAnyGramContainPartialPhone(List<String> partialPhoneGrams) {
    for (String gram : partialPhoneGrams) {
      if (doesGramContainPartial(gram)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Take a phoneGram and return the same gram with every eligible phone replaced with its corresponding partialPhone
   * tag.  This is used to create training data from aligned dictionary entries
   */
  public static String phoneGramToPartialPhoneGram(String finalPhoneGram) {
    if (isBlank(finalPhoneGram) || finalPhoneGram.equalsIgnoreCase(GramBuilder.EPS)) {
      return "";
    }

    if (GramBuilder.isUnaryGram(finalPhoneGram)) {
      if (isPhoneEligibleAsPartial(finalPhoneGram)) {
        return partialPhoneForPhone(finalPhoneGram);
      } else {
        return finalPhoneGram;
      }
    }
    // its a multi-gram graphone
    GramBuilder sb = new GramBuilder();
    for (String s : GramBuilder.SPLITTER.split(finalPhoneGram)) {
      if (isPhoneEligibleAsPartial(s)) {
        sb.append(partialPhoneForPhone(s));
      } else {
        sb.append(s);
      }
    }
    return sb.make();
  }

  public static List<String> phoneGramsToPartialPhoneGrams(List<String> finalPhoneGrams) {
    List<String> output = Lists.newArrayListWithCapacity(finalPhoneGrams.size());
    for (String phoneGram : finalPhoneGrams) {
      output.add(phoneGramToPartialPhoneGram(phoneGram));
    }
    return output;
  }

  public static String extractVowelOrPartialFromGram(String gram) {
    if (isBlank(gram)) {
      return "";
    }
    if (GramBuilder.isUnaryGram(gram)) {
      if (isPartialPhone(gram) || Phonemes.isVowel(gram)) {
        return gram;
      } else {
        return "";
      }
    }
    GramBuilder sb = new GramBuilder();
    for (String s : GramBuilder.SPLITTER.split(gram)) {
      if (isPartialPhone(s) || Phonemes.isVowel(s)) {
        sb.append(s);
      }
    }
    return sb.make();
  }
}
