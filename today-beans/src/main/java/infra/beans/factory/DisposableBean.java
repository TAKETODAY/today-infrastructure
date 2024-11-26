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

package infra.beans.factory;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Interface to be implemented by beans that want to release resources on destruction.
 * A {@link BeanFactory} will invoke the destroy method on individual destruction of a
 * scoped bean. An {@link infra.context.ApplicationContext} is supposed
 * to dispose all of its singletons on shutdown, driven by the application lifecycle.
 *
 * <p>A Infra-managed bean may also implement Java's {@link AutoCloseable} interface
 * for the same purpose. An alternative to implementing an interface is specifying a
 * custom destroy method, for example in an XML bean definition. For a list of all
 * bean lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InitializingBean
 * @see BeanDefinition#getDestroyMethodName()
 * @see ConfigurableBeanFactory#destroySingletons()
 * @see infra.context.ConfigurableApplicationContext#close()
 * @since 2018-7-18 1:07:15
 */
public interface DisposableBean {

  /**
   * Invoked by a ApplicationContext on destruction of a singleton.
   *
   * @throws Exception in case of errors
   */
  void destroy() throws Exception;
}
