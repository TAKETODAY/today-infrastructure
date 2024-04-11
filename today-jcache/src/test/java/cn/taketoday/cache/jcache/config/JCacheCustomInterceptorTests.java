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

package cn.taketoday.cache.jcache.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.jcache.interceptor.AnnotatedJCacheableService;
import cn.taketoday.cache.jcache.interceptor.JCacheInterceptor;
import cn.taketoday.cache.jcache.interceptor.JCacheOperationSource;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.contextsupport.testfixture.jcache.JCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Stephane Nicoll
 */
class JCacheCustomInterceptorTests {

  protected ConfigurableApplicationContext ctx;

  protected JCacheableService<?> cs;

  protected Cache exceptionCache;

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().addBeanPostProcessor(
            new CacheInterceptorBeanPostProcessor(context.getBeanFactory()));
    context.register(EnableCachingConfig.class);
    context.refresh();
    this.ctx = context;
    cs = ctx.getBean("service", JCacheableService.class);
    exceptionCache = ctx.getBean("exceptionCache", Cache.class);
  }

  @AfterEach
  void tearDown() {
    ctx.close();
  }

  @Test
  void onlyOneInterceptorIsAvailable() {
    Map<String, JCacheInterceptor> interceptors = ctx.getBeansOfType(JCacheInterceptor.class);
    assertThat(interceptors).as("Only one interceptor should be defined").hasSize(1);
    JCacheInterceptor interceptor = interceptors.values().iterator().next();
    assertThat(interceptor.getClass()).as("Custom interceptor not defined").isEqualTo(TestCacheInterceptor.class);
  }

  @Test
  void customInterceptorAppliesWithRuntimeException() {
    Object o = cs.cacheWithException("id", true);
    // See TestCacheInterceptor
    assertThat(o).isEqualTo(55L);
  }

  @Test
  void customInterceptorAppliesWithCheckedException() {
    assertThatRuntimeException()
            .isThrownBy(() -> cs.cacheWithCheckedException("id", true))
            .withCauseExactlyInstanceOf(IOException.class);
  }

  @Configuration
  @EnableCaching
  static class EnableCachingConfig {

    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager cm = new SimpleCacheManager();
      cm.setCaches(Arrays.asList(
              defaultCache(),
              exceptionCache()));
      return cm;
    }

    @Bean
    public JCacheableService<?> service() {
      return new AnnotatedJCacheableService(defaultCache());
    }

    @Bean
    public Cache defaultCache() {
      return new ConcurrentMapCache("default");
    }

    @Bean
    public Cache exceptionCache() {
      return new ConcurrentMapCache("exception");
    }

  }

  static class CacheInterceptorBeanPostProcessor implements InitializationBeanPostProcessor {

    private final BeanFactory beanFactory;

    CacheInterceptorBeanPostProcessor(BeanFactory beanFactory) { this.beanFactory = beanFactory; }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (beanName.equals("jCacheInterceptor")) {
        JCacheInterceptor cacheInterceptor = new TestCacheInterceptor();
        cacheInterceptor.setCacheOperationSource(beanFactory.getBean(JCacheOperationSource.class));
        return cacheInterceptor;
      }
      return bean;
    }

  }

  /**
   * A test {@link JCacheInterceptor} that handles special exception types.
   */
  @SuppressWarnings("serial")
  static class TestCacheInterceptor extends JCacheInterceptor {

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
