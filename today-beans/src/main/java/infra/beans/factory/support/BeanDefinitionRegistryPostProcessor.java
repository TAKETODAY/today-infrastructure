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
