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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import cn.taketoday.cache.support.NoOpCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoOpCacheManager}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 */
public class NoOpCacheManagerTests {

	private final CacheManager manager = new NoOpCacheManager();

	@Test
	public void testGetCache() throws Exception {
		Cache cache = this.manager.getCache("bucket");
		assertThat(cache).isNotNull();
		assertThat(this.manager.getCache("bucket")).isSameAs(cache);
	}

	@Test
	public void testNoOpCache() throws Exception {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		assertThat(cache.getName()).isEqualTo(name);
		Object key = new Object();
		cache.put(key, new Object());
		assertThat(cache.get(key)).isNull();
		assertThat(cache.get(key, Object.class)).isNull();
		assertThat(cache.getNativeCache()).isSameAs(cache);
	}

	@Test
	public void testCacheName() throws Exception {
		String name = "bucket";
		assertThat(this.manager.getCacheNames().contains(name)).isFalse();
		this.manager.getCache(name);
		assertThat(this.manager.getCacheNames().contains(name)).isTrue();
	}

	@Test
	public void testCacheCallable() throws Exception {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		Object returnValue = new Object();
		Object value = cache.get(new Object(), () -> returnValue);
		assertThat(value).isEqualTo(returnValue);
	}

	@Test
	public void testCacheGetCallableFail() {
		Cache cache = this.manager.getCache(createRandomKey());
		String key = createRandomKey();
		try {
			cache.get(key, () -> {
				throw new UnsupportedOperationException("Expected exception");
			});
		}
		catch (Cache.ValueRetrievalException ex) {
			assertThat(ex.getCause()).isNotNull();
			assertThat(ex.getCause().getClass()).isEqualTo(UnsupportedOperationException.class);
		}
	}

	private String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}
