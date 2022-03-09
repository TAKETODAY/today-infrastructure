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

package cn.taketoday.cache.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.Caching;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.contextsupport.testfixture.cache.CacheTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Provides various failure scenario linked to the use of {@link Cacheable#sync()}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class CacheSyncFailureTests {

	private ConfigurableApplicationContext context;

	private SimpleService simpleService;

	@BeforeEach
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.simpleService = this.context.getBean(SimpleService.class);
	}

	@AfterEach
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void unlessSync() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.simpleService.unlessSync("key"))
			.withMessageContaining("@Cacheable(sync=true) does not support unless attribute");
	}

	@Test
	public void severalCachesSync() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.simpleService.severalCachesSync("key"))
			.withMessageContaining("@Cacheable(sync=true) only allows a single cache");
	}

	@Test
	public void severalCachesWithResolvedSync() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.simpleService.severalCachesWithResolvedSync("key"))
			.withMessageContaining("@Cacheable(sync=true) only allows a single cache");
	}

	@Test
	public void syncWithAnotherOperation() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.simpleService.syncWithAnotherOperation("key"))
			.withMessageContaining("@Cacheable(sync=true) cannot be combined with other cache operations");
	}

	@Test
	public void syncWithTwoGetOperations() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.simpleService.syncWithTwoGetOperations("key"))
			.withMessageContaining("Only one @Cacheable(sync=true) entry is allowed");
	}


	static class SimpleService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable(cacheNames = "testCache", sync = true, unless = "#result > 10")
		public Object unlessSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheNames = {"testCache", "anotherTestCache"}, sync = true)
		public Object severalCachesSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "testCacheResolver", sync = true)
		public Object severalCachesWithResolvedSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheNames = "testCache", sync = true)
		@CacheEvict(cacheNames = "anotherTestCache", key = "#arg1")
		public Object syncWithAnotherOperation(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Caching(cacheable = {
				@Cacheable(cacheNames = "testCache", sync = true),
				@Cacheable(cacheNames = "anotherTestCache", sync = true)
		})
		public Object syncWithTwoGetOperations(Object arg1) {
			return this.counter.getAndIncrement();
		}
	}

	@Configuration
	@EnableCaching
	static class Config implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache", "anotherTestCache");
		}

		@Bean
		public CacheResolver testCacheResolver() {
			return new NamedCacheResolver(cacheManager(), "testCache", "anotherTestCache");
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}
	}

}
