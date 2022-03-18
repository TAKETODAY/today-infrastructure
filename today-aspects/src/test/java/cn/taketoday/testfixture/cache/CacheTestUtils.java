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

package cn.taketoday.testfixture.cache;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.support.SimpleCacheManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * General cache-related test utilities.
 *
 * @author Stephane Nicoll
 */
public class CacheTestUtils {

	/**
	 * Create a {@link SimpleCacheManager} with the specified cache(s).
	 * @param cacheNames the names of the caches to create
	 */
	public static CacheManager createSimpleCacheManager(String... cacheNames) {
		SimpleCacheManager result = new SimpleCacheManager();
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : cacheNames) {
			caches.add(new ConcurrentMapCache(cacheName));
		}
		result.setCaches(caches);
		result.afterPropertiesSet();
		return result;
	}


	/**
	 * Assert the following key is not held within the specified cache(s).
	 */
	public static void assertCacheMiss(Object key, Cache... caches) {
		for (Cache cache : caches) {
			assertThat(cache.get(key)).as("No entry in " + cache + " should have been found with key " + key).isNull();
		}
	}

	/**
	 * Assert the following key has a matching value within the specified cache(s).
	 */
	public static void assertCacheHit(Object key, Object value, Cache... caches) {
		for (Cache cache : caches) {
			Cache.ValueWrapper wrapper = cache.get(key);
			assertThat(wrapper).as("An entry in " + cache + " should have been found with key " + key).isNotNull();
			assertThat(wrapper.get()).as("Wrong value in " + cache + " for entry with key " + key).isEqualTo(value);
		}
	}

}
