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
package cn.taketoday.cache;

import java.util.Collection;

import cn.taketoday.lang.Nullable;

/**
 * Framework's central cache manager SPI.
 *
 * <p>Allows for retrieving named {@link Cache} regions.
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @author TODAY
 * @since 2019-01-02 22:44
 */
public interface CacheManager {

  /**
   * Get the cache associated with the given name.
   * <p>Note that the cache may be lazily created at runtime if the
   * native provider supports it.
   *
   * @param name the cache identifier (must not be {@code null})
   * @return the associated cache, or {@code null} if such a cache
   * does not exist or could be not created
   */
  @Nullable
  Cache getCache(String name);

  /**
   * Get a collection of the cache names known by this manager.
   *
   * @return the names of all caches known by the cache manager
   */
  Collection<String> getCacheNames();

}
