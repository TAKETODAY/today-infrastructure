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

package infra.cache.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.cache.CacheManager;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.NamedCacheResolver;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.cache.support.NoOpCacheManager;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AdviceMode;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.CacheTestUtils;
import infra.context.testfixture.cache.SomeCustomKeyGenerator;
import infra.context.testfixture.cache.SomeKeyGenerator;
import infra.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import infra.context.testfixture.cache.beans.CacheableService;
import infra.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class AspectJEnableCachingIsolatedTests {

  private ConfigurableApplicationContext ctx;

  private void load(Class<?>... config) {
    this.ctx = new AnnotationConfigApplicationContext(config);
  }

  @AfterEach
  public void closeContext() {
    if (this.ctx != null) {
      this.ctx.close();
    }
  }

  @Test
  public void testKeyStrategy() {
    load(EnableCachingConfig.class);
    AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
    assertThat(aspect.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator", KeyGenerator.class));
  }

  @Test
  public void testCacheErrorHandler() {
    load(EnableCachingConfig.class);
    AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
    assertThat(aspect.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
  }

  // --- local tests -------

  @Test
  public void singleCacheManagerBean() {
    load(SingleCacheManagerConfig.class);
  }

  @Test
  public void multipleCacheManagerBeans() {
    try {
      load(MultiCacheManagerConfig.class);
    }
    catch (NoUniqueBeanDefinitionException ex) {
      assertThat(ex.getMessage()).contains(
              "no CacheResolver specified and expected single matching CacheManager but found 2: cm1,cm2");
      assertThat(ex.getNumberOfBeansFound()).isEqualTo(2);
      assertThat(ex.getBeanNamesFound()).containsExactly("cm1", "cm2");
    }
  }

  @Test
  public void multipleCacheManagerBeans_implementsCachingConfigurer() {
    load(MultiCacheManagerConfigurer.class); // does not throw
  }

  @Test
  public void multipleCachingConfigurers() {
    try {
      load(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
    }
    catch (IllegalStateException ex) {
      assertThat(ex.getMessage().contains("implementations of CachingConfigurer")).isTrue();
    }
  }

  @Test
  public void noCacheManagerBeans() {
    try {
      load(EmptyConfig.class);
    }
    catch (NoSuchBeanDefinitionException ex) {
      assertThat(ex.getMessage()).contains("no CacheResolver specified");
    }
  }

  @Test
  public void emptyConfigSupport() {
    load(EmptyConfigSupportConfig.class);
    AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
    assertThat(aspect.getCacheResolver()).isNotNull();
    assertThat(aspect.getCacheResolver().getClass()).isEqualTo(SimpleCacheResolver.class);
    assertThat(((SimpleCacheResolver) aspect.getCacheResolver()).getCacheManager()).isSameAs(this.ctx.getBean(CacheManager.class));
  }

  @Test
  public void bothSetOnlyResolverIsUsed() {
    load(FullCachingConfig.class);

    AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
    assertThat(aspect.getCacheResolver()).isSameAs(this.ctx.getBean("cacheResolver"));
    assertThat(aspect.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class EnableCachingConfig implements CachingConfigurer {

    @Override
    @Bean
    public CacheManager cacheManager() {
      return CacheTestUtils.createSimpleCacheManager("testCache", "primary", "secondary");
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
      return new SomeKeyGenerator();
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
      return new SimpleCacheErrorHandler();
    }

    @Bean
    public KeyGenerator customKeyGenerator() {
      return new SomeCustomKeyGenerator();
    }

    @Bean
    public CacheManager customCacheManager() {
      return CacheTestUtils.createSimpleCacheManager("testCache");
    }
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class EmptyConfig {
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class SingleCacheManagerConfig {

    @Bean
    public CacheManager cm1() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class MultiCacheManagerConfig {

    @Bean
    public CacheManager cm1() {
      return new NoOpCacheManager();
    }

    @Bean
    public CacheManager cm2() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class MultiCacheManagerConfigurer implements CachingConfigurer {

    @Bean
    public CacheManager cm1() {
      return new NoOpCacheManager();
    }

    @Bean
    public CacheManager cm2() {
      return new NoOpCacheManager();
    }

    @Override
    public CacheManager cacheManager() {
      return cm1();
    }

    @Override
    public KeyGenerator keyGenerator() {
      return null;
    }
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class EmptyConfigSupportConfig implements CachingConfigurer {

    @Bean
    public CacheManager cm() {
      return new NoOpCacheManager();
    }

  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class FullCachingConfig implements CachingConfigurer {

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
  }
}
