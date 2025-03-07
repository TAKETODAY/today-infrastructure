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

package infra.cache.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.cache.CacheManager;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.CachingConfigurer;
import infra.cache.annotation.EnableCaching;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheInterceptor;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.NamedCacheResolver;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.cache.support.NoOpCacheManager;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.testfixture.cache.AbstractCacheAnnotationTests;
import infra.context.testfixture.cache.CacheTestUtils;
import infra.context.testfixture.cache.SomeCustomKeyGenerator;
import infra.context.testfixture.cache.SomeKeyGenerator;
import infra.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import infra.context.testfixture.cache.beans.CacheableService;
import infra.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@code @EnableCaching} and its related
 * {@code @Configuration} classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class EnableCachingTests extends AbstractCacheAnnotationTests {

  /** hook into superclass suite of tests */
  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
  }

  @Test
  void keyStrategy() {
    CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
    assertThat(ci.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator", KeyGenerator.class));
  }

  @Test
  void cacheErrorHandler() {
    CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
    assertThat(ci.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
  }

  @Test
  void singleCacheManagerBean() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(SingleCacheManagerConfig.class);
    assertThatCode(ctx::refresh).doesNotThrowAnyException();
    ctx.close();
  }

  @Test
  void multipleCacheManagerBeans() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(MultiCacheManagerConfig.class);
    assertThatThrownBy(ctx::refresh)
            .isInstanceOfSatisfying(NoUniqueBeanDefinitionException.class, ex -> {
              assertThat(ex.getMessage()).contains(
                      "no CacheResolver specified and expected single matching CacheManager but found 2: cm1,cm2");
              assertThat(ex.getNumberOfBeansFound()).isEqualTo(2);
              assertThat(ex.getBeanNamesFound()).containsExactly("cm1", "cm2");
            }).hasNoCause();
  }

  @Test
  void multipleCacheManagerBeans_implementsCachingConfigurer() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(MultiCacheManagerConfigurer.class);
    assertThatCode(ctx::refresh).doesNotThrowAnyException();
    ctx.close();
  }

  @Test
  void multipleCachingConfigurers() {
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
    assertThatThrownBy(ctx::refresh)
            .hasMessageContaining("implementations of CachingConfigurer");
  }

  @Test
  void noCacheManagerBeans() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EmptyConfig.class);
    assertThatThrownBy(ctx::refresh)
            .isInstanceOf(NoSuchBeanDefinitionException.class)
            .hasMessageContaining("no CacheResolver specified")
            .hasMessageContaining(
                    "register a CacheManager bean or remove the @EnableCaching annotation from your configuration.")
            .hasNoCause();
  }

  @Test
  void emptyConfigSupport() {
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);
    CacheInterceptor ci = context.getBean(CacheInterceptor.class);
    assertThat(ci.getCacheResolver()).isInstanceOfSatisfying(SimpleCacheResolver.class, cacheResolver ->
            assertThat(cacheResolver.getCacheManager()).isSameAs(context.getBean(CacheManager.class)));
    context.close();
  }

  @Test
  void bothSetOnlyResolverIsUsed() {
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FullCachingConfig.class);
    CacheInterceptor ci = context.getBean(CacheInterceptor.class);
    assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
    assertThat(ci.getKeyGenerator()).isSameAs(context.getBean("keyGenerator"));
    context.close();
  }

  @Test
  void mutableKey() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableCachingConfig.class, ServiceWithMutableKey.class);
    ctx.refresh();

    ServiceWithMutableKey service = ctx.getBean(ServiceWithMutableKey.class);
    String result = service.find(new ArrayList<>(List.of("id")));
    assertThat(service.find(new ArrayList<>(List.of("id")))).isSameAs(result);
    ctx.close();
  }

  @Configuration
  @EnableCaching
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
  @EnableCaching
  static class EmptyConfig {
  }

  @Configuration
  @EnableCaching
  static class SingleCacheManagerConfig {

    @Bean
    public CacheManager cm1() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  @EnableCaching
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
  @EnableCaching
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
  @EnableCaching
  static class EmptyConfigSupportConfig implements CachingConfigurer {

    @Bean
    public CacheManager cm() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  @EnableCaching
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

  static class ServiceWithMutableKey {

    @Cacheable(value = "testCache", keyGenerator = "customKeyGenerator")
    public String find(Collection<String> id) {
      id.add("other");
      return id.toString();
    }
  }

}
