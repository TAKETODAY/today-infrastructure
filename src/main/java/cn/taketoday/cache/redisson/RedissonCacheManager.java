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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache.redisson;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.context.annotation.Autowired;

/**
 * A {@link CacheManager} implementation backed by Redisson instance.
 *
 * @author TODAY <br>
 *         2018-12-24 19:06
 */
public class RedissonCacheManager implements CacheManager {

    private Codec codec;
    private boolean dynamic = true;
    private final RedissonClient redisson;

    private Map<String, CacheConfig> configMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cache> instanceMap = new ConcurrentHashMap<>();

    @Autowired
    public RedissonCacheManager(
            @Autowired(required = false) Codec codec,
            @Autowired(required = true) RedissonClient redisson) //
    {
        this.codec = codec;
        this.redisson = redisson;
    }

    /**
     * Defines 'fixed' cache names. A new cache instance will not be created in
     * dynamic for non-defined names.
     * <p>
     * `null` parameter setups dynamic mode
     * 
     * @param names
     *            of caches
     */
    public void setCacheNames(Collection<String> names) {
        if (names != null) {
            for (String name : names) {
                getCache(name);
            }
            dynamic = false;
        }
        else {
            dynamic = true;
        }
    }

    /**
     * Set cache config mapped by cache name
     *
     * @param config
     *            object
     */
    @SuppressWarnings("unchecked")
    public void setConfig(Map<String, ? extends CacheConfig> config) {
        this.configMap = (Map<String, CacheConfig>) config;
    }

    /**
     * Set Codec instance shared between all Cache instances
     *
     * @param codec
     *            object
     */
    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    @Override
    public Cache getCache(String name) {

        Cache cache = instanceMap.get(name);
        if (cache != null) {
            return cache;
        }
        if (!dynamic) {
            return cache;
        }

        final CacheConfig config = configMap.get(name); // if exist in config map
        if (isDefault(config)) {
            return createMap(name);
        }
        return createMapCache(name, config);
    }

    @Override
    public Cache getCache(String name, CacheConfig cacheConfig) {

        Cache cache = instanceMap.get(name);
        if (cache != null) {
            return cache;
        }
        if (!dynamic) {
            return cache;
        }

        if (isDefault(cacheConfig)) {
            return createMap(name);
        }
        return createMapCache(name, cacheConfig);
    }

    private final static boolean isDefault(CacheConfig cacheConfig) {
        if (cacheConfig == null || cacheConfig == CacheConfig.EMPTY_CACHE_CONFIG) {
            return true;
        }
        return cacheConfig.maxIdleTime() == 0 && cacheConfig.expire() == 0 && cacheConfig.maxSize() == 0;
    }

    private Cache createMap(String name) {

        Cache cache = new RedissonCache(getMap(name), null);
        Cache oldCache = instanceMap.putIfAbsent(name, cache);
        if (oldCache != null) {
            return oldCache;
        }
        return cache;
    }

    protected RMap<Object, Object> getMap(String name) {
        if (codec != null) {
            return redisson.getMap(name, codec);
        }
        return redisson.getMap(name);
    }

    /**
     * Not a default config
     * 
     * @param name
     *            the name of cache
     * @param config
     *            config instance
     */
    private Cache createMapCache(String name, CacheConfig config) {

        RMapCache<Object, Object> map = getMapCache(name);

        Cache cache = new RedissonCache(map, config);
        Cache oldCache = instanceMap.putIfAbsent(name, cache);
        if (oldCache != null) {
            cache = oldCache;
        }
        else {
            map.setMaxSize(config.maxSize());
        }
        return cache;
    }

    protected RMapCache<Object, Object> getMapCache(String name) {
        if (codec != null) {
            return redisson.getMapCache(name, codec);
        }
        return redisson.getMapCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(configMap.keySet());
    }

}
