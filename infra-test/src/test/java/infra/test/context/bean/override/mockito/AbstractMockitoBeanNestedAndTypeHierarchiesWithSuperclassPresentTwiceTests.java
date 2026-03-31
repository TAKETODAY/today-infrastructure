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
 * Abstract top-level class and abstract inner class for integration tests for
 * {@link MockitoBean @MockitoBean} which verify that {@code @MockitoBean} fields
 * are not discovered more than once when searching intertwined enclosing class
 * hierarchies and type hierarchies, when a superclass is <em>present</em> twice
 * in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @see MockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34844">gh-34844</a>
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
abstract class AbstractMockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests {

  @Autowired
  ApplicationContext enclosingContext;

  @MockitoBean
  ExampleService service;

  @Test
  void topLevelTest() {
    assertIsMock(service);
    assertThat(enclosingContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
  }

  abstract class AbstractBaseClassForNestedTests {

    @Test
    void nestedTest(ApplicationContext nestedContext) {
      assertIsMock(service);
      assertThat(enclosingContext).isSameAs(nestedContext);
      assertThat(enclosingContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
    }
  }

}
