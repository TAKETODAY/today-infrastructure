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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

import cn.taketoday.cache.Cache;

/**
 * Class describing the root object used during the expression evaluation.
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CacheExpressionRootObject {

  private final Collection<? extends Cache> caches;

  private final Method method;

  private final Object[] args;

  private final Object target;

  private final Class<?> targetClass;

  public CacheExpressionRootObject(Collection<? extends Cache> caches,
          Method method, Object[] args, Object target, Class<?> targetClass) {

    this.method = method;
    this.target = target;
    this.targetClass = targetClass;
    this.args = args;
    this.caches = caches;
  }

  public Collection<? extends Cache> getCaches() {
    return this.caches;
  }

  public Method getMethod() {
    return this.method;
  }

  public String getMethodName() {
    return this.method.getName();
  }

  public Object[] getArgs() {
    return this.args;
  }

  public Object getTarget() {
    return this.target;
  }

  public Class<?> getTargetClass() {
    return this.targetClass;
  }

}
