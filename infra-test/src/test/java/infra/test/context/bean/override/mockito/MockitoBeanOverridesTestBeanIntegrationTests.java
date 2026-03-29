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

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.bean.override.convention.TestBean;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.RealExampleService;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for Bean Overrides where a {@link MockitoBean @MockitoBean}
 * overrides a {@link TestBean @TestBean} when trying to replace the same existing
 * bean, selected by-type.
 *
 * <p>In other words, this test class demonstrates how one Bean Override can
 * silently override another Bean Override.
 *
 * @author Sam Brannen
 * @see MockitoBeanDuplicateTypeCreationIntegrationTests
 * @see MockitoBeanDuplicateTypeReplacementIntegrationTests
 * @since 5.0
 */
@JUnitConfig
public class MockitoBeanOverridesTestBeanIntegrationTests {

  @TestBean
  ExampleService testService;

  @MockitoBean
  ExampleService mockService;

  @Autowired
  List<ExampleService> services;

  static ExampleService testService() {
    return new RealExampleService("@TestBean");
  }

  /**
   * One could argue that we would ideally expect an exception to be thrown when
   * two competing overrides are created to replace the same existing bean; however,
   * we currently only log a warning in such cases.
   * <p>This method therefore asserts the status quo in terms of behavior.
   * <p>And the log can be manually checked to verify that an appropriate
   * warning was logged.
   */
  @Test
  void mockitoBeanShouldOverrideTestBean() {
    // Currently logs something similar to the following.
    //
    // WARN - Bean with name 'exampleService' was overridden by multiple handlers:
    // [TestBeanOverrideHandler@770beef5 ..., MockitoBeanOverrideHandler@6dd1f638 ...]

    // Last override wins...
    assertThat(services).containsExactly(mockService);
    assertThat(testService).isSameAs(mockService);

    assertIsMock(mockService);

    assertThat(mockService.greeting()).isNull();
    given(mockService.greeting()).willReturn("mocked");
    assertThat(mockService.greeting()).isEqualTo("mocked");
  }

  @Configuration
  static class Config {

    @Bean
    ExampleService exampleService() {
      return () -> "@Bean";
    }
  }

}
