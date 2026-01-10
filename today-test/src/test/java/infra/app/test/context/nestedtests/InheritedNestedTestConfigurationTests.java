/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context.nestedtests;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.app.InfraConfiguration;
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
