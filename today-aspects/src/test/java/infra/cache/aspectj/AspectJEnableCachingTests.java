/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.cache.aspectj;

import infra.cache.CacheManager;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.cache.config.AnnotatedClassCacheableService;
import infra.cache.config.CacheableService;
import infra.cache.config.DefaultCacheableService;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AdviceMode;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.CacheTestUtils;
import infra.context.testfixture.cache.SomeCustomKeyGenerator;
import infra.context.testfixture.cache.SomeKeyGenerator;

/**
 * @author Stephane Nicoll
 */
public class AspectJEnableCachingTests extends AbstractCacheAnnotationTests {

  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
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

}
