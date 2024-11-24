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

package infra.cache.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.cache.concurrent.ConcurrentMapCacheManager;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.LoggingCacheErrorHandler;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

/**
 * Tests for annotation-based caching methods that use reactive operators.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class ReactiveCachingTests {

  @ParameterizedTest
  @ValueSource(classes = { EarlyCacheHitDeterminationConfig.class,
          EarlyCacheHitDeterminationWithoutNullValuesConfig.class,
          LateCacheHitDeterminationConfig.class,
          LateCacheHitDeterminationWithValueWrapperConfig.class })
  void cacheHitDetermination(Class<?> configClass) {

    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(configClass, ReactiveCacheableService.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Object key = new Object();

    Long r1 = service.cacheFuture(key).join();
    Long r2 = service.cacheFuture(key).join();
    Long r3 = service.cacheFuture(key).join();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheFuture").isSameAs(r2).isSameAs(r3);

    key = new Object();

    r1 = service.cacheMono(key).block();
    r2 = service.cacheMono(key).block();
    r3 = service.cacheMono(key).block();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheMono").isSameAs(r2).isSameAs(r3);

    key = new Object();

    r1 = service.cacheFlux(key).blockFirst();
    r2 = service.cacheFlux(key).blockFirst();
    r3 = service.cacheFlux(key).blockFirst();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheFlux blockFirst").isSameAs(r2).isSameAs(r3);

    key = new Object();

    List<Long> l1 = service.cacheFlux(key).collectList().block();
    List<Long> l2 = service.cacheFlux(key).collectList().block();
    List<Long> l3 = service.cacheFlux(key).collectList().block();

    assertThat(l1).isNotNull();
    assertThat(l1).as("cacheFlux collectList").isEqualTo(l2).isEqualTo(l3);

    key = new Object();

    r1 = service.cacheMono(key).block();
    r2 = service.cacheMono(key).block();
    r3 = service.cacheMono(key).block();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheMono common key").isSameAs(r2).isSameAs(r3);

    // Same key as for Mono, reusing its cached value

    r1 = service.cacheFlux(key).blockFirst();
    r2 = service.cacheFlux(key).blockFirst();
    r3 = service.cacheFlux(key).blockFirst();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheFlux blockFirst common key").isSameAs(r2).isSameAs(r3);

    ctx.close();
  }

  @Test
  void cacheErrorHandlerWithLoggingCacheErrorHandler() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ExceptionCacheManager.class, ReactiveCacheableService.class, ErrorHandlerCachingConfiguration.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Object key = new Object();
    Long r1 = service.cacheFuture(key).join();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheFuture").isEqualTo(0L);

    key = new Object();

    r1 = service.cacheMono(key).block();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheMono").isEqualTo(1L);

    key = new Object();

    r1 = service.cacheFlux(key).blockFirst();

    assertThat(r1).isNotNull();
    assertThat(r1).as("cacheFlux blockFirst").isEqualTo(2L);
  }

  @Test
  void cacheErrorHandlerWithLoggingCacheErrorHandlerAndMethodError() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ExceptionCacheManager.class, ReactiveFailureCacheableService.class, ErrorHandlerCachingConfiguration.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Object key = new Object();
    StepVerifier.create(service.cacheMono(key))
            .expectErrorMessage("mono service error")
            .verify();

    key = new Object();
    StepVerifier.create(service.cacheFlux(key))
            .expectErrorMessage("flux service error")
            .verify();
  }

  @Test
  void cacheErrorHandlerWithSimpleCacheErrorHandler() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ExceptionCacheManager.class, ReactiveCacheableService.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Throwable completableFuturThrowable = catchThrowable(() -> service.cacheFuture(new Object()).join());
    assertThat(completableFuturThrowable).isInstanceOf(CompletionException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class);

    Throwable monoThrowable = catchThrowable(() -> service.cacheMono(new Object()).block());
    assertThat(monoThrowable).isInstanceOf(UnsupportedOperationException.class);

    Throwable fluxThrowable = catchThrowable(() -> service.cacheFlux(new Object()).blockFirst());
    assertThat(fluxThrowable).isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @ValueSource(classes = { EarlyCacheHitDeterminationConfig.class,
          EarlyCacheHitDeterminationWithoutNullValuesConfig.class,
          LateCacheHitDeterminationConfig.class,
          LateCacheHitDeterminationWithValueWrapperConfig.class })
  void fluxCacheDoesntDependOnFirstRequest(Class<?> configClass) {

    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(configClass, ReactiveCacheableService.class);
    ReactiveCacheableService service = ctx.getBean(ReactiveCacheableService.class);

    Object key = new Object();

    List<Long> l1 = service.cacheFlux(key).take(1L, true).collectList().block();
    List<Long> l2 = service.cacheFlux(key).take(3L, true).collectList().block();
    List<Long> l3 = service.cacheFlux(key).collectList().block();

    Long first = l1.get(0);

    assertThat(l1).as("l1").containsExactly(first);
    assertThat(l2).as("l2").containsExactly(first, 0L, -1L);
    assertThat(l3).as("l3").containsExactly(first, 0L, -1L, -2L, -3L);

    ctx.close();
  }

  @CacheConfig(cacheNames = "first")
  static class ReactiveCacheableService {

    private final AtomicLong counter = new AtomicLong();

    @Cacheable
    CompletableFuture<Long> cacheFuture(Object arg) {
      return CompletableFuture.completedFuture(this.counter.getAndIncrement());
    }

    @Cacheable
    Mono<Long> cacheMono(Object arg) {
      // here counter not only reflects invocations of cacheMono but subscriptions to
      // the returned Mono as well. See https://github.com/spring-projects/spring-framework/issues/32370
      return Mono.defer(() -> Mono.just(this.counter.getAndIncrement()));
    }

    @Cacheable
    Flux<Long> cacheFlux(Object arg) {
      // here counter not only reflects invocations of cacheFlux but subscriptions to
      // the returned Flux as well. See https://github.com/spring-projects/spring-framework/issues/32370
      return Flux.defer(() -> Flux.just(this.counter.getAndIncrement(), 0L, -1L, -2L, -3L));
    }
  }

  @CacheConfig(cacheNames = "first")
  static class ReactiveFailureCacheableService extends ReactiveCacheableService {

    @Cacheable
    Mono<Long> cacheMono(Object arg) {
      return Mono.error(new IllegalStateException("mono service error"));
    }

    @Cacheable
    Flux<Long> cacheFlux(Object arg) {
      return Flux.error(new IllegalStateException("flux service error"));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class EarlyCacheHitDeterminationConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("first");
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class EarlyCacheHitDeterminationWithoutNullValuesConfig {

    @Bean
    CacheManager cacheManager() {
      ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("first");
      cm.setAllowNullValues(false);
      return cm;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class LateCacheHitDeterminationConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("first") {
        @Override
        protected Cache createConcurrentMapCache(String name) {
          return new ConcurrentMapCache(name, isAllowNullValues()) {
            @Override
            public CompletableFuture<?> retrieve(Object key) {
              return CompletableFuture.completedFuture(lookup(key));
            }

            @Override
            public void put(Object key, @Nullable Object value) {
              assertThat(get(key)).as("Double put").isNull();
              super.put(key, value);
            }
          };
        }
      };
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class LateCacheHitDeterminationWithValueWrapperConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("first") {
        @Override
        protected Cache createConcurrentMapCache(String name) {
          return new ConcurrentMapCache(name, isAllowNullValues()) {
            @Override
            public CompletableFuture<?> retrieve(Object key) {
              Object value = lookup(key);
              return CompletableFuture.completedFuture(value != null ? toValueWrapper(value) : null);
            }

            @Override
            public void put(Object key, @Nullable Object value) {
              assertThat(get(key)).as("Double put").isNull();
              super.put(key, value);
            }
          };
        }
      };
    }
  }

  @Configuration
  static class ErrorHandlerCachingConfiguration implements CachingConfigurer {

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
      return new LoggingCacheErrorHandler();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class ExceptionCacheManager {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("first") {
        @Override
        protected Cache createConcurrentMapCache(String name) {
          return new ConcurrentMapCache(name, isAllowNullValues()) {
            @Override
            public CompletableFuture<?> retrieve(Object key) {
              return CompletableFuture.supplyAsync(() -> {
                throw new UnsupportedOperationException("Test exception on retrieve");
              });
            }

            @Override
            public void put(Object key, @Nullable Object value) {
              throw new UnsupportedOperationException("Test exception on put");
            }
          };
        }
      };
    }
  }

}
