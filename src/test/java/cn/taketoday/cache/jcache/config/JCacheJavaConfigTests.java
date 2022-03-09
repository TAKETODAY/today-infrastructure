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

package cn.taketoday.cache.jcache.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.NamedCacheResolver;
import cn.taketoday.cache.interceptor.SimpleCacheErrorHandler;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.cache.jcache.interceptor.AnnotatedJCacheableService;
import cn.taketoday.cache.jcache.interceptor.DefaultJCacheOperationSource;
import cn.taketoday.cache.jcache.interceptor.JCacheInterceptor;
import cn.taketoday.cache.support.NoOpCacheManager;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.contextsupport.testfixture.cache.SomeKeyGenerator;
import cn.taketoday.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;
import cn.taketoday.contextsupport.testfixture.jcache.JCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class JCacheJavaConfigTests extends AbstractJCacheAnnotationTests {

  @Override
  protected ApplicationContext getApplicationContext() {
    return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
  }

  @Test
  public void fullCachingConfig() throws Exception {
    AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(FullCachingConfig.class);

    DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
    assertThat(cos.getKeyGenerator()).isSameAs(context.getBean(KeyGenerator.class));
    assertThat(cos.getCacheResolver()).isSameAs(context.getBean("cacheResolver", CacheResolver.class));
    assertThat(cos.getExceptionCacheResolver()).isSameAs(context.getBean("exceptionCacheResolver", CacheResolver.class));
    JCacheInterceptor interceptor = context.getBean(JCacheInterceptor.class);
    assertThat(interceptor.getErrorHandler()).isSameAs(context.getBean("errorHandler", CacheErrorHandler.class));
    context.close();
  }

  @Test
  public void emptyConfigSupport() {
    ConfigurableApplicationContext context =
            new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);

    DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
    assertThat(cos.getCacheResolver()).isNotNull();
    assertThat(cos.getCacheResolver().getClass()).isEqualTo(SimpleCacheResolver.class);
    assertThat(((SimpleCacheResolver) cos.getCacheResolver()).getCacheManager()).isSameAs(context.getBean(CacheManager.class));
    assertThat(cos.getExceptionCacheResolver()).isNull();
    context.close();
  }

  @Test
  public void bothSetOnlyResolverIsUsed() {
    ConfigurableApplicationContext context =
            new AnnotationConfigApplicationContext(FullCachingConfigSupport.class);

    DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
    assertThat(cos.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
    assertThat(cos.getKeyGenerator()).isSameAs(context.getBean("keyGenerator"));
    assertThat(cos.getExceptionCacheResolver()).isSameAs(context.getBean("exceptionCacheResolver"));
    context.close();
  }

  @Test
  public void exceptionCacheResolverLazilyRequired() {
    try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(NoExceptionCacheResolverConfig.class)) {
      DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
      assertThat(cos.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));

      JCacheableService<?> service = context.getBean(JCacheableService.class);
      service.cache("id");

      // This call requires the cache manager to be set
      assertThatIllegalStateException().isThrownBy(() ->
              service.cacheWithException("test", false));
    }
  }

  @Configuration
  @EnableCaching
  public static class EnableCachingConfig {

    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager cm = new SimpleCacheManager();
      cm.setCaches(Arrays.asList(
              defaultCache(),
              new ConcurrentMapCache("primary"),
              new ConcurrentMapCache("secondary"),
              new ConcurrentMapCache("exception")));
      return cm;
    }

    @Bean
    public JCacheableService<?> cacheableService() {
      return new AnnotatedJCacheableService(defaultCache());
    }

    @Bean
    public Cache defaultCache() {
      return new ConcurrentMapCache("default");
    }
  }

  @Configuration
  @EnableCaching
  public static class FullCachingConfig implements JCacheConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return new NoOpCacheManager();
    }

    @Override
    @Bean
    public KeyGenerator keyGenerator() {
      return new SimpleKeyGenerator();
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
      return new SimpleCacheErrorHandler();
    }

    @Override
    @Bean
    public CacheResolver cacheResolver() {
      return new SimpleCacheResolver(cacheManager());
    }

    @Override
    @Bean
    public CacheResolver exceptionCacheResolver() {
      return new SimpleCacheResolver(cacheManager());
    }
  }

  @Configuration
  @EnableCaching
  public static class EmptyConfigSupportConfig implements JCacheConfigurer {
    @Bean
    public CacheManager cm() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  @EnableCaching
  static class FullCachingConfigSupport implements JCacheConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return new NoOpCacheManager();
    }

    @Override
    @Bean
    public KeyGenerator keyGenerator() {
      return new SomeKeyGenerator();
    }

    @Override
    @Bean
    public CacheResolver cacheResolver() {
      return new NamedCacheResolver(cacheManager(), "foo");
    }

    @Override
    @Bean
    public CacheResolver exceptionCacheResolver() {
      return new NamedCacheResolver(cacheManager(), "exception");
    }
  }

  @Configuration
  @EnableCaching
  static class NoExceptionCacheResolverConfig implements JCacheConfigurer {

    @Override
    @Bean
    public CacheResolver cacheResolver() {
      return new NamedCacheResolver(new ConcurrentMapCacheManager(), "default");
    }

    @Bean
    public JCacheableService<?> cacheableService() {
      return new AnnotatedJCacheableService(new ConcurrentMapCache("default"));
    }
  }

}
