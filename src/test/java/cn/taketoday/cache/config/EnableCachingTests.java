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

package cn.taketoday.cache.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.NamedCacheResolver;
import cn.taketoday.cache.interceptor.SimpleCacheErrorHandler;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.cache.support.NoOpCacheManager;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.contextsupport.testfixture.cache.AbstractCacheAnnotationTests;
import cn.taketoday.contextsupport.testfixture.cache.CacheTestUtils;
import cn.taketoday.contextsupport.testfixture.cache.SomeCustomKeyGenerator;
import cn.taketoday.contextsupport.testfixture.cache.SomeKeyGenerator;
import cn.taketoday.contextsupport.testfixture.cache.beans.AnnotatedClassCacheableService;
import cn.taketoday.contextsupport.testfixture.cache.beans.CacheableService;
import cn.taketoday.contextsupport.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@code @EnableCaching} and its related
 * {@code @Configuration} classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class EnableCachingTests extends AbstractCacheAnnotationTests {

	/** hook into superclass suite of tests */
	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}

	@Test
	public void testKeyStrategy() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertThat(ci.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator", KeyGenerator.class));
	}

	@Test
	public void testCacheErrorHandler() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertThat(ci.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
	}

	@Test
	public void singleCacheManagerBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SingleCacheManagerConfig.class);
		ctx.refresh();
	}

	@Test
	public void multipleCacheManagerBeans() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfig.class);
		try {
			ctx.refresh();
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("no unique bean of type CacheManager")).isTrue();
			assertThat(ex).hasCauseInstanceOf(NoUniqueBeanDefinitionException.class);
		}
	}

	@Test
	public void multipleCacheManagerBeans_implementsCachingConfigurer() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class);
		ctx.refresh();  // does not throw an exception
	}

	@Test
	public void multipleCachingConfigurers() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
		try {
			ctx.refresh();
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("implementations of CachingConfigurer")).isTrue();
		}
	}

	@Test
	public void noCacheManagerBeans() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EmptyConfig.class);
		try {
			ctx.refresh();
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("no bean of type CacheManager")).isTrue();
			assertThat(ex).hasCauseInstanceOf(NoSuchBeanDefinitionException.class);
		}
	}

	@Test
	public void emptyConfigSupport() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isNotNull();
		assertThat(ci.getCacheResolver().getClass()).isEqualTo(SimpleCacheResolver.class);
		assertThat(((SimpleCacheResolver) ci.getCacheResolver()).getCacheManager()).isSameAs(context.getBean(CacheManager.class));
		context.close();
	}

	@Test
	public void bothSetOnlyResolverIsUsed() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FullCachingConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
		assertThat(ci.getKeyGenerator()).isSameAs(context.getBean("keyGenerator"));
		context.close();
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache", "primary", "secondary");
		}

		@Bean
		public CacheableService<?> service() {
			return new DefaultCacheableService();
		}

		@Bean
		public CacheableService<?> classService() {
			return new AnnotatedClassCacheableService();
		}

		@Override
		@Bean
		public KeyGenerator keyGenerator() {
			return new SomeKeyGenerator();
		}

		@Override
		@Bean
		public CacheErrorHandler errorHandler() {
			return new SimpleCacheErrorHandler();
		}

		@Bean
		public KeyGenerator customKeyGenerator() {
			return new SomeCustomKeyGenerator();
		}

		@Bean
		public CacheManager customCacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache");
		}
	}


	@Configuration
	@EnableCaching
	static class EmptyConfig {
	}


	@Configuration
	@EnableCaching
	static class SingleCacheManagerConfig {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfig {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}

		@Bean
		public CacheManager cm2() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfigurer implements CachingConfigurer {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}

		@Bean
		public CacheManager cm2() {
			return new NoOpCacheManager();
		}

		@Override
		public CacheManager cacheManager() {
			return cm1();
		}

		@Override
		public KeyGenerator keyGenerator() {
			return null;
		}
	}


	@Configuration
	@EnableCaching
	static class EmptyConfigSupportConfig implements CachingConfigurer {

		@Bean
		public CacheManager cm() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class FullCachingConfig implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new NoOpCacheManager();
		}

		@Override
		@Bean
		public KeyGenerator keyGenerator() {
			return new SomeKeyGenerator();
		}

		@Override
		@Bean
		public CacheResolver cacheResolver() {
			return new NamedCacheResolver(cacheManager(), "foo");
		}
	}

}
