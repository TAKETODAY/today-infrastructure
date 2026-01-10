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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.context.ApplicationEvent;
import infra.context.annotation.Configuration;
import infra.context.event.EventListener;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for custom event publication via
 * {@link TestContext#publishEvent(java.util.function.Function)}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@TestExecutionListeners(listeners = CustomTestEventTests.CustomEventPublishingTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
public class CustomTestEventTests {

  private static final List<CustomEvent> events = new ArrayList<>();

  @BeforeEach
  public void clearEvents() {
    events.clear();
  }

  @Test
  public void customTestEventPublished() {
    assertThat(events).hasSize(1);
    CustomEvent customEvent = events.get(0);
    assertThat(customEvent.getSource()).isEqualTo(getClass());
    assertThat(customEvent.getTestName()).isEqualTo("customTestEventPublished");
  }

  @Configuration
  static class Config {

    @EventListener
    void processCustomEvent(CustomEvent event) {
      events.add(event);
    }
  }

  @SuppressWarnings("serial")
  static class CustomEvent extends ApplicationEvent {

    private final Method testMethod;

    public CustomEvent(Class<?> testClass, Method testMethod) {
      super(testClass);
      this.testMethod = testMethod;
    }

    String getTestName() {
      return this.testMethod.getName();
    }
  }

  static class CustomEventPublishingTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestExecution(TestContext testContext) throws Exception {
      testContext.publishEvent(tc -> new CustomEvent(tc.getTestClass(), tc.getTestMethod()));
    }
  }

}
