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

package cn.taketoday.cache.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import cn.taketoday.cache.support.NoOpCacheManager;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/12 21:00
 */
public class EnableCachingIntegrationTests {

  @Test
  void repositoryIsClassBasedCacheProxy() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, ProxyTargetClassCachingConfig.class);
    ctx.refresh();

    assertCacheProxying(ctx);
    assertThat(AopUtils.isCglibProxy(ctx.getBean(FooRepository.class))).isTrue();
  }

  @Test
  @Disabled("need AspectJCachingConfiguration")
  void repositoryUsesAspectJAdviceMode() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class, AspectJCacheConfig.class);
    // this test is a bit fragile, but gets the job done, proving that an
    // attempt was made to look up the AJ aspect. It's due to classpath issues
    // in .integration-tests that it's not found.
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(ctx::refresh)
            .withMessageContaining("AspectJCachingConfiguration");
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
