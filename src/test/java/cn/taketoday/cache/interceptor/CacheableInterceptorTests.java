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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CaffeineCache;
import cn.taketoday.cache.CaffeineCacheManager;
import cn.taketoday.cache.NoSuchCacheException;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.interceptor.AbstractCacheInterceptor.MethodKey;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.loader.CandidateComponentScanner;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareAnnotation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author TODAY 2021/4/10 17:11
 * @since 3.0
 */
public class CacheableInterceptorTests {
  CaffeineCacheManager cacheManager = new CaffeineCacheManager();
  CacheableInterceptor interceptor = new CacheableInterceptor(cacheManager);

  {
    interceptor.setExceptionResolver(new DefaultCacheExceptionResolver());
  }

  @CacheConfig(cacheName = "users", timeUnit = TimeUnit.MILLISECONDS, expire = 60)
  static class UserService {
    DefaultUserRepository userDao = new DefaultUserRepository();

    int accessTime = 0;

    public void save(User user) {
      userDao.save(user);
    }

    @Cacheable(key = "by_id_${id}")
    public User getUser(String id) {
      accessTime++;
      System.out.println(id);
      return userDao.findUser(id);
    }

    public int getAccessTime() {
      return accessTime;
    }
  }

  @Test
  public void cacheableAttributes() throws Exception {
    // cacheName
    final Method getUser = UserService.class.getDeclaredMethod("getUser", String.class);
    final MethodKey methodKey = new MethodKey(getUser, Cacheable.class);
    final CacheConfiguration cacheable = prepareAnnotation(methodKey);
    final Cache users = interceptor.getCache("users", cacheable);
    assertThat(users)
            .isInstanceOf(CaffeineCache.class);

    assertThat(users.getName())
            .isEqualTo("users");

    // key
    final Cache cache = interceptor.obtainCache(getUser, cacheable);
    assertThat(cache).isEqualTo(users);

    CacheConfiguration cacheableClone = new CacheConfiguration();
    cacheableClone.mergeCacheConfigAttributes(cacheable);
    cacheableClone.setCacheName("users1");

    final Cache users1 = interceptor.obtainCache(getUser, cacheableClone);
    assertThat(users1).isNotEqualTo(cache).isNotEqualTo(users);

    cacheManager.setDynamicCreation(false);

    cacheableClone.setCacheName("users2");

    try {
      final Cache users2 = interceptor.obtainCache(getUser, cacheableClone);
      fail("obtainCache error");
    }
    catch (NoSuchCacheException ignored) { }
  }

  @Test
  public void testContext() throws Exception {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.importBeans(UserService.class);
      context.importBeans(CacheableInterceptor.class);
      context.importBeans(CaffeineCacheManager.class);
      context.importBeans(AspectAutoProxyCreator.class);
      context.importBeans(DefaultCacheExceptionResolver.class);
      context.registerFrameworkBeans();

      final CacheableInterceptor interceptor = context.getBean(CacheableInterceptor.class);

      final Method getUser = UserService.class.getDeclaredMethod("getUser", String.class);
      final MethodKey methodKey = new MethodKey(getUser, Cacheable.class);
      final CacheConfiguration cacheable = prepareAnnotation(methodKey);
      final Cache users = interceptor.getCache("users", cacheable);

      final AnnotationMatchingPointcut matchingPointcut
              = AnnotationMatchingPointcut.forMethodAnnotation(Cacheable.class);
      final DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, interceptor);
      context.registerBean(pointcutAdvisor);

      final CandidateComponentScanner scanner = context.getCandidateComponentScanner();
      final Set<Class<?>> candidates = scanner.getCandidates();
      context.load(new HashSet<>());
      scanner.setCandidates(candidates);

      final User today = new User(1, "TODAY", 20, "666", "666", "男", new Date());
      final UserService userService = context.getBean(UserService.class);
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
      final Object by_id_666 = users.get("by_id_666");
      assertThat(today).isEqualTo(user).isEqualTo(by_id_666);
      assertThat(userService.getAccessTime()).isEqualTo(2);

    }

  }

}
