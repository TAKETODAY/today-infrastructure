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
