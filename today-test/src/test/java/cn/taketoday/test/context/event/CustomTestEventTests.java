/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static cn.taketoday.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
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
