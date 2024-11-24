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

package infra.cache.jcache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import infra.beans.factory.annotation.Autowired;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.SimpleKeyGenerator;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.AbstractCacheAnnotationTests;
import infra.context.testfixture.cache.SomeCustomKeyGenerator;
import infra.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import infra.context.testfixture.cache.beans.CacheableService;
import infra.context.testfixture.cache.beans.DefaultCacheableService;
import infra.transaction.support.TransactionTemplate;
import infra.transaction.testfixture.CallCountingTransactionManager;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class JCacheEhCacheAnnotationTests extends AbstractCacheAnnotationTests {

  private final TransactionTemplate txTemplate = new TransactionTemplate(new CallCountingTransactionManager());

  private CacheManager jCacheManager;

  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("cachingProvider", getCachingProvider());
    context.register(EnableCachingConfig.class);
    context.refresh();
    jCacheManager = context.getBean("jCacheManager", CacheManager.class);
    return context;
  }

  protected CachingProvider getCachingProvider() {
    return Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
  }

  @AfterEach
  public void shutdown() {
    if (jCacheManager != null) {
      jCacheManager.close();
    }
  }

  @Override
  @Test
  @Disabled("Multi cache manager support to be added")
  public void testCustomCacheManager() {
  }

  @Test
  public void testEvictWithTransaction() {
    txTemplate.executeWithoutResult(s -> testEvict(this.cs, false));
  }

  @Test
  public void testEvictEarlyWithTransaction() {
    txTemplate.executeWithoutResult(s -> testEvictEarly(this.cs));
  }

  @Test
  public void testEvictAllWithTransaction() {
    txTemplate.executeWithoutResult(s -> testEvictAll(this.cs, false));
  }

  @Test
  public void testEvictAllEarlyWithTransaction() {
    txTemplate.executeWithoutResult(s -> testEvictAllEarly(this.cs));
  }

  @Configuration
  @EnableCaching
  static class EnableCachingConfig implements CachingConfigurer {

    @Autowired
    CachingProvider cachingProvider;

    @Override
    @Bean
    public infra.cache.CacheManager cacheManager() {
      JCacheCacheManager cm = new JCacheCacheManager(jCacheManager());
      cm.setTransactionAware(true);
      return cm;
    }

    @Bean
    public CacheManager jCacheManager() {
      CacheManager cacheManager = this.cachingProvider.getCacheManager();
      MutableConfiguration<Object, Object> mutableConfiguration = new MutableConfiguration<>();
      mutableConfiguration.setStoreByValue(false);  // otherwise value has to be Serializable
      cacheManager.createCache("testCache", mutableConfiguration);
      cacheManager.createCache("primary", mutableConfiguration);
      cacheManager.createCache("secondary", mutableConfiguration);
      return cacheManager;
    }

    @Bean
    public CacheableService<?> service() {
      return new DefaultCacheableService();
    }

    @Bean
    public CacheableService<?> classService() {
      return new AnnotatedClassCacheableService();
    }

    @Override
    @Bean
    public KeyGenerator keyGenerator() {
      return new SimpleKeyGenerator();
    }

    @Bean
    public KeyGenerator customKeyGenerator() {
      return new SomeCustomKeyGenerator();
    }
  }

}
