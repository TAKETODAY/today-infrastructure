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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is effectively a one-to-one copy of
 * {@link MockitoSpyBeanByNameInParentAndChildContextHierarchyTests}, except
 * that this test class uses different names for the context hierarchy levels:
 * level-1 and level-2 instead of parent and child.
 *
 * <p>If the context cache is broken, either this test class or
 * {@code MockitoSpyBeanByNameInParentAndChildContextHierarchyTests} will fail
 * when run within the same test suite.
 *
 * @author Sam Brannen
 * @see MockitoSpyBeanByNameInParentAndChildContextHierarchyTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = MockitoSpyBeanByNameInParentAndChildContextHierarchyTests.Config1.class, name = "level-1"),
        @ContextConfiguration(classes = MockitoSpyBeanByNameInParentAndChildContextHierarchyTests.Config2.class, name = "level-2")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockitoSpyBeanByNameInParentAndChildContextHierarchyV2Tests {

  @MockitoSpyBean(name = "service", contextName = "level-1")
  ExampleService serviceInParent;

  @MockitoSpyBean(name = "service", contextName = "level-2")
  ExampleService serviceInChild;

  @Autowired
  ExampleServiceCaller serviceCaller1;

  @Autowired
  ExampleServiceCaller serviceCaller2;

  @Test
  void test() {
    assertIsSpy(serviceInParent);
    assertIsSpy(serviceInChild);

    assertThat(serviceInParent.greeting()).isEqualTo("Service 1");
    assertThat(serviceInChild.greeting()).isEqualTo("Service 2");
    assertThat(serviceCaller1.getService()).isSameAs(serviceInParent);
    assertThat(serviceCaller2.getService()).isSameAs(serviceInChild);
    assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Service 1");
    assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Service 2");
  }

}
