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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import infra.beans.BeansException;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Extension to the standard {@link BeanFactoryPostProcessor} SPI, allowing for
 * the registration of further bean definitions <i>before</i> regular
 * BeanFactoryPostProcessor detection kicks in. In particular,
 * BeanDefinitionRegistryPostProcessor may register further bean definitions
 * which in turn define BeanFactoryPostProcessor instances.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.context.annotation.ConfigurationClassPostProcessor
 * @since 4.0 2021/12/8 15:00
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

  /**
   * Modify the application context's internal bean definition registry after its
   * standard initialization. All regular bean definitions will have been loaded,
   * but no beans will have been instantiated yet. This allows for adding further
   * bean definitions before the next post-processing phase kicks in.
   *
   * @param registry the bean definition registry used by the application context
   * @throws BeansException in case of errors
   */
  void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

  /**
   * Empty implementation of {@link BeanFactoryPostProcessor#postProcessBeanFactory}
   * since custom {@code BeanDefinitionRegistryPostProcessor} implementations will
   * typically only provide a {@link #postProcessBeanDefinitionRegistry} method.
   */
  @Override
  default void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {

  }

}
