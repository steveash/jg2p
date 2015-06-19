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

package com.github.steveash.jg2p.util;

import com.google.common.primitives.Doubles;

/**
 * @author Steve Ash
 */
public class Scaler {

  public static double scaleLog(double val, double base) {
    if (val < 0) {
      throw new IllegalArgumentException("Cannot log scale a negative value " + val);
    }
    if (val == 0.0) {
      return 0.0;
    }
    double result = Math.log10(val) / Math.log10(base);
    if (!Doubles.isFinite(result)) {
      throw new IllegalArgumentException("problem with " + val + " and base " + base);
    }
    return result;
  }

  public static double scaleLogSquash(double val, double base, double max) {
    double maybe = scaleLog(val, base);
    if (maybe > max) return max;
    return maybe;
  }
}
