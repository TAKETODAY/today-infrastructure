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
import java.util.List;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Nullable;

/**
 * A {@link CacheResolver} that forces the resolution to a configurable
 * collection of name(s) against a given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class NamedCacheResolver extends AbstractCacheResolver {

  @Nullable
  private Collection<String> cacheNames;

  public NamedCacheResolver() { }

  public NamedCacheResolver(CacheManager cacheManager, String... cacheNames) {
    super(cacheManager);
    this.cacheNames = List.of(cacheNames);
  }

  /**
   * Set the cache name(s) that this resolver should use.
   */
  public void setCacheNames(Collection<String> cacheNames) {
    this.cacheNames = cacheNames;
  }

  @Override
  protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
    return this.cacheNames;
  }

}
