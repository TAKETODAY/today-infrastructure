/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.context.testfixture.cache.CacheTestUtils;
import cn.taketoday.core.env.Environment;

import static cn.taketoday.context.testfixture.cache.CacheTestUtils.assertCacheHit;
import static cn.taketoday.context.testfixture.cache.CacheTestUtils.assertCacheMiss;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that represent real use cases with advanced configuration.
 *
 * @author Stephane Nicoll
 */
public class EnableCachingIntegrationTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  public void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void fooServiceWithInterface() {
    this.context = new AnnotationConfigApplicationContext(FooConfig.class);
    FooService service = this.context.getBean(FooService.class);
    fooGetSimple(service);
  }

  @Test
  public void fooServiceWithInterfaceCglib() {
    this.context = new AnnotationConfigApplicationContext(FooConfigCglib.class);
    FooService service = this.context.getBean(FooService.class);
    fooGetSimple(service);
  }

  private void fooGetSimple(FooService service) {
    Cache cache = getCache();

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = service.getSimple(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void barServiceWithCacheableInterfaceCglib() {
    this.context = new AnnotationConfigApplicationContext(BarConfigCglib.class);
    BarService service = this.context.getBean(BarService.class);
    Cache cache = getCache();

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = service.getSimple(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void beanConditionOff() {
    this.context = new AnnotationConfigApplicationContext(BeanConditionConfig.class);
    FooService service = this.context.getBean(FooService.class);
    Cache cache = getCache();

    Object key = new Object();
    service.getWithCondition(key);
    assertCacheMiss(key, cache);
    service.getWithCondition(key);
    assertCacheMiss(key, cache);

    assertThat(this.context.getBean(BeanConditionConfig.Bar.class).count).isEqualTo(2);
  }

  @Test
  public void beanConditionOn() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.setEnvironment(new MockEnvironment().withProperty("bar.enabled", "true"));
    ctx.register(BeanConditionConfig.class);
    ctx.refresh();
    this.context = ctx;

    FooService service = this.context.getBean(FooService.class);
    Cache cache = getCache();

    Object key = new Object();
    Object value = service.getWithCondition(key);
    assertCacheHit(key, value, cache);
    value = service.getWithCondition(key);
    assertCacheHit(key, value, cache);

    assertThat(this.context.getBean(BeanConditionConfig.Bar.class).count).isEqualTo(2);
  }

  private Cache getCache() {
    return this.context.getBean(CacheManager.class).getCache("testCache");
  }

  @Configuration
  static class SharedConfig implements CachingConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return CacheTestUtils.createSimpleCacheManager("testCache");
    }
  }

  @Configuration
  @Import(SharedConfig.class)
  @EnableCaching
  static class FooConfig {

    @Bean
    public FooService fooService() {
      return new FooServiceImpl();
    }
  }

  @Configuration
  @Import(SharedConfig.class)
  @EnableCaching(proxyTargetClass = true)
  static class FooConfigCglib {

    @Bean
    public FooService fooService() {
      return new FooServiceImpl();
    }
  }

  interface FooService {

    Object getSimple(Object key);

    Object getWithCondition(Object key);
  }

  @CacheConfig(cacheNames = "testCache")
  static class FooServiceImpl implements FooService {

    private final AtomicLong counter = new AtomicLong();

    @Override
    @Cacheable
    public Object getSimple(Object key) {
      return this.counter.getAndIncrement();
    }

    @Override
    @Cacheable(condition = "@bar.enabled")
    public Object getWithCondition(Object key) {
      return this.counter.getAndIncrement();
    }
  }

  @Configuration
  @Import(SharedConfig.class)
  @EnableCaching(proxyTargetClass = true)
  static class BarConfigCglib {

    @Bean
    public BarService barService() {
      return new BarServiceImpl();
    }
  }

  interface BarService {

    @Cacheable(cacheNames = "testCache")
    Object getSimple(Object key);
  }

  static class BarServiceImpl implements BarService {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public Object getSimple(Object key) {
      return this.counter.getAndIncrement();
    }
  }

  @Configuration
  @Import(FooConfig.class)
  @EnableCaching
  static class BeanConditionConfig {

    @Autowired
    Environment env;

    @Bean
    public Bar bar() {
      return new Bar(Boolean.parseBoolean(env.getProperty("bar.enabled")));
    }

    static class Bar {

      public int count;

      private final boolean enabled;

      public Bar(boolean enabled) {
        this.enabled = enabled;
      }

      public boolean isEnabled() {
        this.count++;
        return this.enabled;
      }
    }
  }

}
