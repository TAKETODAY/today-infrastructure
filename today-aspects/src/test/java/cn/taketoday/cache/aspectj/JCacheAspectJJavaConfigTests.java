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

import java.util.Arrays;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.config.AnnotatedJCacheableService;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

/**
 * @author Stephane Nicoll
 */
public class JCacheAspectJJavaConfigTests extends AbstractJCacheAnnotationTests {

  @Override
  protected ApplicationContext getApplicationContext() {
    return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  public static class EnableCachingConfig {

    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager cm = new SimpleCacheManager();
      cm.setCaches(Arrays.asList(
              defaultCache(),
              new ConcurrentMapCache("primary"),
              new ConcurrentMapCache("secondary"),
              new ConcurrentMapCache("exception")));
      return cm;
    }

    @Bean
    public AnnotatedJCacheableService cacheableService() {
      return new AnnotatedJCacheableService(defaultCache());
    }

    @Bean
    public Cache defaultCache() {
      return new ConcurrentMapCache("default");
    }
  }

}
