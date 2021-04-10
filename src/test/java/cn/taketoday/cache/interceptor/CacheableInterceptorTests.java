/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.CaffeineCache;
import cn.taketoday.cache.CaffeineCacheManager;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.interceptor.AbstractCacheInterceptor.MethodKey;
import cn.taketoday.context.StandardApplicationContext;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareAnnotation;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/10 17:11
 * @since 3.0
 */
public class CacheableInterceptorTests {
  CacheManager cacheManager = new CaffeineCacheManager();
  CacheableInterceptor interceptor = new CacheableInterceptor(cacheManager);

  {
    interceptor.setExceptionResolver(new DefaultCacheExceptionResolver());
  }

  @CacheConfig(cacheName = "users", timeUnit = TimeUnit.SECONDS, expire = 3)
  static class UserService {
    DefaultUserRepository userDao = new DefaultUserRepository();

    {
      userDao.initData();
    }

    @Cacheable(key = "by_id_${id}")
    public User getUser(String id) {
      System.err.println("get-user:" + id);
      return userDao.findUser(id);
    }

  }

  @Test
  public void cacheName() throws Exception {
    final Method getUser = UserService.class.getDeclaredMethod("getUser", String.class);
    final MethodKey methodKey = new MethodKey(getUser, Cacheable.class);
    final CacheConfiguration cacheable = prepareAnnotation(methodKey);
    final Cache users = interceptor.getCache("users", cacheable);

    assertThat(users)
            .isInstanceOf(CaffeineCache.class);

    assertThat(users.getName())
            .isEqualTo("users");

    System.out.println(users);
  }

//  @Aspect
//  @MissingBean
//  @Advice(Cacheable.class)
//  CacheableInterceptor cacheableInterceptor(CacheManager cacheManager) {
//    return new CacheableInterceptor(cacheManager);
//  }

  @Test
  public void testContext() {
    try(StandardApplicationContext context = new StandardApplicationContext()){
      context.importBeans(UserService.class);
//      context.importBeans(CacheableInterceptor.class);
//      context.importBeans(CaffeineCacheManager.class);
      context.importBeans(AspectAutoProxyCreator.class);
      context.importBeans(DefaultCacheExceptionResolver.class);

      final AnnotationMatchingPointcut matchingPointcut
              = AnnotationMatchingPointcut.forMethodAnnotation(Cacheable.class);

      final DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, interceptor);
      context.registerBean(pointcutAdvisor);

      context.load(Collections.emptySet());

      System.out.println(context);

      final UserService userService = context.getBean(UserService.class);

      User user = userService.getUser("666");
      user = userService.getUser("666");
      user = userService.getUser("666");
      user = userService.getUser("666");

    }

  }

}
