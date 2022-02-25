/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
package cn.taketoday.beans.factory;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

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
 * @author TODAY <br>
 * 2020-04-02 20:58
 * @see ConfigurableBeanFactory#registerScope
 * @since 2.1.7
 */
public class CustomScopeConfigurer
        extends OrderedSupport implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

  private Map<String, Object> scopes;

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
          Class<Scope> scopeClass = ClassUtils.resolveClassName((String) scope, this.beanClassLoader);
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
