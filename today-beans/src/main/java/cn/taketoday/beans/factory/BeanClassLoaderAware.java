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
 * Callback that allows a bean to be aware of the bean {@link ClassLoader class
 * loader}; that is, the class loader used by the present bean factory to load
 * bean classes.
 *
 * <p>
 * This is mainly intended to be implemented by framework classes which have to
 * pick up application classes by name despite themselves potentially being
 * loaded from a shared class loader.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 * @since 2.1.7 2020-02-21 11:45
 */
public interface BeanClassLoaderAware extends Aware {

  /**
   * Callback that supplies the bean {@link ClassLoader class loader} to
   * a bean instance.
   * <p>Invoked <i>after</i> the population of normal bean properties but
   * <i>before</i> an initialization callback such as
   * {@link InitializingBean InitializingBean's}
   * {@link InitializingBean#afterPropertiesSet()}
   * method or a custom init-method.
   *
   * @param beanClassLoader the owning class loader
   */
  void setBeanClassLoader(ClassLoader beanClassLoader);

}
