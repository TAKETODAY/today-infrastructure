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
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} which verify that type-level
 * {@code @MockitoBean} declarations are not discovered more than once when searching
 * a type hierarchy, when an interface is <em>present</em> twice in the hierarchy.
 *
 * @author Sam Brannen
 * @see MockitoBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests
 * @see MockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 * @since 5.0
 */
class MockitoBeanWithInterfacePresentTwiceTests extends AbstractMockitoBeanWithInterfacePresentTwiceTests
        implements MockConfigInterface {

  @Test
  void test(ApplicationContext context) {
    assertIsMock(service);
    assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);

    // The following are prerequisites for the tested scenario.
    assertThat(getClass().getInterfaces()).containsExactly(MockConfigInterface.class);
    assertThat(getClass().getSuperclass().getInterfaces()).containsExactly(MockConfigInterface.class);
  }

}

@MockitoBean(types = ExampleService.class)
interface MockConfigInterface {
}

@ExtendWith(InfraExtension.class)
abstract class AbstractMockitoBeanWithInterfacePresentTwiceTests implements MockConfigInterface {

  @Autowired
  ExampleService service;

}
