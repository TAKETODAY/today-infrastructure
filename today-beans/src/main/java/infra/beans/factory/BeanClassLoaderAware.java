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

package infra.beans.factory;

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
