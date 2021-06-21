/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.function.UnaryOperator;

import cn.taketoday.context.utils.Assert;

/**
 * @author TODAY 2020-08-15 19:50
 * @since 3.0
 */
public class CaffeineCache extends AbstractMappingFunctionCache {

  private final Cache<Object, Object> caffeine;

  /**
   * Create a {@link CaffeineCache} instance with the specified name and the given
   * internal {@link Cache} to use.
   *
   * @param name
   *         the name of the cache
   * @param caffeine
   *         Caffeine Cache instance
   */
  public CaffeineCache(String name, Cache<Object, Object> caffeine) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(caffeine, "com.github.benmanes.caffeine.cache.Cache must not be null");
    this.caffeine = caffeine;
    setName(name);
  }

  @Override
  public void evict(Object key) {
    this.caffeine.invalidate(key);
  }

  @Override
  public void clear() {
    this.caffeine.invalidateAll();
  }

  @Override
  protected Object getInternal(Object key, UnaryOperator<Object> mappingFunction) {
    return this.caffeine.get(key, mappingFunction);
  }

  @Override
  protected Object lookupValue(Object key) {
    if (this.caffeine instanceof LoadingCache) {
      return ((LoadingCache<Object, Object>) this.caffeine).get(key);
    }
    return this.caffeine.getIfPresent(key);
  }

  @Override
  protected void putInternal(Object key, Object value) {
    this.caffeine.put(key, value);
  }

}
