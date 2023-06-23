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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.interceptor.CacheOperationSource;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.testfixture.cache.CacheTestUtils;
import cn.taketoday.context.testfixture.cache.beans.CacheableService;
import cn.taketoday.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 */
public class CustomInterceptorTests {

  protected ConfigurableApplicationContext ctx;

  protected CacheableService<?> cs;

  @BeforeEach
  public void setup() {
    this.ctx = new AnnotationConfigApplicationContext(EnableCachingConfig.class);
    this.cs = ctx.getBean("service", CacheableService.class);
  }

  @AfterEach
  public void tearDown() {
    this.ctx.close();
  }

  @Test
  public void onlyOneInterceptorIsAvailable() {
    Map<String, CacheInterceptor> interceptors = this.ctx.getBeansOfType(CacheInterceptor.class);
    assertThat(interceptors.size()).as("Only one interceptor should be defined").isEqualTo(1);
    CacheInterceptor interceptor = interceptors.values().iterator().next();
    assertThat(interceptor.getClass()).as("Custom interceptor not defined").isEqualTo(TestCacheInterceptor.class);
  }

  @Test
  public void customInterceptorAppliesWithRuntimeException() {
    Object o = this.cs.throwUnchecked(0L);
    // See TestCacheInterceptor
    assertThat(o).isEqualTo(55L);
  }

  @Test
  public void customInterceptorAppliesWithCheckedException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                    this.cs.throwChecked(0L))
            .withCauseExactlyInstanceOf(IOException.class);
  }

  @Configuration
  @EnableCaching
  static class EnableCachingConfig {

    @Bean
    public CacheManager cacheManager() {
      return CacheTestUtils.createSimpleCacheManager("testCache", "primary", "secondary");
    }

    @Bean
    public CacheableService<?> service() {
      return new DefaultCacheableService();
    }

    @Bean
    public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
      CacheInterceptor cacheInterceptor = new TestCacheInterceptor();
      cacheInterceptor.setCacheManager(cacheManager());
      cacheInterceptor.setCacheOperationSources(cacheOperationSource);
      return cacheInterceptor;
    }
  }

  /**
   * A test {@link CacheInterceptor} that handles special exception
   * types.
   */
  @SuppressWarnings("serial")
  static class TestCacheInterceptor extends CacheInterceptor {

    @Override
    protected Object invokeOperation(CacheOperationInvoker invoker) {
      try {
        return super.invokeOperation(invoker);
      }
      catch (CacheOperationInvoker.ThrowableWrapper e) {
        Throwable original = e.getOriginal();
        if (original.getClass() == UnsupportedOperationException.class) {
          return 55L;
        }
        else {
          throw new CacheOperationInvoker.ThrowableWrapper(
                  new RuntimeException("wrapping original", original));
        }
      }
    }
  }

}
