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

package cn.taketoday.cache.jcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.cache.jcache.interceptor.SimpleExceptionCacheResolver;
import cn.taketoday.cache.support.SimpleCacheManager;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class AbstractJCacheTests {

	protected String cacheName;


	@BeforeEach
	void trackCacheName(TestInfo testInfo) {
		this.cacheName = testInfo.getTestMethod().get().getName();
	}


	protected final CacheManager cacheManager = createSimpleCacheManager("default", "simpleCache");

	protected final CacheResolver defaultCacheResolver = new SimpleCacheResolver(cacheManager);

	protected final CacheResolver defaultExceptionCacheResolver = new SimpleExceptionCacheResolver(cacheManager);

	protected final KeyGenerator defaultKeyGenerator = new SimpleKeyGenerator();

	protected static CacheManager createSimpleCacheManager(String... cacheNames) {
		SimpleCacheManager result = new SimpleCacheManager();
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : cacheNames) {
			caches.add(new ConcurrentMapCache(cacheName));
		}
		result.setCaches(caches);
		result.afterPropertiesSet();
		return result;
	}

}
