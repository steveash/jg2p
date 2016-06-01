import com.github.steveash.jg2p.Grams
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.ProbTable
import com.github.steveash.jg2p.phoseq.Phonemes
import com.github.steveash.jg2p.syll.SWord
import com.github.steveash.jg2p.train.PipelineTrainer
import com.github.steveash.jg2p.util.ModelReadWrite
import com.google.common.math.DoubleMath
import org.apache.commons.lang3.tuple.Pair

def inputFile = "cmu7b.train"
def testFile = "cmu7b.test"
def inputs = InputReader.makePSaurusReader().readFromClasspath(inputFile)
inputs = inputs.findAll { PipelineTrainer.keepTrainable.apply(it) }
def testInputs = InputReader.makePSaurusReader().readFromClasspath(testFile)
println "reading model..."
def aligner1 = ModelReadWrite.readTrainAlignerFrom("../resources/syllchainAlignNoConstrain.dat")

int total = 0
def pt1 = new ProbTable()
def pt2 = new ProbTable()

inputs.each { rec ->
  def sword = rec.right as SWord
  def res1 = aligner1.align(rec.left, rec.right, 1)
  if (res1.empty) {
    return
  }
  res1 = res1.first()
  total += 1
  if (total % 10000 == 0) {
    println "just did $total"
  }

  res1.graphonesSplit.each { Pair<List<String>, List<String>> pair ->
    pair.left.each { g ->
      pair.right.each { p ->
        if (Grams.EPSILON.equalsIgnoreCase(p)) {
          return
        }
        if (Grams.EPSILON.equalsIgnoreCase(g)) {
          return
        }
        def pc = Phonemes.getClassForPhone(p)
        pt1.addProb(g, p, 1.0)
        pt2.addProb(g, pc, 1.0)
      }
    }
  }
}

printOut(new File("../resources/graphphonespace.csv"), pt1)
printOut(new File("../resources/graphpclassspace.csv"), pt2)
println "done"

def printOut(File file, ProbTable rawTable) {

  file.withPrintWriter { pw ->
    def tab = rawTable.makeRowNormalizedCopy()
    def hdr = tab.yCols().sort()
    pw.println("X," + hdr.join(",") + ",Entropy")
    tab.xRows().sort().each { x ->
      StringBuilder sb = new StringBuilder()
      sb.append(x)
      double sum = 0;
      hdr.each { y ->
        def p = tab.prob(x, y)
        if (p > 0) {
          sum += (p * DoubleMath.log2(p))
        }
        sb.append(",").append(p)
      }

      sb.append(",").append(-1 * sum)
      pw.println(sb.toString())
    }
  }
}
