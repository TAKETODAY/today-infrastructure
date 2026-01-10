/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.ArrayList;
import java.util.List;

import infra.context.annotation.Configuration;
import infra.core.annotation.Order;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;
import infra.test.context.TestExecutionListeners.MergeMode;
import infra.test.context.event.annotation.AfterTestClass;
import infra.test.context.event.annotation.AfterTestExecution;
import infra.test.context.event.annotation.AfterTestMethod;
import infra.test.context.event.annotation.BeforeTestClass;
import infra.test.context.event.annotation.BeforeTestExecution;
import infra.test.context.event.annotation.BeforeTestMethod;
import infra.test.context.event.annotation.PrepareTestInstance;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for the {@link EventPublishingTestExecutionListener} which verify that
 * a {@link BeforeTestClassEvent} can be eagerly published; whereas, such an
 * event is not published by default for the first run of a test class for a
 * specific {@code ApplicationContext}.
 *
 * @author Sam Brannen
 * @see https://github.com/spring-projects/spring-framework/issues/27757
 * @since 4.0
 */
class EagerTestExecutionEventPublishingTests {

  private static final List<Class<? extends TestContextEvent>> events = new ArrayList<>();

  @BeforeEach
  @AfterEach
  void resetEvents() {
    events.clear();
  }

  @Test
  void beforeTestClassEventIsNotPublishedByDefaultForFirstTestClass() {
    EngineTestKit.engine("junit-jupiter")//
            .selectors(selectClass(LazyTestCase1.class), selectClass(LazyTestCase2.class))//
            .execute()//
            .testEvents()//
            .assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

    assertThat(events).containsExactly(//
            // 1st test class
            // BeforeTestClassEvent.class -- missing for 1st test class
            PrepareTestInstanceEvent.class, //
            BeforeTestMethodEvent.class, //
            BeforeTestExecutionEvent.class, //
            AfterTestExecutionEvent.class, //
            AfterTestMethodEvent.class, //
            AfterTestClassEvent.class, //
            // 2nd test class
            BeforeTestClassEvent.class, //
            PrepareTestInstanceEvent.class, //
            BeforeTestMethodEvent.class, //
            BeforeTestExecutionEvent.class, //
            AfterTestExecutionEvent.class, //
            AfterTestMethodEvent.class, //
            AfterTestClassEvent.class//
    );
  }

  @Test
  void beforeTestClassEventIsPublishedForAllTestClassesIfCustomListenerEagerlyLoadsContext() {
    EngineTestKit.engine("junit-jupiter")//
            .selectors(selectClass(EagerTestCase1.class), selectClass(EagerTestCase2.class))//
            .execute()//
            .testEvents()//
            .assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

    assertThat(events).containsExactly(//
            // 1st test class
            BeforeTestClassEvent.class, //
            PrepareTestInstanceEvent.class, //
            BeforeTestMethodEvent.class, //
            BeforeTestExecutionEvent.class, //
            AfterTestExecutionEvent.class, //
            AfterTestMethodEvent.class, //
            AfterTestClassEvent.class, //
            // 2nd test class
            BeforeTestClassEvent.class, //
            PrepareTestInstanceEvent.class, //
            BeforeTestMethodEvent.class, //
            BeforeTestExecutionEvent.class, //
            AfterTestExecutionEvent.class, //
            AfterTestMethodEvent.class, //
            AfterTestClassEvent.class//
    );
  }

  @JUnitConfig(Config.class)
  static class LazyTestCase1 {

    @Test
    void test() {
    }
  }

  static class LazyTestCase2 extends LazyTestCase1 {
  }

  @TestExecutionListeners(listeners = EagerLoadingTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
  static class EagerTestCase1 extends LazyTestCase1 {
  }

  static class EagerTestCase2 extends EagerTestCase1 {
  }

  @Configuration
  static class Config {

    @BeforeTestClass
    public void beforeTestClass(BeforeTestClassEvent e) {
      events.add(e.getClass());
    }

    @PrepareTestInstance
    public void prepareTestInstance(PrepareTestInstanceEvent e) {
      events.add(e.getClass());
    }

    @BeforeTestMethod
    public void beforeTestMethod(BeforeTestMethodEvent e) {
      events.add(e.getClass());
    }

    @BeforeTestExecution
    public void beforeTestExecution(BeforeTestExecutionEvent e) {
      events.add(e.getClass());
    }

    @AfterTestExecution
    public void afterTestExecution(AfterTestExecutionEvent e) {
      events.add(e.getClass());
    }

    @AfterTestMethod
    public void afterTestMethod(AfterTestMethodEvent e) {
      events.add(e.getClass());
    }

    @AfterTestClass
    public void afterTestClass(AfterTestClassEvent e) {
      events.add(e.getClass());
    }

  }

  @Order(0)
  static class EagerLoadingTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
      testContext.getApplicationContext();
    }
  }

}
