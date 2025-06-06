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
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import infra.test.annotation.Repeat;
import infra.test.annotation.Timed;
import infra.test.context.TestExecutionListeners;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link InfraRunner}:
 * <ul>
 * <li>Infra {@link Repeat @Repeat}</li>
 * <li>Infra {@link Timed @Timed}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class RepeatedInfraRunnerTests {

  protected static final AtomicInteger invocationCount = new AtomicInteger();

  private final Class<?> testClass;

  private final int expectedFailureCount;
  private final int expectedStartedCount;
  private final int expectedFinishedCount;
  private final int expectedInvocationCount;

  @Parameters(name = "{0}")
  public static Object[][] repetitionData() {
    return new Object[][] {//
            { NonAnnotatedRepeatedTestCase.class.getSimpleName(), 0, 1, 1, 1 },//
            { DefaultRepeatValueRepeatedTestCase.class.getSimpleName(), 0, 1, 1, 1 },//
            { NegativeRepeatValueRepeatedTestCase.class.getSimpleName(), 0, 1, 1, 1 },//
            { RepeatedFiveTimesRepeatedTestCase.class.getSimpleName(), 0, 1, 1, 5 },//
            { RepeatedFiveTimesViaMetaAnnotationRepeatedTestCase.class.getSimpleName(), 0, 1, 1, 5 },//
            { TimedRepeatedTestCase.class.getSimpleName(), 3, 4, 4, (5 + 1 + 4 + 10) } //
    };
  }

  public RepeatedInfraRunnerTests(String testClassName, int expectedFailureCount,
          int expectedTestStartedCount, int expectedTestFinishedCount, int expectedInvocationCount) throws Exception {
    this.testClass = ClassUtils.forName(getClass().getName() + "." + testClassName, getClass().getClassLoader());
    this.expectedFailureCount = expectedFailureCount;
    this.expectedStartedCount = expectedTestStartedCount;
    this.expectedFinishedCount = expectedTestFinishedCount;
    this.expectedInvocationCount = expectedInvocationCount;
  }

  protected Class<? extends org.junit.runner.Runner> getRunnerClass() {
    return InfraRunner.class;
  }

  @Test
  public void assertRepetitions() throws Exception {
    invocationCount.set(0);

    JUnitTestingUtils.runTestsAndAssertCounters(getRunnerClass(), this.testClass, expectedStartedCount, expectedFailureCount,
            expectedFinishedCount, 0, 0);

    assertThat(invocationCount.get()).as("invocations for [" + testClass + "]:").isEqualTo(expectedInvocationCount);
  }

  @TestExecutionListeners({})
  public abstract static class AbstractRepeatedTestCase {

    protected void incrementInvocationCount() throws IOException {
      invocationCount.incrementAndGet();
    }
  }

  public static final class NonAnnotatedRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @Timed(millis = 10000)
    public void nonAnnotated() throws Exception {
      incrementInvocationCount();
    }
  }

  public static final class DefaultRepeatValueRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @Repeat
    @Timed(millis = 10000)
    public void defaultRepeatValue() throws Exception {
      incrementInvocationCount();
    }
  }

  public static final class NegativeRepeatValueRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @Repeat(-5)
    @Timed(millis = 10000)
    public void negativeRepeatValue() throws Exception {
      incrementInvocationCount();
    }
  }

  public static final class RepeatedFiveTimesRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @Repeat(5)
    public void repeatedFiveTimes() throws Exception {
      incrementInvocationCount();
    }
  }

  @Repeat(5)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface RepeatedFiveTimes {
  }

  public static final class RepeatedFiveTimesViaMetaAnnotationRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @RepeatedFiveTimes
    public void repeatedFiveTimes() throws Exception {
      incrementInvocationCount();
    }
  }

  /**
   * Unit tests for claims raised in .
   */
  @Ignore("TestCase classes are run manually by the enclosing test class")
  public static final class TimedRepeatedTestCase extends AbstractRepeatedTestCase {

    @Test
    @Timed(millis = 1000)
    @Repeat(5)
    public void repeatedFiveTimesButDoesNotExceedTimeout() throws Exception {
      incrementInvocationCount();
    }

    @Test
    @Timed(millis = 10)
    @Repeat(1)
    public void singleRepetitionExceedsTimeout() throws Exception {
      incrementInvocationCount();
      Thread.sleep(15);
    }

    @Test
    @Timed(millis = 20)
    @Repeat(4)
    public void firstRepetitionOfManyExceedsTimeout() throws Exception {
      incrementInvocationCount();
      Thread.sleep(25);
    }

    @Test
    @Timed(millis = 100)
    @Repeat(10)
    public void collectiveRepetitionsExceedTimeout() throws Exception {
      incrementInvocationCount();
      Thread.sleep(11);
    }
  }

}
