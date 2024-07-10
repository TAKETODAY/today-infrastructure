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

package cn.taketoday.beans.factory;

/**
 * Callback interface for initializing a Infra {@link BeanFactory}
 * prior to entering the singleton pre-instantiation phase. Can be used to
 * trigger early initialization of specific beans before regular singletons.
 *
 * <p>Can be programmatically applied to a {@code BeanFactory} instance.
 * In an {@code ApplicationContext}, beans of type {@code BeanFactoryInitializer}
 * will be autodetected and automatically applied to the underlying bean factory.
 *
 * @param <F> the bean factory type
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.beans.factory.config.ConfigurableBeanFactory#preInstantiateSingletons()
 * @since 5.0
 */
public interface BeanFactoryInitializer<F extends BeanFactory> {

  /**
   * Initialize the given bean factory.
   *
   * @param beanFactory the bean factory to bootstrap
   */
  void initialize(F beanFactory);

}
