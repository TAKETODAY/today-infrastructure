/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;

import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CaffeineCache;
import cn.taketoday.cache.CaffeineCacheManager;
import cn.taketoday.cache.NoSuchCacheException;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.lang.Singleton;
import test.demo.config.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author TODAY 2021/4/10 17:11
 * @since 3.0
 */
class CacheableInterceptorTests {

  @Test
  void cacheableAttributes() throws Exception {
    CacheExpressionOperations operations = new CacheExpressionOperations();

    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    CacheableInterceptor interceptor = new CacheableInterceptor(cacheManager);
    interceptor.setExceptionResolver(new DefaultCacheExceptionResolver());

    // cacheName
    Method getUser = CacheUserService.class.getDeclaredMethod("getUser", String.class);
    MethodKey methodKey = new MethodKey(getUser, Cacheable.class);
    CacheConfiguration cacheable = operations.getConfig(methodKey);
    Cache users = interceptor.getCache("users", cacheable);
    assertThat(users)
            .isInstanceOf(CaffeineCache.class);

    assertThat(users.getName())
            .isEqualTo("users");

    // key
    Cache cache = interceptor.obtainCache(getUser, cacheable);
    assertThat(cache).isEqualTo(users);

    CacheConfiguration cacheableClone = new CacheConfiguration();
    cacheableClone.mergeCacheConfigAttributes(cacheable);
    cacheableClone.setCacheName("users1");

    Cache users1 = interceptor.obtainCache(getUser, cacheableClone);
    assertThat(users1).isNotEqualTo(cache).isNotEqualTo(users);

    cacheManager.setDynamicCreation(false);
    cacheableClone.setCacheName("users2");

    try {
      Cache users2 = interceptor.obtainCache(getUser, cacheableClone);
      fail("obtainCache error");
    }
    catch (NoSuchCacheException ignored) {
    }
  }

  @Import({
          CacheUserService.class,
          CacheableInterceptor.class,
          CaffeineCacheManager.class,
          AspectAutoProxyCreator.class,
          DefaultCacheExceptionResolver.class
  })
  @Configuration
  static class AppConfig {

    @Singleton
    public DefaultPointcutAdvisor cacheableAdvisor(CacheableInterceptor interceptor) {
      AnnotationMatchingPointcut matchingPointcut
              = AnnotationMatchingPointcut.forMethodAnnotation(Cacheable.class);
      return new DefaultPointcutAdvisor(matchingPointcut, interceptor);
    }

  }

  @Test
  void testContext() throws Exception {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.register(AppConfig.class);
      context.refresh();
      CacheExpressionOperations operations = new CacheExpressionOperations();
      Method getUser = CacheUserService.class.getDeclaredMethod("getUser", String.class);
      MethodKey methodKey = new MethodKey(getUser, Cacheable.class);
      CacheConfiguration cacheable = operations.getConfig(methodKey);
      CacheableInterceptor interceptor = context.getBean(CacheableInterceptor.class);
      interceptor.setExpressionOperations(operations);
      Cache users = interceptor.getCache("users", cacheable);

      User today = new User(1, "TODAY", 20, "666", "666", "男", new Date());
      CacheUserService userService = context.getBean(CacheUserService.class);
      userService.save(today);

      User user = userService.getUser("666");
      assertThat(today).isEqualTo(user);
      user = userService.getUser("666");
      assertThat(today).isEqualTo(user);
      user = userService.getUser("666");
      assertThat(today).isEqualTo(user);
      user = userService.getUser("666");
      assertThat(today).isEqualTo(user);
      // access time
      assertThat(userService.getAccessTime()).isEqualTo(1);

      Thread.sleep(100);
      user = userService.getUser("666");
      assertThat(today).isEqualTo(user);
      assertThat(userService.getAccessTime()).isEqualTo(2);

      //
      Object by_id_666 = users.get("by_id_666");
      assertThat(today).isEqualTo(user).isEqualTo(by_id_666);
      assertThat(userService.getAccessTime()).isEqualTo(2);

    }

  }
}
