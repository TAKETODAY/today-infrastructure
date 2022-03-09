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

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import cn.taketoday.cache.interceptor.BasicOperation;
import cn.taketoday.cache.interceptor.CacheResolver;

/**
 * Model the base of JSR-107 cache operation through an interface contract.
 *
 * <p>A cache operation can be statically cached as it does not contain any
 * runtime operation of a specific cache invocation.
 *
 * @param <A> the type of the JSR-107 annotation
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface JCacheOperation<A extends Annotation> extends BasicOperation, CacheMethodDetails<A> {

  /**
   * Return the {@link CacheResolver} instance to use to resolve the cache
   * to use for this operation.
   */
  CacheResolver getCacheResolver();

  /**
   * Return the {@link CacheInvocationParameter} instances based on the
   * specified method arguments.
   * <p>The method arguments must match the signature of the related method invocation
   *
   * @param values the parameters value for a particular invocation
   */
  CacheInvocationParameter[] getAllParameters(Object... values);

}
