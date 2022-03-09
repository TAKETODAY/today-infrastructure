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

package cn.taketoday.cache.jcache.interceptor;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import cn.taketoday.beans.factory.annotation.Value;

/**
 * @author Stephane Nicoll
 */
class SampleObject {

	// Simple

	@CacheResult(cacheName = "simpleCache")
	public SampleObject simpleGet(Long id) {
		return null;
	}

	@CachePut(cacheName = "simpleCache")
	public void simplePut(Long id, @CacheValue SampleObject instance) {
	}

	@CacheRemove(cacheName = "simpleCache")
	public void simpleRemove(Long id) {
	}

	@CacheRemoveAll(cacheName = "simpleCache")
	public void simpleRemoveAll() {
	}

	@CacheResult(cacheName = "testSimple")
	public SampleObject anotherSimpleGet(String foo, Long bar) {
		return null;
	}

	// @CacheKey

	@CacheResult
	public SampleObject multiKeysGet(@CacheKey Long id, Boolean notUsed,
			@CacheKey String domain) {
		return null;
	}

	// @CacheValue

	@CachePut(cacheName = "simpleCache")
	public void noCacheValue(Long id) {
	}

	@CachePut(cacheName = "simpleCache")
	public void multiCacheValues(Long id, @CacheValue SampleObject instance,
			@CacheValue SampleObject anotherInstance) {
	}

	// Parameter annotation

	@CacheResult(cacheName = "simpleCache")
	public SampleObject annotatedGet(@CacheKey Long id, @Value("${foo}") String foo) {
		return null;
	}

	// Full config

	@CacheResult(cacheName = "simpleCache", skipGet = true,
			cachedExceptions = Exception.class, nonCachedExceptions = RuntimeException.class)
	public SampleObject fullGetConfig(@CacheKey Long id) {
		return null;
	}

	@CachePut(cacheName = "simpleCache", afterInvocation = false,
			cacheFor = Exception.class, noCacheFor = RuntimeException.class)
	public void fullPutConfig(@CacheKey Long id, @CacheValue SampleObject instance) {
	}

}
