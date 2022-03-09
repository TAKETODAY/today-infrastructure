/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * A Pointcut that matches if the underlying {@link CacheOperationSource}
 * has an attribute for a given method.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
abstract class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

  protected CacheOperationSourcePointcut() {
    setClassFilter(new CacheOperationSourceClassFilter());
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    CacheOperationSource cas = getCacheOperationSource();
    return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CacheOperationSourcePointcut otherPc)) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
  }

  @Override
  public int hashCode() {
    return CacheOperationSourcePointcut.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + getCacheOperationSource();
  }

  /**
   * Obtain the underlying {@link CacheOperationSource} (may be {@code null}).
   * To be implemented by subclasses.
   */
  @Nullable
  protected abstract CacheOperationSource getCacheOperationSource();

  /**
   * {@link ClassFilter} that delegates to {@link CacheOperationSource#isCandidateClass}
   * for filtering classes whose methods are not worth searching to begin with.
   */
  private class CacheOperationSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      if (CacheManager.class.isAssignableFrom(clazz)) {
        return false;
      }
      CacheOperationSource cas = getCacheOperationSource();
      return (cas == null || cas.isCandidateClass(clazz));
    }
  }

}
