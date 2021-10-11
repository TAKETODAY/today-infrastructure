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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY <br>
 * 2019-02-28 16:38
 */
public class CompositeCacheManager implements CacheManager {

  private final ArrayList<CacheManager> cacheManagers = new ArrayList<>();

  public CompositeCacheManager() { }

  public CompositeCacheManager(CacheManager... cacheManagers) {
    Assert.notNull(cacheManagers, "cacheManager s can't be null");
    Collections.addAll(this.cacheManagers, cacheManagers);
  }

  public void addCacheManagers(Collection<CacheManager> cacheManagers) {
    Assert.notNull(cacheManagers, "cacheManager s can't be null");
    this.cacheManagers.addAll(cacheManagers);
  }

  public void setCacheManagers(Collection<CacheManager> cacheManagers) {
    Assert.notNull(cacheManagers, "cacheManager s can't be null");
    this.cacheManagers.clear();
    this.cacheManagers.addAll(cacheManagers);
  }

  @Override
  public Cache getCache(String name, CacheConfig cacheConfig) {
    for (final CacheManager cacheManager : this.cacheManagers) {
      final Cache cache = cacheManager.getCache(name, cacheConfig);
      if (cache != null) {
        return cache;
      }
    }
    return null;
  }

  @Override
  public Collection<String> getCacheNames() {
    if (CollectionUtils.isEmpty(cacheManagers)) {
      return Collections.emptySet();
    }
    final Set<String> names = new LinkedHashSet<>();
    for (CacheManager manager : this.cacheManagers) {
      names.addAll(manager.getCacheNames());
    }
    return names;
  }

}
