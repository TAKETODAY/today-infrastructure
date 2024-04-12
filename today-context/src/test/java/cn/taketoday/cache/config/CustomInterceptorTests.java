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

package cn.taketoday.cache.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Stephane Nicoll
 */
class CustomInterceptorTests {

  protected ConfigurableApplicationContext ctx;

  protected CacheableService<?> cs;

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().addBeanPostProcessor(
            new CacheInterceptorBeanPostProcessor(context.getBeanFactory()));
    context.register(EnableCachingConfig.class);
    context.refresh();
    this.ctx = context;
    this.cs = ctx.getBean("service", CacheableService.class);
  }

  @AfterEach
  void tearDown() {
    this.ctx.close();
  }

  @Test
  void onlyOneInterceptorIsAvailable() {
    Map<String, CacheInterceptor> interceptors = this.ctx.getBeansOfType(CacheInterceptor.class);
    assertThat(interceptors).as("Only one interceptor should be defined").hasSize(1);
    CacheInterceptor interceptor = interceptors.values().iterator().next();
    assertThat(interceptor).as("Custom interceptor not defined").isInstanceOf(TestCacheInterceptor.class);
  }

  @Test
  void customInterceptorAppliesWithRuntimeException() {
    Object o = this.cs.throwUnchecked(0L);
    // See TestCacheInterceptor
    assertThat(o).isEqualTo(55L);
  }

  @Test
  void customInterceptorAppliesWithCheckedException() {
    assertThatThrownBy(() -> this.cs.throwChecked(0L))
            .isInstanceOf(RuntimeException.class)
            .hasCauseExactlyInstanceOf(IOException.class);
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

  }

  static class CacheInterceptorBeanPostProcessor implements InitializationBeanPostProcessor {

    private final BeanFactory beanFactory;

    CacheInterceptorBeanPostProcessor(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (beanName.equals("cacheInterceptor")) {
        CacheInterceptor cacheInterceptor = new TestCacheInterceptor();
        cacheInterceptor.setCacheManager(beanFactory.getBean(CacheManager.class));
        cacheInterceptor.setCacheOperationSource(beanFactory.getBean(CacheOperationSource.class));
        return cacheInterceptor;
      }
      return bean;
    }

  }

  /**
   * A test {@link CacheInterceptor} that handles special exception types.
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
