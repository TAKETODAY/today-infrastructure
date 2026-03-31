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

package infra.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.test.context.ContextHierarchy;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests which verify that {@link MockitoBean @MockitoBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoSpyBeanAndContextHierarchyChildIntegrationTests
 * @since 5.0
 */
@JUnitConfig
public class MockitoBeanAndContextHierarchyParentIntegrationTests {

  @MockitoBean
  ExampleService service;

  @BeforeEach
  void configureServiceMock() {
    given(service.greeting()).willReturn("mock");
  }

  @Test
  void test(ApplicationContext context) {
    assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).isEmpty();

    assertThat(service.greeting()).isEqualTo("mock");
  }

}
