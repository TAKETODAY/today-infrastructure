/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AliasFor;
import infra.test.annotation.Timed;
import infra.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link InfraRunner}:
 * <ul>
 * <li>JUnit's {@link Test#timeout() @Test(timeout=...)}</li>
 * <li>Infra {@link Timed @Timed}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
public class TimedInfraRunnerTests {

  protected Class<?> getTestCase() {
    return TimedInfraRunnerTestCase.class;
  }

  protected Class<? extends org.junit.runner.Runner> getRunnerClass() {
    return InfraRunner.class;
  }

  @Test
  public void timedTests() throws Exception {
    JUnitTestingUtils.runTestsAndAssertCounters(getRunnerClass(), getTestCase(), 7, 5, 7, 0, 0);
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners({})
  public static class TimedInfraRunnerTestCase {

    // Should Pass.
    @Test(timeout = 2000)
    public void jUnitTimeoutWithNoOp() {
      /* no-op */
    }

    // Should Pass.
    @Test
    @Timed(millis = 2000)
    public void infraTimeoutWithNoOp() {
      /* no-op */
    }

    // Should Fail due to timeout.
    @Test(timeout = 10)
    public void jUnitTimeoutWithSleep() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to timeout.
    @Test
    @Timed(millis = 10)
    public void infraTimeoutWithSleep() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to timeout.
    @Test
    @MetaTimed
    public void infraTimeoutWithSleepAndMetaAnnotation() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to timeout.
    @Test
    @MetaTimedWithOverride(millis = 10)
    public void infraTimeoutWithSleepAndMetaAnnotationAndOverride() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to duplicate configuration.
    @Test(timeout = 200)
    @Timed(millis = 200)
    public void infraAndJUnitTimeouts() {
      /* no-op */
    }
  }

  @Timed(millis = 10)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimed {
  }

  @Timed(millis = 1000)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimedWithOverride {

    @AliasFor(annotation = Timed.class)
    long millis() default 1000;
  }

}
