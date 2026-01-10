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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.beans.BeanUtils;
import infra.beans.factory.BeanClassLoaderAware;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * Simple {@link BeanFactoryPostProcessor} implementation that registers custom
 * {@link Scope Scope(s)} with the containing {@link ConfigurableBeanFactory}.
 *
 * <p>
 * Will register all of the supplied {@link #setScopes(java.util.Map) scopes}
 * with the {@link ConfigurableBeanFactory} that is passed to the
 * {@link #postProcessBeanFactory(ConfigurableBeanFactory)} method.
 *
 * <p>
 * This class allows for <i>declarative</i> registration of custom scopes.
 * Alternatively, consider implementing a custom
 * {@link BeanFactoryPostProcessor} that calls
 * {@link ConfigurableBeanFactory#registerScope} programmatically.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableBeanFactory#registerScope
 * @since 2.1.7 2020-04-02 20:58
 */
public class CustomScopeConfigurer extends OrderedSupport implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

  @Nullable
  private Map<String, Object> scopes;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  /**
   * Specify the custom scopes that are to be registered.
   * <p>
   * The keys indicate the scope names (of type String); each value is expected to
   * be the corresponding custom {@link Scope} instance or class name.
   */
  public void setScopes(Map<String, Object> scopes) {
    this.scopes = scopes;
  }

  /**
   * Add the given scope to this configurer's map of scopes.
   *
   * @param scopeName the name of the scope
   * @param scope the scope implementation
   */
  public void addScope(String scopeName, Scope scope) {
    if (this.scopes == null) {
      this.scopes = new LinkedHashMap<>(1);
    }
    this.scopes.put(scopeName, scope);
  }

  @Override
  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    if (this.scopes != null) {
      for (Map.Entry<String, Object> entry : scopes.entrySet()) {
        String name = entry.getKey();
        Object scope = entry.getValue();
        if (scope instanceof Scope) {
          beanFactory.registerScope(name, (Scope) scope);
        }
        else if (scope instanceof Class) {
          @SuppressWarnings("unchecked")
          Class<Scope> scopeClass = (Class<Scope>) scope;
          Assert.isAssignable(Scope.class, scopeClass, "Invalid scope class");
          beanFactory.registerScope(name, BeanUtils.newInstance(scopeClass));
        }
        else if (scope instanceof String) {
          Class<Scope> scopeClass = ClassUtils.resolveClassName((String) scope, beanClassLoader);
          Assert.isAssignable(Scope.class, scopeClass, "Invalid scope class");
          beanFactory.registerScope(name, BeanUtils.newInstance(scopeClass));
        }
        else {
          throw new IllegalStateException(
                  "Mapped value [" + scope + "] for scope key [" + name
                          + "] is not an instance of required type [" + Scope.class.getName()
                          + "] or a corresponding Class or String value indicating a Scope implementation");
        }
      }
    }
  }

}
