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

package cn.taketoday.cache.interceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * A Pointcut that matches if the underlying {@link CacheOperationSource}
 * has an attribute for a given method.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private CacheOperationSource cacheOperationSource;

  public CacheOperationSourcePointcut() {
    setClassFilter(new CacheOperationSourceClassFilter());
  }

  public void setCacheOperationSource(@Nullable CacheOperationSource cacheOperationSource) {
    this.cacheOperationSource = cacheOperationSource;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return (this.cacheOperationSource == null ||
            this.cacheOperationSource.hasCacheOperations(method, targetClass));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof CacheOperationSourcePointcut that &&
            ObjectUtils.nullSafeEquals(this.cacheOperationSource, that.cacheOperationSource)));
  }

  @Override
  public int hashCode() {
    return CacheOperationSourcePointcut.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.cacheOperationSource;
  }

  /**
   * {@link ClassFilter} that delegates to {@link CacheOperationSource#isCandidateClass}
   * for filtering classes whose methods are not worth searching to begin with.
   */
  private final class CacheOperationSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      if (CacheManager.class.isAssignableFrom(clazz)) {
        return false;
      }
      return (cacheOperationSource == null || cacheOperationSource.isCandidateClass(clazz));
    }

    @Nullable
    private CacheOperationSource getCacheOperationSource() {
      return cacheOperationSource;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof CacheOperationSourceClassFilter that &&
              ObjectUtils.nullSafeEquals(getCacheOperationSource(), that.getCacheOperationSource())));
    }

    @Override
    public int hashCode() {
      return CacheOperationSourceClassFilter.class.hashCode();
    }

    @Override
    public String toString() {
      return CacheOperationSourceClassFilter.class.getName() + ": " + getCacheOperationSource();
    }
  }

}
