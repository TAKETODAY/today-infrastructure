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

package infra.cache.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.CacheTestUtils;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;

import static infra.context.testfixture.cache.CacheTestUtils.assertCacheHit;
import static infra.context.testfixture.cache.CacheTestUtils.assertCacheMiss;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Provides various {@link CacheResolver} customisations scenario
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class CacheResolverCustomizationTests {

  private CacheManager cacheManager;

  private CacheManager anotherCacheManager;

  private SimpleService simpleService;

  @BeforeEach
  public void setup() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    this.cacheManager = context.getBean("cacheManager", CacheManager.class);
    this.anotherCacheManager = context.getBean("anotherCacheManager", CacheManager.class);
    this.simpleService = context.getBean(SimpleService.class);
  }

  @Test
  public void noCustomization() {
    Cache cache = this.cacheManager.getCache("default");

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = this.simpleService.getSimple(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void customCacheResolver() {
    Cache cache = this.cacheManager.getCache("primary");

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = this.simpleService.getWithCustomCacheResolver(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void customCacheManager() {
    Cache cache = this.anotherCacheManager.getCache("default");

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = this.simpleService.getWithCustomCacheManager(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void runtimeResolution() {
    Cache defaultCache = this.cacheManager.getCache("default");
    Cache primaryCache = this.cacheManager.getCache("primary");

    Object key = new Object();
    assertCacheMiss(key, defaultCache, primaryCache);
    Object value = this.simpleService.getWithRuntimeCacheResolution(key, "default");
    assertCacheHit(key, value, defaultCache);
    assertCacheMiss(key, primaryCache);

    Object key2 = new Object();
    assertCacheMiss(key2, defaultCache, primaryCache);
    Object value2 = this.simpleService.getWithRuntimeCacheResolution(key2, "primary");
    assertCacheHit(key2, value2, primaryCache);
    assertCacheMiss(key2, defaultCache);
  }

  @Test
  public void namedResolution() {
    Cache cache = this.cacheManager.getCache("secondary");

    Object key = new Object();
    assertCacheMiss(key, cache);

    Object value = this.simpleService.getWithNamedCacheResolution(key);
    assertCacheHit(key, value, cache);
  }

  @Test
  public void noCacheResolved() {
    Method method = ReflectionUtils.findMethod(SimpleService.class, "noCacheResolved", Object.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> simpleService.noCacheResolved(new Object()))
            .withMessageContaining(method.toString());
  }

  @Test
  public void unknownCacheResolver() {
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> simpleService.unknownCacheResolver(new Object()))
            .satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("unknownCacheResolver"));
  }

  @Configuration
  @EnableCaching
  static class Config implements CachingConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return CacheTestUtils.createSimpleCacheManager("default", "primary", "secondary");
    }

    @Bean
    public CacheManager anotherCacheManager() {
      return CacheTestUtils.createSimpleCacheManager("default", "primary", "secondary");
    }

    @Bean
    public CacheResolver primaryCacheResolver() {
      return new NamedCacheResolver(cacheManager(), "primary");
    }

    @Bean
    public CacheResolver secondaryCacheResolver() {
      return new NamedCacheResolver(cacheManager(), "primary");
    }

    @Bean
    public CacheResolver runtimeCacheResolver() {
      return new RuntimeCacheResolver(cacheManager());
    }

    @Bean
    public CacheResolver namedCacheResolver() {
      NamedCacheResolver resolver = new NamedCacheResolver();
      resolver.setCacheManager(cacheManager());
      resolver.setCacheNames(Collections.singleton("secondary"));
      return resolver;
    }

    @Bean
    public CacheResolver nullCacheResolver() {
      return new NullCacheResolver(cacheManager());
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }
  }

  @CacheConfig(cacheNames = "default")
  static class SimpleService {

    private final AtomicLong counter = new AtomicLong();

    @Cacheable
    public Object getSimple(Object key) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "primaryCacheResolver")
    public Object getWithCustomCacheResolver(Object key) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheManager = "anotherCacheManager")
    public Object getWithCustomCacheManager(Object key) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "runtimeCacheResolver", key = "#p0")
    public Object getWithRuntimeCacheResolution(Object key, String cacheName) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "namedCacheResolver")
    public Object getWithNamedCacheResolution(Object key) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "nullCacheResolver") // No cache resolved for the operation
    public Object noCacheResolved(Object key) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "unknownCacheResolver") // No such bean defined
    public Object unknownCacheResolver(Object key) {
      return this.counter.getAndIncrement();
    }
  }

  /**
   * Example of {@link CacheResolver} that resolve the caches at
   * runtime (i.e. based on method invocation parameters).
   * <p>Expects the second argument to hold the name of the cache to use
   */
  private static class RuntimeCacheResolver extends AbstractCacheResolver {

    private RuntimeCacheResolver(CacheManager cacheManager) {
      super(cacheManager);
    }

    @Override
    @Nullable
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
      String cacheName = (String) context.getArgs()[1];
      return Collections.singleton(cacheName);
    }
  }

  private static class NullCacheResolver extends AbstractCacheResolver {

    private NullCacheResolver(CacheManager cacheManager) {
      super(cacheManager);
    }

    @Override
    @Nullable
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
      return null;
    }
  }

}
