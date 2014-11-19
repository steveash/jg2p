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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;

import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;

/**
 * Simple continuous histogram that observes double values and puts them in equally sized bins.
 * This uses straightforward arithmetic with doubles, so theoretically there is a possibility for
 * overflow/underflow or precision problems, but fairly extensive testing has been unable to find
 * any serious issues.
 *
 * @author Steve Ash
 */
public class Histogram {

    /**
     * Represents one bin in the histogram; the count is the count in the bin
     */
    @Immutable
    public static class HistogramBin {
        public final double binMinimumInclusive;
        public final double binMaximumExclusive;
        public final int binIndex;

        public final int count;

        private HistogramBin(double binMinimumInclusive, double binMaximumExclusive, int binIndex, int count) {
            this.binMinimumInclusive = binMinimumInclusive;
            this.binMaximumExclusive = binMaximumExclusive;
            this.binIndex = binIndex;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("[%.3f,%.3f)=%d", binMinimumInclusive, binMaximumExclusive, count);
        }
    }

    private static Predicate<HistogramBin> EmptyBinFilter = new Predicate<HistogramBin>() {
        @Override
        public boolean apply(@Nullable HistogramBin input) {
            return input.count > 0;
        }
    };

    private static final Function<HistogramBin, String>
        convertBinToString = new Function<HistogramBin, String>() {
        @Override
        public String apply(@Nullable HistogramBin input) {
            return input.toString();
        }
    };

    private final double min;
    private final double maxExcl;
    private final int binCount;
    private final double binWidth;
    private final int[] histogram;

    public Histogram(double min, double maxExcl, int binCount) {
        this.min = min;
        this.maxExcl = maxExcl;
        this.binCount = binCount;
        this.binWidth = computeBinWidth(min, maxExcl, binCount);
        this.histogram = new int[binCount];
    }

    /**
     * copy constructor
     */
    Histogram(Histogram backing) {
        min = backing.min;
        maxExcl = backing.maxExcl;
        binCount = backing.binCount;
        binWidth = backing.binWidth;
        histogram = Arrays.copyOf(backing.histogram, backing.histogram.length);
    }

    private double computeBinWidth(double min, double maxExcl, int binCount) {
        return (maxExcl - min) / binCount;
    }

    /**
     * Returns the count in this bin where the bin is identified by the 0-based index;
     * i.e. if there are 10 bins for x values [0.0, 1.0) then the x bin at index 0 covers
     * the range [0.0, 0.10) and the x bin at index 9 (the last bin) covers the range
     * [0.9, 1.0)
     */
    public int getCountAtIndex(int index) {
        return histogram[index];
    }

    /**
     * Returns the count in this bin which corresponds to where the given value
     * would be placed. Thus, if you had a histogram covering [0.0, 1.0) with 10 bins,
     * then asking for the count at 0.15 would return the count of the bin corresponding
     * to index 1, because the 1st index corresponds to the interval [0.1, 0.2).
     */
    public int getCountAt(double sampleValue) {
        int index = convertSampleToBinIndex(sampleValue);
        return getCountAtIndex(index);
    }

    /**
     * Adds one sample to the histogram (i.e. increments the count in the bucket corresponding to x)
     */
    public void add(double x) {
        int index = convertSampleToBinIndex(x);
        histogram[index] += 1;
    }

    private int convertSampleToBinIndex(double sampleValue) {
        if (sampleValue <= min)
            return 0;

        double shiftedToZero = (sampleValue - min);
        int index = (int) Math.floor(shiftedToZero / binWidth);

        if (index >= binCount)
            return binCount - 1;

        return index;
    }

    @VisibleForTesting
    double getMinBinRange(int index) {
        return index * binWidth + min;
    }

    @VisibleForTesting
    double getMaxBinRange(int index) {
        return getMinBinRange(index) + binWidth;
    }

    public Iterator<HistogramBin> iterator() {
        return new AbstractIterator<HistogramBin>() {
            int cursor = 0;

            @Override
            protected HistogramBin computeNext() {
                if (cursor >= histogram.length)
                    return endOfData();

                HistogramBin ret = new HistogramBin(getMinBinRange(cursor), getMaxBinRange(cursor), cursor,
                        histogram[cursor]);
                cursor++;
                return ret;
            }
        };
    }

    public Iterator<HistogramBin> iteratorNonEmptyBins() {
        return filter(iterator(), EmptyBinFilter);
    }

    public String nonEmptyBinsAsString() {
        return Joiner.on(',').join(transform(iteratorNonEmptyBins(), convertBinToString));
    }

    @Override
    public String toString() {
        return "Histogram [histo=" + nonEmptyBinsAsString() + "]";
    }

    public void merge(Histogram otherHisto) {
        throwIfNotMergeable(otherHisto);
        for (int i = 0; i < binCount; i++)
            histogram[i] += otherHisto.histogram[i];
    }

    private void throwIfNotMergeable(Histogram other) {
        Preconditions.checkState(min == other.min);
        Preconditions.checkState(maxExcl == other.maxExcl);
        Preconditions.checkState(binCount == other.binCount);
    }

    /**
     * Returns a brand new instance with results of merge
     */
    public static Histogram merge(Histogram co1, Histogram co2) {
        Histogram histo = new Histogram(co1);
        histo.merge(co2);
        return histo;
    }

    public double getMin() {
        return min;
    }

    public double getMaxExcl() {
        return maxExcl;
    }

    public int getBinCount() {
        return binCount;
    }

    public double getBinWidth() {
        return binWidth;
    }

    public void clear() {
        Arrays.fill(histogram, 0);
    }
}
