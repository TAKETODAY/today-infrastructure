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
package cn.taketoday.cache.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.lang.Nullable;

/**
 * Composite {@link CacheManager} implementation that iterates over
 * a given collection of delegate {@link CacheManager} instances.
 *
 * <p>Allows {@link NoOpCacheManager} to be automatically added to the end of
 * the list for handling cache declarations without a backing store. Otherwise,
 * any custom {@link CacheManager} may play that role of the last delegate as
 * well, lazily creating cache regions for any requested name.
 *
 * <p>Note: Regular CacheManagers that this composite manager delegates to need
 * to return {@code null} from {@link #getCache(String)} if they are unaware of
 * the specified cache name, allowing for iteration to the next delegate in line.
 * However, most {@link CacheManager} implementations fall back to lazy creation
 * of named caches once requested; check out the specific configuration details
 * for a 'static' mode with fixed cache names, if available.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author TODAY <br>
 * @see #setFallbackToNoOpCache
 * @see ConcurrentMapCacheManager#setCacheNames
 * @since 2019-02-28 16:38
 */
public class CompositeCacheManager implements CacheManager, InitializingBean {

  private final List<CacheManager> cacheManagers = new ArrayList<>();

  private boolean fallbackToNoOpCache = false;

  /**
   * Construct an empty CompositeCacheManager, with delegate CacheManagers to
   * be added via the {@link #setCacheManagers "cacheManagers"} property.
   */
  public CompositeCacheManager() {
  }

  /**
   * Construct a CompositeCacheManager from the given delegate CacheManagers.
   *
   * @param cacheManagers the CacheManagers to delegate to
   */
  public CompositeCacheManager(CacheManager... cacheManagers) {
    setCacheManagers(Arrays.asList(cacheManagers));
  }

  /**
   * Specify the CacheManagers to delegate to.
   */
  public void setCacheManagers(Collection<CacheManager> cacheManagers) {
    this.cacheManagers.addAll(cacheManagers);
  }

  /**
   * Indicate whether a {@link NoOpCacheManager} should be added at the end of the delegate list.
   * In this case, any {@code getCache} requests not handled by the configured CacheManagers will
   * be automatically handled by the {@link NoOpCacheManager} (and hence never return {@code null}).
   */
  public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
    this.fallbackToNoOpCache = fallbackToNoOpCache;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.fallbackToNoOpCache) {
      this.cacheManagers.add(new NoOpCacheManager());
    }
  }

  @Override
  @Nullable
  public Cache getCache(String name) {
    for (CacheManager cacheManager : this.cacheManagers) {
      Cache cache = cacheManager.getCache(name);
      if (cache != null) {
        return cache;
      }
    }
    return null;
  }

  @Override
  public Collection<String> getCacheNames() {
    Set<String> names = new LinkedHashSet<>();
    for (CacheManager manager : this.cacheManagers) {
      names.addAll(manager.getCacheNames());
    }
    return Collections.unmodifiableSet(names);
  }

}
