import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.operator.PoisonPill
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

/**
 * @author Steve Ash
 */
class TestGpars {

  private static final Logger log = LoggerFactory.getLogger(TestGpars.class);

  @Ignore
  @Test
  public void shouldRunConcurrently() throws Exception {
    GParsPool.withPool {
      def df = new DataflowQueue()
      def work = Dataflow.task {
        log.info("Starting consumer task")
        try {
          while (true) {
            def gotit = df.val
            if (gotit instanceof PoisonPill) {
              log.info("Got the poison pill")
              return
            }
            log.info("Consumer got " + gotit)
            Thread.currentThread().sleep(100)
          }
        } catch (RuntimeException e) {
          log.info("Consumer caught an exception " + e)
          throw e
        }
      }
      (0..100).eachParallel {
        log.info("Producer is about to write $it ")
        Thread.currentThread().sleep(10)
        df << it
        Thread.currentThread().sleep(10)
      }
      log.info("Finished all things from the producer")
      df << PoisonPill.instance
      work.get()
    }
    log.info("Just finished the gpars loop")
  }
}
