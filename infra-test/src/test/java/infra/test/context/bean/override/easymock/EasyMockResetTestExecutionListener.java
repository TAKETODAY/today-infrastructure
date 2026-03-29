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

package infra.test.context.bean.override.easymock;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.test.context.TestContext;
import infra.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that provides support for resetting mocks
 * created via {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class EasyMockResetTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    resetMocks(testContext.getApplicationContext());
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    resetMocks(testContext.getApplicationContext());
  }

  private void resetMocks(ApplicationContext applicationContext) {
    if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
      resetMocks(configurableContext);
    }
  }

  private void resetMocks(ConfigurableApplicationContext applicationContext) {
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    try {
      beanFactory.getBean(EasyMockBeans.class).resetAll();
    }
    catch (NoSuchBeanDefinitionException ex) {
      // Continue
    }
    if (applicationContext.getParent() != null) {
      resetMocks(applicationContext.getParent());
    }
  }

}
