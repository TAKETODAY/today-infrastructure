/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache;

import java.util.Collection;

import cn.taketoday.cache.annotation.CacheConfig;

/**
 * @author TODAY <br>
 * 2019-01-02 22:44
 */
public interface CacheManager {

  /**
   * Use default {@link CacheConfig#EMPTY_CACHE_CONFIG}
   *
   * @param name
   *         the cache identifier (must not be {@code null})
   *
   * @return Target {@link Cache}
   *
   * @see CacheConfig#EMPTY_CACHE_CONFIG
   */
  default Cache getCache(String name) {
    return getCache(name, CacheConfig.EMPTY_CACHE_CONFIG);
  }

  /**
   * Return the cache associated with the given name.
   *
   * @param name
   *         the cache identifier (must not be {@code null})
   * @param cacheConfig
   *         {@link CacheConfig}
   *
   * @return Target {@link Cache}
   */
  Cache getCache(String name, CacheConfig cacheConfig);

  /**
   * Return a collection of the cache names known by this manager.
   *
   * @return the names of all caches known by the cache manager
   */
  Collection<String> getCacheNames();

}
