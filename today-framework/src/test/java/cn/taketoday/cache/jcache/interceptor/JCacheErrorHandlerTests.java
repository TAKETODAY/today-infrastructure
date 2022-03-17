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

package cn.taketoday.cache.jcache.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.cache.jcache.config.JCacheConfigurer;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class JCacheErrorHandlerTests {

  private Cache cache;

  private Cache errorCache;

  private CacheErrorHandler errorHandler;

  private SimpleService simpleService;

  @BeforeEach
  public void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    this.cache = context.getBean("mockCache", Cache.class);
    this.errorCache = context.getBean("mockErrorCache", Cache.class);
    this.errorHandler = context.getBean(CacheErrorHandler.class);
    this.simpleService = context.getBean(SimpleService.class);
    context.close();
  }

  @Test
  public void getFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
    Object key = SimpleKeyGenerator.generateKey(0L);
    willThrow(exception).given(this.cache).get(key);

    this.simpleService.get(0L);
    verify(this.errorHandler).handleCacheGetError(exception, this.cache, key);
  }

  @Test
  public void getPutNewElementFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    Object key = SimpleKeyGenerator.generateKey(0L);
    given(this.cache.get(key)).willReturn(null);
    willThrow(exception).given(this.cache).put(key, 0L);

    this.simpleService.get(0L);
    verify(this.errorHandler).handleCachePutError(exception, this.cache, key, 0L);
  }

  @Test
  public void getFailPutExceptionFail() {
    UnsupportedOperationException exceptionOnPut = new UnsupportedOperationException("Test exception on put");
    Object key = SimpleKeyGenerator.generateKey(0L);
    given(this.cache.get(key)).willReturn(null);
    willThrow(exceptionOnPut).given(this.errorCache).put(key, SimpleService.TEST_EXCEPTION);

    try {
      this.simpleService.getFail(0L);
    }
    catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).isEqualTo("Test exception");
    }
    verify(this.errorHandler).handleCachePutError(
            exceptionOnPut, this.errorCache, key, SimpleService.TEST_EXCEPTION);
  }

  @Test
  public void putFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
    Object key = SimpleKeyGenerator.generateKey(0L);
    willThrow(exception).given(this.cache).put(key, 234L);

    this.simpleService.put(0L, 234L);
    verify(this.errorHandler).handleCachePutError(exception, this.cache, key, 234L);
  }

  @Test
  public void evictFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    Object key = SimpleKeyGenerator.generateKey(0L);
    willThrow(exception).given(this.cache).evict(key);

    this.simpleService.evict(0L);
    verify(this.errorHandler).handleCacheEvictError(exception, this.cache, key);
  }

  @Test
  public void clearFail() {
    UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
    willThrow(exception).given(this.cache).clear();

    this.simpleService.clear();
    verify(this.errorHandler).handleCacheClearError(exception, this.cache);
  }

  @Configuration
  @EnableCaching
  static class Config implements JCacheConfigurer {

    @Bean
    @Override
    public CacheManager cacheManager() {
      SimpleCacheManager cacheManager = new SimpleCacheManager();
      cacheManager.setCaches(Arrays.asList(mockCache(), mockErrorCache()));
      return cacheManager;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
      return mock(CacheErrorHandler.class);
    }

    @Bean
    public SimpleService simpleService() {
      return new SimpleService();
    }

    @Bean
    public Cache mockCache() {
      Cache cache = mock(Cache.class);
      given(cache.getName()).willReturn("test");
      return cache;
    }

    @Bean
    public Cache mockErrorCache() {
      Cache cache = mock(Cache.class);
      given(cache.getName()).willReturn("error");
      return cache;
    }
  }

  @CacheDefaults(cacheName = "test")
  public static class SimpleService {

    private static final IllegalStateException TEST_EXCEPTION = new IllegalStateException("Test exception");

    private AtomicLong counter = new AtomicLong();

    @CacheResult
    public Object get(long id) {
      return this.counter.getAndIncrement();
    }

    @CacheResult(exceptionCacheName = "error")
    public Object getFail(long id) {
      throw TEST_EXCEPTION;
    }

    @CachePut
    public void put(long id, @CacheValue Object object) {
    }

    @CacheRemove
    public void evict(long id) {
    }

    @CacheRemoveAll
    public void clear() {
    }
  }

}
