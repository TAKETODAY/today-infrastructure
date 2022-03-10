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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleKey;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.cache.jcache.config.JCacheConfigurer;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class JCacheKeyGeneratorTests {

  private TestKeyGenerator keyGenerator;

  private SimpleService simpleService;

  private Cache cache;

  @BeforeEach
  public void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    this.keyGenerator = context.getBean(TestKeyGenerator.class);
    this.simpleService = context.getBean(SimpleService.class);
    this.cache = context.getBean(CacheManager.class).getCache("test");
    context.close();
  }

  @Test
  public void getSimple() {
    this.keyGenerator.expect(1L);
    Object first = this.simpleService.get(1L);
    Object second = this.simpleService.get(1L);
    assertThat(second).isSameAs(first);

    Object key = new SimpleKey(1L);
    assertThat(cache.get(key).get()).isEqualTo(first);
  }

  @Test
  public void getFlattenVararg() {
    this.keyGenerator.expect(1L, "foo", "bar");
    Object first = this.simpleService.get(1L, "foo", "bar");
    Object second = this.simpleService.get(1L, "foo", "bar");
    assertThat(second).isSameAs(first);

    Object key = new SimpleKey(1L, "foo", "bar");
    assertThat(cache.get(key).get()).isEqualTo(first);
  }

  @Test
  public void getFiltered() {
    this.keyGenerator.expect(1L);
    Object first = this.simpleService.getFiltered(1L, "foo", "bar");
    Object second = this.simpleService.getFiltered(1L, "foo", "bar");
    assertThat(second).isSameAs(first);

    Object key = new SimpleKey(1L);
    assertThat(cache.get(key).get()).isEqualTo(first);
  }

  @Configuration
  @EnableCaching
  static class Config implements JCacheConfigurer {

    @Bean
    @Override
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
      return new TestKeyGenerator();
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }

  }

  @CacheDefaults(cacheName = "test")
  public static class SimpleService {
    private AtomicLong counter = new AtomicLong();

    @CacheResult
    public Object get(long id) {
      return counter.getAndIncrement();
    }

    @CacheResult
    public Object get(long id, String... items) {
      return counter.getAndIncrement();
    }

    @CacheResult
    public Object getFiltered(@CacheKey long id, String... items) {
      return counter.getAndIncrement();
    }

  }

  private static class TestKeyGenerator extends SimpleKeyGenerator {

    private Object[] expectedParams;

    private void expect(Object... params) {
      this.expectedParams = params;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
      assertThat(Arrays.equals(expectedParams, params)).as("Unexpected parameters: expected: "
              + Arrays.toString(this.expectedParams) + " but got: " + Arrays.toString(params)).isTrue();
      return new SimpleKey(params);
    }
  }
}
