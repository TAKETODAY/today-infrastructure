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

package cn.taketoday.aop.aspectj;

import org.aspectj.weaver.tools.ShadowMatch;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.ExpressionPointcut;
import cn.taketoday.lang.Nullable;

/**
 * Internal {@link ShadowMatch} utilities.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public abstract class ShadowMatchUtils {

  private static final Map<Key, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(256);

  /**
   * Clear the cache of computed {@link ShadowMatch} instances.
   */
  public static void clearCache() {
    shadowMatchCache.clear();
  }

  /**
   * Return the {@link ShadowMatch} for the specified {@link ExpressionPointcut}
   * and {@link Method} or {@code null} if none is found.
   *
   * @param expression the expression
   * @param method the method
   * @return the {@code ShadowMatch} to use for the specified expression and method
   */
  @Nullable
  static ShadowMatch getShadowMatch(ExpressionPointcut expression, Method method) {
    return shadowMatchCache.get(new Key(expression, method));
  }

  /**
   * Associate the {@link ShadowMatch} to the specified {@link ExpressionPointcut}
   * and method. If an entry already exists, the given {@code shadowMatch} is
   * ignored.
   *
   * @param expression the expression
   * @param method the method
   * @param shadowMatch the shadow match to use for this expression and method
   * if none already exists
   * @return the shadow match to use for the specified expression and method
   */
  static ShadowMatch setShadowMatch(ExpressionPointcut expression, Method method, ShadowMatch shadowMatch) {
    ShadowMatch existing = shadowMatchCache.putIfAbsent(new Key(expression, method), shadowMatch);
    return (existing != null ? existing : shadowMatch);
  }

  private record Key(ExpressionPointcut expression, Method method) { }

}
