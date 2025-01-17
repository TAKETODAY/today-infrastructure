/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.cache.caffeine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import infra.cache.CacheManager;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.EnableCaching;
import infra.cache.support.CaffeineCacheManager;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for annotation-based caching methods that use reactive operators.
 *
 * @author Juergen Hoeller
 */
public class CaffeineReactiveCachingTests {

  @ParameterizedTest
  @ValueSource(classes = { AsyncCacheModeConfig.class, AsyncCacheModeConfig.class })
  void cacheHitDetermination(Class<?> configClass) {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(configClass, ReactiveCacheableService.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Object key = new Object();

    Long r1 = service.cacheFuture(key).join();
    Long r2 = service.cacheFuture(key).join();
    Long r3 = service.cacheFuture(key).join();

    assertThat(r1).isNotNull();
    assertThat(r1).isSameAs(r2).isSameAs(r3);

    key = new Object();

    r1 = service.cacheMono(key).block();
    r2 = service.cacheMono(key).block();
    r3 = service.cacheMono(key).block();

    assertThat(r1).isNotNull();
    assertThat(r1).isSameAs(r2).isSameAs(r3);

    key = new Object();

    r1 = service.cacheFlux(key).blockFirst();
    r2 = service.cacheFlux(key).blockFirst();
    r3 = service.cacheFlux(key).blockFirst();

    assertThat(r1).isNotNull();
    assertThat(r1).isSameAs(r2).isSameAs(r3);

    key = new Object();

    List<Long> l1 = service.cacheFlux(key).collectList().block();
    List<Long> l2 = service.cacheFlux(key).collectList().block();
    List<Long> l3 = service.cacheFlux(key).collectList().block();

    assertThat(l1).isNotNull();
    assertThat(l1).isEqualTo(l2).isEqualTo(l3);

    key = new Object();

    r1 = service.cacheMono(key).block();
    r2 = service.cacheMono(key).block();
    r3 = service.cacheMono(key).block();

    assertThat(r1).isNotNull();
    assertThat(r1).isSameAs(r2).isSameAs(r3);

    // Same key as for Mono, reusing its cached value

    r1 = service.cacheFlux(key).blockFirst();
    r2 = service.cacheFlux(key).blockFirst();
    r3 = service.cacheFlux(key).blockFirst();

    assertThat(r1).isNotNull();
    assertThat(r1).isSameAs(r2).isSameAs(r3);
  }

  @CacheConfig(cacheNames = "first")
  static class ReactiveCacheableService {

    private final AtomicLong counter = new AtomicLong();

    @Cacheable
    public CompletableFuture<Long> cacheFuture(Object arg) {
      return CompletableFuture.completedFuture(this.counter.getAndIncrement());
    }

    @Cacheable
    public Mono<Long> cacheMono(Object arg) {
      return Mono.just(this.counter.getAndIncrement());
    }

    @Cacheable
    public Flux<Long> cacheFlux(Object arg) {
      return Flux.just(this.counter.getAndIncrement(), 0L);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class AsyncCacheModeConfig {

    @Bean
    CacheManager cacheManager() {
      CaffeineCacheManager cm = new CaffeineCacheManager("first");
      cm.setAsyncCacheMode(true);
      return cm;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class AsyncCacheModeWithoutNullValuesConfig {

    @Bean
    CacheManager cacheManager() {
      CaffeineCacheManager ccm = new CaffeineCacheManager("first");
      ccm.setAsyncCacheMode(true);
      ccm.setAllowNullValues(false);
      return ccm;
    }
  }

}
