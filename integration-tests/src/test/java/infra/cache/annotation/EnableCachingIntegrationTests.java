/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.annotation;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import infra.aop.Advisor;
import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.cache.CacheManager;
import infra.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import infra.cache.support.NoOpCacheManager;
import infra.context.annotation.AdviceMode;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.NestedRuntimeException;
import infra.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the @EnableCaching annotation.
 *
 * @author Chris Beams
 * @since 4.0
 */
@SuppressWarnings("resource")
class EnableCachingIntegrationTests {

  @Test
  void repositoryIsClassBasedCacheProxy() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, ProxyTargetClassCachingConfig.class);
    ctx.refresh();

    assertCacheProxying(ctx);
    assertThat(AopUtils.isCglibProxy(ctx.getBean(FooRepository.class))).isTrue();
  }

  @Test
  void repositoryUsesAspectJAdviceMode() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, AspectJCacheConfig.class);
    // this test is a bit fragile, but gets the job done, proving that an
    // attempt was made to look up the AJ aspect. It's due to classpath issues
    // in .integration-tests that it's not found.
    assertThatExceptionOfType(NestedRuntimeException.class)
            .isThrownBy(ctx::refresh)
            .satisfies(ex -> {
              assertThat(ex.getNestedMessage()).contains("AspectJCachingConfiguration");
            });
  }

  private void assertCacheProxying(AnnotationConfigApplicationContext ctx) {
    FooRepository repo = ctx.getBean(FooRepository.class);
    assertThat(isCacheProxy(repo)).isTrue();
  }

  private boolean isCacheProxy(FooRepository repo) {
    if (AopUtils.isAopProxy(repo)) {
      for (Advisor advisor : ((Advised) repo).getAdvisors()) {
        if (advisor instanceof BeanFactoryCacheOperationSourceAdvisor) {
          return true;
        }
      }
    }
    return false;
  }

  @Configuration
  @EnableCaching(proxyTargetClass = true)
  static class ProxyTargetClassCachingConfig {

    @Bean
    CacheManager mgr() {
      return new NoOpCacheManager();
    }
  }

  @Configuration
  static class Config {

    @Bean
    FooRepository fooRepository() {
      return new DummyFooRepository();
    }
  }

  @Configuration
  @EnableCaching(mode = AdviceMode.ASPECTJ)
  static class AspectJCacheConfig {

    @Bean
    CacheManager cacheManager() {
      return new NoOpCacheManager();
    }
  }

  interface FooRepository {

    List<Object> findAll();
  }

  @Repository
  static class DummyFooRepository implements FooRepository {

    @Override
    @Cacheable("primary")
    public List<Object> findAll() {
      return Collections.emptyList();
    }
  }

}
