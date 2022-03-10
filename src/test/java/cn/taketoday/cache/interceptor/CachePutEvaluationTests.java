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

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests corner case of using {@link Cacheable} and  {@link CachePut} on the
 * same operation.
 *
 * @author Stephane Nicoll
 */
public class CachePutEvaluationTests {

  private ConfigurableApplicationContext context;

  private Cache cache;

  private SimpleService service;

  @BeforeEach
  public void setup() {
    this.context = new AnnotationConfigApplicationContext(Config.class);
    this.cache = this.context.getBean(CacheManager.class).getCache("test");
    this.service = this.context.getBean(SimpleService.class);
  }

  @AfterEach
  public void close() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void mutualGetPutExclusion() {
    String key = "1";

    Long first = this.service.getOrPut(key, true);
    Long second = this.service.getOrPut(key, true);
    assertThat(second).isSameAs(first);

    // This forces the method to be executed again
    Long expected = first + 1;
    Long third = this.service.getOrPut(key, false);
    assertThat(third).isEqualTo(expected);

    Long fourth = this.service.getOrPut(key, true);
    assertThat(fourth).isSameAs(third);
  }

  @Test
  public void getAndPut() {
    this.cache.clear();

    long key = 1;
    Long value = this.service.getAndPut(key);

    assertThat(this.cache.get(key).get()).as("Wrong value for @Cacheable key").isEqualTo(value);
    // See @CachePut
    assertThat(this.cache.get(value + 100).get()).as("Wrong value for @CachePut key").isEqualTo(value);

    // CachePut forced a method call
    Long anotherValue = this.service.getAndPut(key);
    assertThat(anotherValue).isNotSameAs(value);
    // NOTE: while you might expect the main key to have been updated, it hasn't. @Cacheable operations
    // are only processed in case of a cache miss. This is why combining @Cacheable with @CachePut
    // is a very bad idea. We could refine the condition now that we can figure out if we are going
    // to invoke the method anyway but that brings a whole new set of potential regressions.
    //assertEquals("Wrong value for @Cacheable key", anotherValue, cache.get(key).get());
    assertThat(this.cache.get(anotherValue + 100).get()).as("Wrong value for @CachePut key").isEqualTo(anotherValue);
  }

  @Configuration
  @EnableCaching
  static class Config implements CachingConfigurer {

    @Bean
    @Override
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }

  }

  public static class SimpleService {
    private AtomicLong counter = new AtomicLong();

    /**
     * Represent a mutual exclusion use case. The boolean flag exclude one of the two operation.
     */
    @Cacheable(condition = "#p1", key = "#p0")
    @CachePut(condition = "!#p1", key = "#p0")
    public Long getOrPut(Object id, boolean flag) {
      return this.counter.getAndIncrement();
    }

    /**
     * Represent an invalid use case. If the result of the operation is non null, then we put
     * the value with a different key. This forces the method to be executed every time.
     */
    @Cacheable
    @CachePut(key = "#result + 100", condition = "#result != null")
    public Long getAndPut(long id) {
      return this.counter.getAndIncrement();
    }
  }
}
