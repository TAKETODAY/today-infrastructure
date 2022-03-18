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

package cn.taketoday.cache.aspectj;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.config.AnnotatedClassCacheableService;
import cn.taketoday.cache.config.CacheableService;
import cn.taketoday.cache.config.DefaultCacheableService;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleCacheErrorHandler;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.testfixture.cache.CacheTestUtils;
import cn.taketoday.testfixture.cache.SomeCustomKeyGenerator;
import cn.taketoday.testfixture.cache.SomeKeyGenerator;

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
