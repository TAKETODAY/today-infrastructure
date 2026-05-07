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

package infra.testcontainers.lifecycle;

import org.testcontainers.lifecycle.Startable;

import infra.beans.BeansException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.core.Ordered;
import infra.core.annotation.Order;

/**
 * {@link BeanFactoryPostProcessor} to prevent {@link AutoCloseable} destruction calls so
 * that {@link TestcontainersLifecycleBeanPostProcessor} can be smarter about which
 * containers to close.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see TestcontainersLifecycleApplicationContextInitializer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class TestcontainersLifecycleBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    for (String beanName : beanFactory.getBeanNamesForType(Startable.class, false, false)) {
      try {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        String destroyMethodName = beanDefinition.getDestroyMethodName();
        if (destroyMethodName == null || AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName)) {
          beanDefinition.setDestroyMethodName("");
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Ignore
      }
    }
  }

}
