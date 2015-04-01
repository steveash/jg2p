import com.github.steveash.jg2p.align.AlignModel
import com.github.steveash.jg2p.util.Histogram
import com.github.steveash.jg2p.util.JenksBreaks
import com.github.steveash.jg2p.util.Percent
import com.github.steveash.jg2p.util.ReadWrite
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.math3.stat.descriptive.rank.Percentile

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

/**
 * @author Steve Ash
 */
AlignModel model = ReadWrite.readFromFile(AlignModel.class, new File("../resources/am_cmudict_22_xeps_ww_A.dat"))
def t = model.getTransitions()
double minScore = 1.0E-100
int oneToOne = 0
int oneToTwo = 0
int twoToOne = 0
int xCount = 0
//def oneToTwoHisto = new Histogram(0, 1.0, 10)
//def oneToOneHisto = new Histogram(0, 1.0, 10)
//def twoToOneHisto = new Histogram(0, 1.0, 10)

private int getDecimalExponent(double v) {
  def vv = Double.toString(v)
  def loc = vv.indexOf("E")
  if (loc < 0) {
    return 0
  }
  return Integer.parseInt(vv.substring(loc + 1))
}

def aboveHisto = new Histogram(0, 50, 50)
def threshHisto = new Histogram(-400, 0, 40)
//def threshHisto = new Histogram(0, 1, 100)
def gofHisto = new Histogram(0, 1, 20);
def threshThreshCount = 0
def noBreakCount = 0
def oneOneProbs = []
def oneTwoProbs = []
def twoOneProbs = []

t.xRows().each { x ->

  def yMap = t.getYProbForX(x)
  def powVal = yMap.values().collect { getDecimalExponent(it) as double }
//  def powVal = yMap.values().collect { it as double }
  def thresh
  def breaks = null
  def gof = 0

  if (powVal.size() <= 2) {
    thresh = powVal[0]
  } else {
    breaks = JenksBreaks.computeBreaks(powVal, 2)
    gof = JenksBreaks.goodnessOfFit(powVal, breaks)
    gofHisto.add(gof)
    if (gof < 0.15) {

    }
    thresh = breaks[1]
    if (!(breaks[0] != breaks[1] && breaks[1] != breaks[2])) {
      noBreakCount += 1
    }
  }
  def goodEntryCount = powVal.count { it >= thresh }
  if (goodEntryCount > 50) {
    threshThreshCount += 1
  }
  aboveHisto.add(goodEntryCount.doubleValue());

  threshHisto.add(thresh)
  if (breaks != null && xCount < 10) {
    yMap.entrySet().toList().sort { it.value }.
        each { println "For $x -> ${it.key} = ${it.value} == ${getDecimalExponent(it.value)}" }
    println "Got X breaks " + breaks + " (gof: " + gof + ")"
    println "---------------------------------"
  }
  xCount += 1;

  yMap.each { y, score ->
    boolean x2 = x.contains(" ")
    boolean y2 = y.contains(" ")

    if (!x2 && !y2) {
      oneOneProbs.add(score)
    } else if (y2) {
      oneTwoProbs.add(score)
    } else {
      twoOneProbs.add(score)
    }

    if (score <= minScore) {
      return
    }

    if (!x2 && !y2) {
      oneToOne += 1
      oneOneProbs.add(score)
    } else if (y2) {
      oneToTwo += 1
      oneTwoProbs.add(score)
    } else {
      assert x2: " x $x, y $y"
      twoToOne += 1
      twoOneProbs.add(score)
    }
  }
}
println "X's " + t.xRows().size()
println "Y's " + t.yCols().size()
int sum = oneToOne + oneToTwo + twoToOne
println "1-1 $oneToOne " + Percent.print(oneToOne, sum)
println "1-2 $oneToTwo " + Percent.print(oneToTwo, sum)
println "2-1 $twoToOne " + Percent.print(twoToOne, sum)
println "Thresh below K count " + threshThreshCount
println "No break entries " + noBreakCount
println "Histo of above thresh count " + aboveHisto.nonEmptyBinsAsStringLines()
println "Histo of threshes " + threshHisto.nonEmptyBinsAsStringLines()
//println "Histo of gofs "  + gofHisto.nonEmptyBinsAsStringLines()

def oneOneBreak = 0, oneTwoBreak = 0, twoOneBreak = 0
[oneOneProbs, oneTwoProbs, twoOneProbs].eachWithIndex { List vals, int index ->
  def p = new Percentile()
  assert vals.size() > 0
  def primArray = ArrayUtils.toPrimitive((Double[]) vals.toArray())
  p.setData(primArray)
  println "--------------------------------------"
  println "10p = " + p.evaluate(10) + ", 25p = " + p.evaluate(25) + ", 50p = " + p.evaluate(50) + ", 75p = " +
          p.evaluate(75) + ", 95p = " + p.evaluate(95)
  println "Count " + vals.size()
  def powVal = vals.collect { getDecimalExponent(it) as double }
  def breaks = JenksBreaks.computeBreaks(powVal, 2)
  println "Breaks " + breaks + " gof " + JenksBreaks.goodnessOfFit(powVal, breaks)
  if (index == 0) {
    oneOneBreak = breaks[1]
  }
  if (index == 1) {
    oneTwoBreak = breaks[1]
  }
  if (index == 2) {
    twoOneBreak = breaks[1]
  }
}

// going to see how many qualify after the breaks
def oneOnePassCount = 0, oneTwoPassCount = 0, twoOnePassCount = 0
new File("possible-aligns.txt").withPrintWriter { pw ->
  t.xRows().each { x ->
    t.getYProbForX(x).each { y, score ->
      boolean x2 = x.contains(" ")
      boolean y2 = y.contains(" ")
      def dexp = getDecimalExponent(score)
      if (!x2 && !y2) {
        if (dexp >= oneOneBreak) {
          oneOnePassCount += 1
          pw.println("$x^$y")
        }
      } else if (y2) {
        if (dexp >= oneTwoBreak) {
          oneTwoPassCount += 1
          pw.println("$x^$y")
        }
      } else {
        if (dexp >= twoOneBreak) {
          twoOnePassCount += 1
          pw.println("$x^$y")
        }
      }
    }
  }
}
println "one one pass count $oneOnePassCount"
println "one two pass count $oneTwoPassCount"
println "two one pass count $twoOnePassCount"