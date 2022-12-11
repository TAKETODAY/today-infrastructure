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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;
import cn.taketoday.framework.test.mock.mockito.example.ExampleServiceCaller;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MockBean @MockBean} can be used with a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy({ @ContextConfiguration(classes = MockBeanOnContextHierarchyIntegrationTests.ParentConfig.class),
        @ContextConfiguration(classes = MockBeanOnContextHierarchyIntegrationTests.ChildConfig.class) })
class MockBeanOnContextHierarchyIntegrationTests {

  @Autowired
  private ChildConfig childConfig;

  @Test
  void testMocking() {
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
  @MockBean(ExampleService.class)
  static class ParentConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @MockBean(ExampleServiceCaller.class)
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
