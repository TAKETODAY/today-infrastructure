/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.cache.annotation.CacheConfig;

/**
 * @author TODAY <br>
 *         2019-11-03 19:45
 */
public class DefaultCacheManager implements CacheManager {

    private boolean dynamic = true;

    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>(32);

    public DefaultCacheManager() {

    }

    public DefaultCacheManager(String... cacheNames) {
        Objects.requireNonNull(cacheNames, "cacheNames s can't be null");
        setCacheNames(Arrays.asList(cacheNames));
    }

    /**
     * Specify the set of cache names for this CacheManager's 'static' mode.
     * <p>
     * The number of caches and their names will be fixed after a call to this
     * method, with no creation of further cache regions at runtime.
     * <p>
     * Calling this with a {@code null} collection argument resets the mode to
     * 'dynamic', allowing for further creation of caches again.
     */
    public void setCacheNames(Collection<String> cacheNames) {
        if (cacheNames != null) {
            for (String name : cacheNames) {
                this.cacheMap.put(name, createCache(name));
            }
            this.dynamic = false;
        }
        else {
            this.dynamic = true;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }

    @Override
    public Cache getCache(String name, CacheConfig cacheConfig) {
        Cache cache = this.cacheMap.get(name);
        if (cache == null && this.dynamic) {
            synchronized (cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    cache = createCache(name);
                    this.cacheMap.put(name, cache);
                }
            }
        }
        return cache;
    }

    /**
     * Create a new DefaultMapCache instance for the specified cache name.
     * 
     * @param name
     *            the name of the cache
     * @return the DefaultMapCache
     */
    protected Cache createCache(String name) {
        return new ConcurrentMapCache(name);
    }
}
