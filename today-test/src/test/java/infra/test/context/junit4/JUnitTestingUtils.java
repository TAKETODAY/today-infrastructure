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

import org.junit.experimental.ParallelComputer;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Constructor;

import infra.beans.BeanUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Collection of utilities for testing the execution of JUnit 4 based tests.
 *
 * @author Sam Brannen
 * @see TrackingRunListener
 * @since 4.0
 */
public class JUnitTestingUtils {

  /**
   * Run the tests in the supplied {@code testClass}, using the {@link Runner}
   * configured via {@link RunWith @RunWith} or the default JUnit runner, and
   * assert the expectations of the test execution.
   *
   * @param testClass the test class to run with JUnit
   * @param expectedStartedCount the expected number of tests that started
   * @param expectedFailedCount the expected number of tests that failed
   * @param expectedFinishedCount the expected number of tests that finished
   * @param expectedIgnoredCount the expected number of tests that were ignored
   * @param expectedAssumptionFailedCount the expected number of tests that
   * resulted in a failed assumption
   */
  public static void runTestsAndAssertCounters(Class<?> testClass, int expectedStartedCount, int expectedFailedCount,
          int expectedFinishedCount, int expectedIgnoredCount, int expectedAssumptionFailedCount) throws Exception {

    runTestsAndAssertCounters(null, testClass, expectedStartedCount, expectedFailedCount, expectedFinishedCount,
            expectedIgnoredCount, expectedAssumptionFailedCount);
  }

  /**
   * Run the tests in the supplied {@code testClass}, using the specified
   * {@link Runner}, and assert the expectations of the test execution.
   *
   * <p>If the specified {@code runnerClass} is {@code null}, the tests
   * will be run with the runner that the test class is configured with
   * (i.e., via {@link RunWith @RunWith}) or the default JUnit runner.
   *
   * @param runnerClass the explicit runner class to use or {@code null}
   * if the default JUnit runner should be used
   * @param testClass the test class to run with JUnit
   * @param expectedStartedCount the expected number of tests that started
   * @param expectedFailedCount the expected number of tests that failed
   * @param expectedFinishedCount the expected number of tests that finished
   * @param expectedIgnoredCount the expected number of tests that were ignored
   * @param expectedAssumptionFailedCount the expected number of tests that
   * resulted in a failed assumption
   */
  public static void runTestsAndAssertCounters(Class<? extends Runner> runnerClass, Class<?> testClass,
          int expectedStartedCount, int expectedFailedCount, int expectedFinishedCount, int expectedIgnoredCount,
          int expectedAssumptionFailedCount) throws Exception {

    TrackingRunListener listener = new TrackingRunListener();

    if (runnerClass != null) {
      Constructor<?> constructor = runnerClass.getConstructor(Class.class);
      Runner runner = (Runner) BeanUtils.newInstance(constructor, testClass);
      RunNotifier notifier = new RunNotifier();
      notifier.addListener(listener);
      runner.run(notifier);
    }
    else {
      JUnitCore junit = new JUnitCore();
      junit.addListener(listener);
      junit.run(testClass);
    }

    assertSoftly(softly -> {
      softly.assertThat(listener.getTestStartedCount()).as("tests started for [%s]", testClass)
              .isEqualTo(expectedStartedCount);
      softly.assertThat(listener.getTestFailureCount()).as("tests failed for [%s]", testClass)
              .isEqualTo(expectedFailedCount);
      softly.assertThat(listener.getTestFinishedCount()).as("tests finished for [%s]", testClass)
              .isEqualTo(expectedFinishedCount);
      softly.assertThat(listener.getTestIgnoredCount()).as("tests ignored for [%s]", testClass)
              .isEqualTo(expectedIgnoredCount);
      softly.assertThat(listener.getTestAssumptionFailureCount()).as("failed assumptions for [%s]", testClass)
              .isEqualTo(expectedAssumptionFailedCount);
    });
  }

  /**
   * Run all tests in the supplied test classes according to the policies of
   * the supplied {@link Computer}, using the {@link Runner} configured via
   * {@link RunWith @RunWith} or the default JUnit runner, and assert the
   * expectations of the test execution.
   *
   * <p>To have all tests executed in parallel, supply {@link ParallelComputer#methods()}
   * as the {@code Computer}. To have all tests executed serially, supply
   * {@link Computer#serial()} as the {@code Computer}.
   *
   * @param computer the JUnit {@code Computer} to use
   * @param expectedStartedCount the expected number of tests that started
   * @param expectedFailedCount the expected number of tests that failed
   * @param expectedFinishedCount the expected number of tests that finished
   * @param expectedIgnoredCount the expected number of tests that were ignored
   * @param expectedAssumptionFailedCount the expected number of tests that
   * resulted in a failed assumption
   * @param testClasses one or more test classes to run
   */
  public static void runTestsAndAssertCounters(Computer computer, int expectedStartedCount, int expectedFailedCount,
          int expectedFinishedCount, int expectedIgnoredCount, int expectedAssumptionFailedCount,
          Class<?>... testClasses) throws Exception {

    JUnitCore junit = new JUnitCore();
    TrackingRunListener listener = new TrackingRunListener();
    junit.addListener(listener);
    junit.run(computer, testClasses);

    assertSoftly(softly -> {
      softly.assertThat(listener.getTestStartedCount()).as("tests started]").isEqualTo(expectedStartedCount);
      softly.assertThat(listener.getTestFailureCount()).as("tests failed]").isEqualTo(expectedFailedCount);
      softly.assertThat(listener.getTestFinishedCount()).as("tests finished]").isEqualTo(expectedFinishedCount);
      softly.assertThat(listener.getTestIgnoredCount()).as("tests ignored]").isEqualTo(expectedIgnoredCount);
      softly.assertThat(listener.getTestAssumptionFailureCount()).as("failed assumptions]").isEqualTo(expectedAssumptionFailedCount);
    });
  }

}
