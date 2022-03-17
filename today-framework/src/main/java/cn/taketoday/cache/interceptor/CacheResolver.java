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

import java.util.Collection;

import cn.taketoday.cache.Cache;

/**
 * Determine the {@link Cache} instance(s) to use for an intercepted method invocation.
 *
 * <p>Implementations must be thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@FunctionalInterface
public interface CacheResolver {

  /**
   * Return the cache(s) to use for the specified invocation.
   *
   * @param context the context of the particular invocation
   * @return the cache(s) to use (never {@code null})
   * @throws IllegalStateException if cache resolution failed
   */
  Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context);

}
