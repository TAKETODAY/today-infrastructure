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
package cn.taketoday.cache.redisson;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;

import cn.taketoday.cache.AbstractCache;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheCallback;
import cn.taketoday.cache.CacheValueRetrievalException;
import cn.taketoday.cache.annotation.CacheConfig;

/**
 * @author TODAY <br>
 *         2019-02-28 18:30
 */
public class RedissonCache extends AbstractCache implements Cache {

    private final CacheConfig cacheConfig;
    private final RMap<Object, Object> cache;

    public RedissonCache(RMap<Object, Object> cache) {
        this(cache, null);
    }

    public RedissonCache(RMap<Object, Object> cache, CacheConfig cacheConfig) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.cacheConfig = cacheConfig;
        setName(cache.getName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected final static void doPut(final RMap<Object, Object> cache, //
                                      final CacheConfig cacheConfig,
                                      final Object key, final Object value)//
    {
        if (cacheConfig != null && cache instanceof RMapCache) {
            final TimeUnit timeUnit = cacheConfig.timeUnit();
            ((RMapCache) cache).fastPut(key, value, cacheConfig.expire(), timeUnit, cacheConfig.maxIdleTime(), timeUnit);
        }
        else {
            cache.fastPut(key, value);
        }
    }

    @Override
    public void evict(final Object key) {
        cache.fastRemove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    protected <T> Object lookupValue(Object key, CacheCallback<T> valueLoader) throws CacheValueRetrievalException {
        final RLock lock = cache.getLock(key);
        try {
            lock.lock();
            Object value = cache.get(key);
            if (value == null) {
                final Object newValue = super.lookupValue(key, valueLoader);
                put(key, newValue);
                return newValue;
            }
            else {
                return value;
            }
        }
        catch (Throwable ex) {
            throw new CacheValueRetrievalException(key, valueLoader, ex);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    protected Object lookupValue(Object key) {
        return cache.get(key);
    }

    @Override
    protected void putInternal(Object key, Object value) {
        doPut(cache, cacheConfig, key, value);
    }

}
