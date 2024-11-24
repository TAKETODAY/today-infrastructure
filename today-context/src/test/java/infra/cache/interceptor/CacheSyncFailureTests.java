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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import infra.cache.CacheManager;
import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.Caching;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.CacheTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Provides various failure scenario linked to the use of {@link Cacheable#sync()}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class CacheSyncFailureTests {

  private ConfigurableApplicationContext context;

  private SimpleService simpleService;

  @BeforeEach
  public void setup() {
    this.context = new AnnotationConfigApplicationContext(Config.class);
    this.simpleService = this.context.getBean(SimpleService.class);
  }

  @AfterEach
  public void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void unlessSync() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.simpleService.unlessSync("key"))
            .withMessageContaining("A sync=true operation does not support the unless attribute");
  }

  @Test
  public void severalCachesSync() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.simpleService.severalCachesSync("key"))
            .withMessageContaining("A sync=true operation is restricted to a single cache");
  }

  @Test
  public void severalCachesWithResolvedSync() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.simpleService.severalCachesWithResolvedSync("key"))
            .withMessageContaining("A sync=true operation is restricted to a single cache");
  }

  @Test
  public void syncWithAnotherOperation() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.simpleService.syncWithAnotherOperation("key"))
            .withMessageContaining("A sync=true operation cannot be combined with other cache operations");
  }

  @Test
  public void syncWithTwoGetOperations() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.simpleService.syncWithTwoGetOperations("key"))
            .withMessageContaining("Only one sync=true operation is allowed");
  }

  static class SimpleService {

    private final AtomicLong counter = new AtomicLong();

    @Cacheable(cacheNames = "testCache", sync = true, unless = "#result > 10")
    public Object unlessSync(Object arg1) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheNames = { "testCache", "anotherTestCache" }, sync = true)
    public Object severalCachesSync(Object arg1) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheResolver = "testCacheResolver", sync = true)
    public Object severalCachesWithResolvedSync(Object arg1) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(cacheNames = "testCache", sync = true)
    @CacheEvict(cacheNames = "anotherTestCache", key = "#arg1")
    public Object syncWithAnotherOperation(Object arg1) {
      return this.counter.getAndIncrement();
    }

    @Caching(cacheable = {
            @Cacheable(cacheNames = "testCache", sync = true),
            @Cacheable(cacheNames = "anotherTestCache", sync = true)
    })
    public Object syncWithTwoGetOperations(Object arg1) {
      return this.counter.getAndIncrement();
    }
  }

  @Configuration
  @EnableCaching
  static class Config implements CachingConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return CacheTestUtils.createSimpleCacheManager("testCache", "anotherTestCache");
    }

    @Bean
    public CacheResolver testCacheResolver() {
      return new NamedCacheResolver(cacheManager(), "testCache", "anotherTestCache");
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }
  }

}
