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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.jcache.AbstractJCacheTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
public class CacheResolverAdapterTests extends AbstractJCacheTests {

	@Test
	public void resolveSimpleCache() throws Exception {
		DefaultCacheInvocationContext<?> dummyContext = createDummyContext();
		CacheResolverAdapter adapter = new CacheResolverAdapter(getCacheResolver(dummyContext, "testCache"));
		Collection<? extends Cache> caches = adapter.resolveCaches(dummyContext);
		assertThat(caches).isNotNull();
		assertThat(caches.size()).isEqualTo(1);
		assertThat(caches.iterator().next().getName()).isEqualTo("testCache");
	}

	@Test
	public void resolveUnknownCache() throws Exception {
		DefaultCacheInvocationContext<?> dummyContext = createDummyContext();
		CacheResolverAdapter adapter = new CacheResolverAdapter(getCacheResolver(dummyContext, null));

		assertThatIllegalStateException().isThrownBy(() ->
				adapter.resolveCaches(dummyContext));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected CacheResolver getCacheResolver(CacheInvocationContext<? extends Annotation> context, String cacheName) {
		CacheResolver cacheResolver = mock(CacheResolver.class);
		javax.cache.Cache cache;
		if (cacheName == null) {
			cache = null;
		}
		else {
			cache = mock(javax.cache.Cache.class);
			given(cache.getName()).willReturn(cacheName);
		}
		given(cacheResolver.resolveCache(context)).willReturn(cache);
		return cacheResolver;
	}

	protected DefaultCacheInvocationContext<?> createDummyContext() throws Exception {
		Method method = Sample.class.getMethod("get", String.class);
		CacheResult cacheAnnotation = method.getAnnotation(CacheResult.class);
		CacheMethodDetails<CacheResult> methodDetails =
				new DefaultCacheMethodDetails<>(method, cacheAnnotation, "test");
		CacheResultOperation operation = new CacheResultOperation(methodDetails,
				defaultCacheResolver, defaultKeyGenerator, defaultExceptionCacheResolver);
		return new DefaultCacheInvocationContext<>(operation, new Sample(), new Object[] {"id"});
	}


	static class Sample {

		@CacheResult
		public Object get(String id) {
			return null;
		}
	}

}
