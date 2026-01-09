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
