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

package cn.taketoday.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * A Pointcut that matches if the underlying {@link JCacheOperationSource}
 * has an operation for a given method.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class JCacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    JCacheOperationSource cas = getCacheOperationSource();
    return (cas != null && cas.getCacheOperation(method, targetClass) != null);
  }

  /**
   * Obtain the underlying {@link JCacheOperationSource} (may be {@code null}).
   * To be implemented by subclasses.
   */
  @Nullable
  protected abstract JCacheOperationSource getCacheOperationSource();

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JCacheOperationSourcePointcut otherPc)) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
  }

  @Override
  public int hashCode() {
    return JCacheOperationSourcePointcut.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + getCacheOperationSource();
  }

}
