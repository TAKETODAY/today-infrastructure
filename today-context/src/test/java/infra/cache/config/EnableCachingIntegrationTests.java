/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.support.MockEnvironment;
import infra.context.testfixture.cache.CacheTestUtils;
import infra.core.env.Environment;

import static infra.context.testfixture.cache.CacheTestUtils.assertCacheHit;
import static infra.context.testfixture.cache.CacheTestUtils.assertCacheMiss;
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

  @Test  // gh-31238
  public void cglibProxyClassIsCachedAcrossApplicationContexts() {
    ConfigurableApplicationContext ctx;

    // Round #1
    ctx = new AnnotationConfigApplicationContext(FooConfigCglib.class);
    FooService service1 = ctx.getBean(FooService.class);
    assertThat(AopUtils.isCglibProxy(service1)).as("FooService #1 is not a CGLIB proxy").isTrue();
    ctx.close();

    // Round #2
    ctx = new AnnotationConfigApplicationContext(FooConfigCglib.class);
    FooService service2 = ctx.getBean(FooService.class);
    assertThat(AopUtils.isCglibProxy(service2)).as("FooService #2 is not a CGLIB proxy").isTrue();
    ctx.close();

    assertThat(service1.getClass()).isSameAs(service2.getClass());
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
