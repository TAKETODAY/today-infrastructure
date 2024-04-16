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

package cn.taketoday.jdbc.config;

import java.sql.Wrapper;

import javax.sql.DataSource;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.jdbc.datasource.DelegatingDataSource;
import cn.taketoday.lang.Nullable;

/**
 * Unwraps a {@link DataSource} that may have been proxied or wrapped in a custom
 * {@link Wrapper} such as {@link DelegatingDataSource}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class DataSourceUnwrapper {

  /**
   * Return an object that implements the given {@code target} type, unwrapping delegate
   * or proxy if necessary using the specified {@code unwrapInterface}.
   *
   * @param dataSource the datasource to handle
   * @param unwrapInterface the interface that the target type must implement
   * @param target the type that the result must implement
   * @param <I> the interface that the target type must implement
   * @param <T> the target type
   * @return an object that implements the target type or {@code null}
   * @see Wrapper#unwrap(Class)
   */
  @Nullable
  public static <I, T extends I> T unwrap(DataSource dataSource, Class<I> unwrapInterface, Class<T> target) {
    if (target.isInstance(dataSource)) {
      return target.cast(dataSource);
    }
    I unwrapped = safeUnwrap(dataSource, unwrapInterface);
    if (unwrapped != null && unwrapInterface.isAssignableFrom(target)) {
      return target.cast(unwrapped);
    }

    DataSource targetDataSource = getTargetDataSource(dataSource);
    if (targetDataSource != null) {
      return unwrap(targetDataSource, unwrapInterface, target);
    }
    if (AopUtils.isAopProxy(dataSource)) {
      Object proxyTarget = AopProxyUtils.getSingletonTarget(dataSource);
      if (proxyTarget instanceof DataSource proxyDataSource) {
        return unwrap(proxyDataSource, unwrapInterface, target);
      }
    }
    return null;
  }

  /**
   * Return an object that implements the given {@code target} type, unwrapping delegate
   * or proxy if necessary. Consider using {@link #unwrap(DataSource, Class, Class)} as
   * {@link Wrapper#unwrap(Class) unwrapping} won't be considered if {@code target} is
   * not an interface.
   *
   * @param dataSource the datasource to handle
   * @param target the type that the result must implement
   * @param <T> the target type
   * @return an object that implements the target type or {@code null}
   */
  @Nullable
  public static <T> T unwrap(DataSource dataSource, Class<T> target) {
    return unwrap(dataSource, target, target);
  }

  @Nullable
  private static <S> S safeUnwrap(Wrapper wrapper, Class<S> target) {
    try {
      if (target.isInterface() && wrapper.isWrapperFor(target)) {
        return wrapper.unwrap(target);
      }
    }
    catch (Exception ex) {
      // Continue
    }
    return null;
  }

  @Nullable
  private static DataSource getTargetDataSource(DataSource dataSource) {
    if (dataSource instanceof DelegatingDataSource delegatingDataSource) {
      return delegatingDataSource.getTargetDataSource();
    }
    return null;
  }

}
