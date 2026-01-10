/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.mock.mockito.example.ExampleService;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.SimpleExampleService;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link SpyBean @SpyBean} can be used with a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy({ @ContextConfiguration(classes = SpyBeanOnContextHierarchyIntegrationTests.ParentConfig.class),
        @ContextConfiguration(classes = SpyBeanOnContextHierarchyIntegrationTests.ChildConfig.class) })
class SpyBeanOnContextHierarchyIntegrationTests {

  @Autowired
  private ChildConfig childConfig;

  @Test
  void testSpying() {
    ApplicationContext context = this.childConfig.getContext();
    ApplicationContext parentContext = context.getParent();
    assertThat(parentContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
    assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(0);
    assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(0);
    assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);
    assertThat(context.getBean(ExampleService.class)).isNotNull();
    assertThat(context.getBean(ExampleServiceCaller.class)).isNotNull();
  }

  @Configuration(proxyBeanMethods = false)
  @SpyBean(SimpleExampleService.class)
  static class ParentConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @SpyBean(ExampleServiceCaller.class)
  static class ChildConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.context = applicationContext;
    }

    ApplicationContext getContext() {
      return this.context;
    }

  }

}
