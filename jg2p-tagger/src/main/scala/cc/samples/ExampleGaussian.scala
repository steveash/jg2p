object ExampleGaussian extends App {
  import cc.factorie._                             // The base library
  import cc.factorie.directed._                    // Factors for directed graphical models
  import cc.factorie.variable._
  import cc.factorie.model._
  import cc.factorie.infer._
  implicit val model = DirectedModel()             // Define the "model" that will implicitly store the new factors we create
  implicit val random = new scala.util.Random(0)   // Define a source of randomness that will be used implicitly in data generation below
  val mean = new DoubleVariable(10)                // A random variable for holding the mean of the Gaussian
  val variance = new DoubleVariable(1.0)           // A random variable for holding the variance of the Gaussian
  println("true mean %f variance %f".format(mean.value, variance.value))
  // Generate 1000 new random variables from this Gaussian distribution
  //  "~" would mean just "Add to the model a new factor connecting the parents and the child"
  //  ":~" does this and also assigns a new value to the child by sampling from the factor
  val data = for (i <- 1 to 1000) yield new DoubleVariable :~ Gaussian(mean, variance)
  // Set mean and variance to values that maximize the likelihood of the children
  Maximize(mean)
  Maximize(variance)
  println("estimated mean %f variance %f".format(mean.value, variance.value))
  println("Model " + model.allFactors)
}