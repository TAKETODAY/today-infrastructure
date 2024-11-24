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

import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.util.ClassUtils;

/**
 * {@link TestExecutionListener} to reset any mock beans that have been marked with a
 * {@link MockReset}. Typically used alongside {@link MockitoTestExecutionListener}.
 *
 * @author Phillip Webb
 * @see MockitoTestExecutionListener
 * @since 4.0
 */
public class ResetMocksTestExecutionListener extends AbstractTestExecutionListener {

  private static final boolean MOCKITO_IS_PRESENT = ClassUtils.isPresent("org.mockito.MockSettings",
          ResetMocksTestExecutionListener.class.getClassLoader());

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    if (MOCKITO_IS_PRESENT) {
      resetMocks(testContext.getApplicationContext(), MockReset.BEFORE);
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (MOCKITO_IS_PRESENT) {
      resetMocks(testContext.getApplicationContext(), MockReset.AFTER);
    }
  }

  private void resetMocks(ApplicationContext applicationContext, MockReset reset) {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      resetMocks((ConfigurableApplicationContext) applicationContext, reset);
    }
  }

  private void resetMocks(ConfigurableApplicationContext applicationContext, MockReset reset) {
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    String[] names = beanFactory.getBeanDefinitionNames();
    Set<String> instantiatedSingletons = new HashSet<>(Arrays.asList(beanFactory.getSingletonNames()));
    for (String name : names) {
      BeanDefinition definition = beanFactory.getBeanDefinition(name);
      if (definition.isSingleton() && instantiatedSingletons.contains(name)) {
        Object bean = beanFactory.getSingleton(name);
        if (reset.equals(MockReset.get(bean))) {
          Mockito.reset(bean);
        }
      }
    }
    try {
      MockitoBeans mockedBeans = beanFactory.getBean(MockitoBeans.class);
      for (Object mockedBean : mockedBeans) {
        if (reset.equals(MockReset.get(mockedBean))) {
          Mockito.reset(mockedBean);
        }
      }
    }
    catch (NoSuchBeanDefinitionException ex) {
      // Continue
    }
    if (applicationContext.getParent() != null) {
      resetMocks(applicationContext.getParent(), reset);
    }
  }

}
