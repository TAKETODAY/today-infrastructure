/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.CachePut;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.cache.support.SimpleCacheManager;
import infra.cache.support.SimpleValueWrapper;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.util.function.ThrowingFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class CacheErrorHandlerTests {

  private AnnotationConfigApplicationContext context;

  private Cache cache;

  private CacheInterceptor cacheInterceptor;

  private CacheErrorHandler errorHandler;

  private SimpleService simpleService;

  @BeforeEach
  void setup() {
    this.context = new AnnotationConfigApplicationContext(Config.class);
    this.cache = context.getBean("mockCache", Cache.class);
    this.cacheInterceptor = context.getBean(CacheInterceptor.class);
    this.errorHandler = context.getBean(CacheErrorHandler.class);
    this.simpleService = context.getBean(SimpleService.class);
  }

  @AfterEach
  void closeContext() {
    this.context.close();
  }

  @Test
  void getFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(0L);

    Object result = this.simpleService.get(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, 0L);
    verify(this.cache).get(0L);
    verify(this.cache).put(0L, result); // result of the invocation
  }

  @Test
  public void getSyncFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(eq(0L), any(ThrowingFunction.class));

    Object result = this.simpleService.getSync(0L);
    assertThat(result).isEqualTo(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, 0L);
    verify(this.cache).get(eq(0L), any(ThrowingFunction.class));
  }

  @Test
  public void getCompletableFutureFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).retrieve(eq(0L));

    Object result = this.simpleService.getFuture(0L).join();
    assertThat(result).isEqualTo(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, 0L);
    verify(this.cache).retrieve(eq(0L));
  }

  @Test
  public void getMonoFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).retrieve(eq(0L));

    Object result = this.simpleService.getMono(0L).block();
    assertThat(result).isEqualTo(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, 0L);
    verify(this.cache).retrieve(eq(0L));
  }

  @Test
  public void getFluxFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).retrieve(eq(0L));

    Object result = this.simpleService.getFlux(0L).blockLast();
    assertThat(result).isEqualTo(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, 0L);
    verify(this.cache).retrieve(eq(0L));
  }

  @Test
  void getAndPutFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(0L);
    willThrow(exception).given(this.cache).put(0L, 0L); // Update of the cache will fail as well

    Object counter = this.simpleService.get(0L);

    willReturn(new SimpleValueWrapper(2L)).given(this.cache).get(0L);
    Object counter2 = this.simpleService.get(0L);
    Object counter3 = this.simpleService.get(0L);
    assertThat(counter2).isNotSameAs(counter);
    assertThat(counter3).isEqualTo(counter2);
  }

  @Test
  void getFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> this.simpleService.get(0L))
            .withMessage("Test exception on get");
  }

  @Test
  void putFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    willThrow(exception).given(this.cache).put(0L, 0L);

    this.simpleService.put(0L);
    verify(this.errorHandler).handleCachePutError(exception, cache, 0L, 0L);
  }

  @Test
  void putFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    willThrow(exception).given(this.cache).put(0L, 0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> this.simpleService.put(0L))
            .withMessage("Test exception on put");
  }

  @Test
  void evictFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).evict(0L);

    this.simpleService.evict(0L);
    verify(this.errorHandler).handleCacheEvictError(exception, cache, 0L);
  }

  @Test
  void evictFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).evict(0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> this.simpleService.evict(0L))
            .withMessage("Test exception on evict");
  }

  @Test
  void clearFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).clear();

    this.simpleService.clear();
    verify(this.errorHandler).handleCacheClearError(exception, cache);
  }

  @Test
  void clearFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on clear");
    willThrow(exception).given(this.cache).clear();

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> this.simpleService.clear())
            .withMessage("Test exception on clear");
  }

  @Configuration
  @EnableCaching
  static class Config implements CachingConfigurer {

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
      return mock();
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }

    @Override
    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager cacheManager = new SimpleCacheManager();
      cacheManager.setCaches(Collections.singletonList(mockCache()));
      return cacheManager;
    }

    @Bean
    public Cache mockCache() {
      Cache cache = mock();
      given(cache.getName()).willReturn("test");
      return cache;
    }

  }

  @CacheConfig("test")
  public static class SimpleService {
    private AtomicLong counter = new AtomicLong();

    @Cacheable
    public Object get(long id) {
      return this.counter.getAndIncrement();
    }

    @Cacheable(sync = true)
    public Object getSync(long id) {
      return this.counter.getAndIncrement();
    }

    @Cacheable
    public CompletableFuture<Long> getFuture(long id) {
      return CompletableFuture.completedFuture(this.counter.getAndIncrement());
    }

    @Cacheable
    public Mono<Long> getMono(long id) {
      return Mono.just(this.counter.getAndIncrement());
    }

    @Cacheable
    public Flux<Long> getFlux(long id) {
      return Flux.just(this.counter.getAndIncrement(), 0L);
    }

    @CachePut
    public Object put(long id) {
      return this.counter.getAndIncrement();
    }

    @CacheEvict
    public void evict(long id) {
    }

    @CacheEvict(allEntries = true)
    public void clear() {
    }
  }

}
