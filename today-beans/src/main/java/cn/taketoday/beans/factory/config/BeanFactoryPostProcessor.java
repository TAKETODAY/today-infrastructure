/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;

/**
 * Factory hook that allows for custom modification of an application context's
 * bean definitions, adapting the bean property values of the context's
 * underlying bean factory.
 *
 * <p>
 * A {@code BeanFactoryPostProcessor} may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * <h3>Registration</h3>
 * <p>
 * An {@code ApplicationContext} auto-detects {@code BeanFactoryPostProcessor}
 * beans in its bean definitions and applies them before any other beans get
 * created. A {@code BeanFactoryPostProcessor} may also be registered
 * programmatically with a {@code ConfigurableApplicationContext}.
 *
 * <h3>Ordering</h3>
 * <p>
 * {@code BeanFactoryPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link Ordered} semantics. In contrast,
 * {@code BeanFactoryPostProcessor} beans that are registered programmatically
 * with a
 * {@link cn.taketoday.context.ConfigurableApplicationContext#addBeanFactoryPostProcessor(BeanFactoryPostProcessor)
 * addBeanFactoryPostProcessor} will be applied in the order of registration;
 * any ordering semantics expressed through implementing the {@code Ordered}
 * interface will be ignored for programmatically registered post-processors.
 * Furthermore, the {@link Order @Order}
 * annotation is not taken into account for {@code BeanFactoryPostProcessor}
 * beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanPostProcessor
 * @since 2.1.7 2020-02-27 11:15
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

  /**
   * Modify the application context's internal bean factory after its standard
   * initialization. All bean definitions will have been loaded, but no beans will
   * have been instantiated yet. This allows for overriding or adding properties
   * even to eager-initializing beans.
   *
   * @param beanFactory the bean factory used by the application context
   * @throws BeansException in case of errors
   */
  void postProcessBeanFactory(ConfigurableBeanFactory beanFactory);

}
