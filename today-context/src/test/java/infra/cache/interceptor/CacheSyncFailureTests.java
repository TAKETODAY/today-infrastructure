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
