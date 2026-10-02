/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence.support;

import java.util.HashMap;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.persistence.IdGenerator;
import infra.persistence.IllegalEntityException;

/**
 * Resolves generator beans, owning and caching only instances created on demand.
 * Existing beans retain their container-managed scope and lifecycle.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class IdGeneratorResolver implements DisposableBean {

  private final BeanFactory beanFactory;

  private final HashMap<Class<? extends IdGenerator>, IdGenerator> created = new HashMap<>();

  /**
   * Create a resolver backed by the specified bean factory.
   *
   * @param beanFactory the factory used for lookup and dependency injection
   */
  public IdGeneratorResolver(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Resolve by name, or by type with dependency-injected creation as fallback.
   *
   * @param type the generator type, or IdGenerator.class when unspecified
   * @param name the bean name, or an empty string
   * @return the generator
   */
  public synchronized IdGenerator resolve(Class<? extends IdGenerator> type, String name) {
    if (!name.isEmpty()) {
      return beanFactory.getBean(name, type);
    }
    IdGenerator existing = beanFactory.getBeanProvider(type).getIfAvailable();
    if (existing != null) {
      return existing;
    }
    if (type == IdGenerator.class) {
      throw new IllegalEntityException("An ID generator class or bean name is required");
    }
    if (!(beanFactory instanceof AutowireCapableBeanFactory factory)) {
      throw new IllegalEntityException("Creating an ID generator requires an AutowireCapableBeanFactory");
    }
    return created.computeIfAbsent(type, factory::createBean);
  }

  @Override
  public synchronized void destroy() {
    if (beanFactory instanceof AutowireCapableBeanFactory factory) {
      created.values().forEach(factory::destroyBean);
    }
    created.clear();
  }

}
