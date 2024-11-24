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

package infra.app.test.context.nestedtests;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.InfraConfiguration;
import infra.app.test.context.InfraTest;
import infra.app.test.mock.mockito.MockBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Import;
import infra.stereotype.Component;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Tests for nested test configuration when the configuration is inherited from the
 * enclosing class (the default behaviour).
 *
 * @author Andy Wilkinson
 */
@InfraTest(classes = InheritedNestedTestConfigurationTests.AppConfiguration.class)
@Import(InheritedNestedTestConfigurationTests.ActionPerformer.class)
class InheritedNestedTestConfigurationTests {

  @MockBean
  Action action;

  @Autowired
  ActionPerformer performer;

  @Test
  void mockWasInvokedOnce() {
    this.performer.run();
    then(this.action).should().perform();
  }

  @Test
  void mockWasInvokedTwice() {
    this.performer.run();
    this.performer.run();
    then(this.action).should(times(2)).perform();
  }

  @Nested
  class InnerTests {

    @Test
    void mockWasInvokedOnce() {
      InheritedNestedTestConfigurationTests.this.performer.run();
      then(InheritedNestedTestConfigurationTests.this.action).should().perform();
    }

    @Test
    void mockWasInvokedTwice() {
      InheritedNestedTestConfigurationTests.this.performer.run();
      InheritedNestedTestConfigurationTests.this.performer.run();
      then(InheritedNestedTestConfigurationTests.this.action).should(times(2)).perform();
    }

  }

  @Component
  static class ActionPerformer {

    private final Action action;

    ActionPerformer(Action action) {
      this.action = action;
    }

    void run() {
      this.action.perform();
    }

  }

  public interface Action {

    void perform();

  }

  @InfraConfiguration
  static class AppConfiguration {

  }

}
