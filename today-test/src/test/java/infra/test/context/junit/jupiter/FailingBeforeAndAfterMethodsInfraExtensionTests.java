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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import javax.sql.DataSource;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests run via the {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * <p>See:
 * and .
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class FailingBeforeAndAfterMethodsInfraExtensionTests {

  @ParameterizedTest
  @ValueSource(classes = {
          AlwaysFailingBeforeTestClassTestCase.class,
          AlwaysFailingAfterTestClassTestCase.class,
          AlwaysFailingPrepareTestInstanceTestCase.class,
          AlwaysFailingBeforeTestMethodTestCase.class,
          AlwaysFailingBeforeTestExecutionTestCase.class,
          AlwaysFailingAfterTestExecutionTestCase.class,
          AlwaysFailingAfterTestMethodTestCase.class,
  })
  void failingBeforeAndAfterCallbacks(Class<?> testClass) {
    Events events = EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(testClass))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats
                    .skipped(0)
                    .aborted(0)
                    .started(getExpectedStartedCount(testClass))
                    .succeeded(getExpectedSucceededCount(testClass))
                    .failed(getExpectedFailedCount(testClass)));

    // Ensure it was an AssertionError that failed the test and not
    // something else like an error in the @Configuration class, etc.
    if (getExpectedFailedCount(testClass) > 0) {
      events.assertThatEvents().haveExactly(1,
              event(test("testNothing"),
                      finishedWithFailure(
                              instanceOf(AssertionError.class),
                              message(msg -> msg.contains("always failing")))));
    }
  }

  private int getExpectedStartedCount(Class<?> testClass) {
    return (testClass == AlwaysFailingBeforeTestClassTestCase.class ? 0 : 1);
  }

  private int getExpectedSucceededCount(Class<?> testClass) {
    return (testClass == AlwaysFailingAfterTestClassTestCase.class ? 1 : 0);
  }

  private int getExpectedFailedCount(Class<?> testClass) {
    if (testClass == AlwaysFailingBeforeTestClassTestCase.class
            || testClass == AlwaysFailingAfterTestClassTestCase.class) {
      return 0;
    }
    return 1;
  }

  // -------------------------------------------------------------------

  private static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
      fail("always failing beforeTestClass()");
    }
  }

  private static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestClass(TestContext testContext) {
      fail("always failing afterTestClass()");
    }
  }

  private static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
      fail("always failing prepareTestInstance()");
    }
  }

  private static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
      fail("always failing beforeTestMethod()");
    }
  }

  private static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestExecution(TestContext testContext) {
      fail("always failing beforeTestExecution()");
    }
  }

  private static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestMethod(TestContext testContext) {
      fail("always failing afterTestMethod()");
    }
  }

  private static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestExecution(TestContext testContext) {
      fail("always failing afterTestExecution()");
    }
  }

  @FailingTestCase
  @ExtendWith(InfraExtension.class)
  private static abstract class BaseTestCase {

    @Test
    void testNothing() {
    }
  }

  @TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
  static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
  static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
  static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
  static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
  static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
  static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
  }

  @TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
  static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
  }

  // Must not be private.
  @Configuration
  static class DatabaseConfig {

    @Bean
    PlatformTransactionManager transactionManager() {
      return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }
  }

}
