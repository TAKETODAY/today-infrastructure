/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    return TimedSpringRunnerTestCase.class;
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
  public static class TimedSpringRunnerTestCase {

    // Should Pass.
    @Test(timeout = 2000)
    public void jUnitTimeoutWithNoOp() {
      /* no-op */
    }

    // Should Pass.
    @Test
    @Timed(millis = 2000)
    public void springTimeoutWithNoOp() {
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
    public void springTimeoutWithSleep() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to timeout.
    @Test
    @MetaTimed
    public void springTimeoutWithSleepAndMetaAnnotation() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to timeout.
    @Test
    @MetaTimedWithOverride(millis = 10)
    public void springTimeoutWithSleepAndMetaAnnotationAndOverride() throws Exception {
      Thread.sleep(200);
    }

    // Should Fail due to duplicate configuration.
    @Test(timeout = 200)
    @Timed(millis = 200)
    public void springAndJUnitTimeouts() {
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
    long millis() default 1000;
  }

}
