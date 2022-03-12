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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.cache.support.SimpleValueWrapper;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class CacheErrorHandlerTests {

  private Cache cache;

  private CacheInterceptor cacheInterceptor;

  private CacheErrorHandler errorHandler;

  private SimpleService simpleService;

  @BeforeEach
  public void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    this.cache = context.getBean("mockCache", Cache.class);
    this.cacheInterceptor = context.getBean(CacheInterceptor.class);
    this.errorHandler = context.getBean(CacheErrorHandler.class);
    this.simpleService = context.getBean(SimpleService.class);
  }

  @Test
  public void getFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(0L);

    Object result = this.simpleService.get(0L);
    verify(this.errorHandler).handleCacheGetError(exception, cache, 0L);
    verify(this.cache).get(0L);
    verify(this.cache).put(0L, result); // result of the invocation
  }

  @Test
  public void getAndPutFail() {
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
  public void getFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    willThrow(exception).given(this.cache).get(0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    this.simpleService.get(0L))
            .withMessage("Test exception on get");
  }

  @Test
  public void putFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    willThrow(exception).given(this.cache).put(0L, 0L);

    this.simpleService.put(0L);
    verify(this.errorHandler).handleCachePutError(exception, cache, 0L, 0L);
  }

  @Test
  public void putFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    willThrow(exception).given(this.cache).put(0L, 0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    this.simpleService.put(0L))
            .withMessage("Test exception on put");
  }

  @Test
  public void evictFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).evict(0L);

    this.simpleService.evict(0L);
    verify(this.errorHandler).handleCacheEvictError(exception, cache, 0L);
  }

  @Test
  public void evictFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).evict(0L);

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    this.simpleService.evict(0L))
            .withMessage("Test exception on evict");
  }

  @Test
  public void clearFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).clear();

    this.simpleService.clear();
    verify(this.errorHandler).handleCacheClearError(exception, cache);
  }

  @Test
  public void clearFailProperException() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on clear");
    willThrow(exception).given(this.cache).clear();

    this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    this.simpleService.clear())
            .withMessage("Test exception on clear");
  }

  @Configuration
  @EnableCaching
  static class Config implements CachingConfigurer {

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
      return mock(CacheErrorHandler.class);
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
      Cache cache = mock(Cache.class);
      given(cache.getName()).willReturn("test");
      return cache;
    }

  }

  @CacheConfig(cacheNames = "test")
  public static class SimpleService {
    private AtomicLong counter = new AtomicLong();

    @Cacheable
    public Object get(long id) {
      return this.counter.getAndIncrement();
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
