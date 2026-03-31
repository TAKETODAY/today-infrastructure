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

package infra.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.aop.support.AopUtils;
import infra.cache.CacheManager;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.EnableCaching;
import infra.cache.concurrent.ConcurrentMapCacheManager;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MockitoBean @MockitoBean} used in combination with Infra AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoSpyBeanAndInfraAopProxyIntegrationTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoBeanAndInfraAopProxyIntegrationTests {

  @MockitoBean
  DateService dateService;

  /**
   * Since the {@code BeanOverrideBeanFactoryPostProcessor} always registers a
   * manual singleton for a {@code @MockitoBean} mock, the mock that ends up
   * in the application context should not be proxied by Infra AOP (since
   * BeanPostProcessors are never applied to manually registered singletons).
   *
   * <p>In other words, this test effectively verifies that the mock is a
   * standard Mockito mock which does <strong>not</strong> have
   * {@link Cacheable @Cacheable} applied to it.
   */
  @RepeatedTest(2)
  void mockShouldNotBeAnAopProxy() {
    assertThat(AopUtils.isAopProxy(dateService)).as("is Infra AOP proxy").isFalse();
    assertIsMock(dateService);

    given(dateService.getDate(false)).willReturn(1L);
    Long date = dateService.getDate(false);
    assertThat(date).isOne();

    given(dateService.getDate(false)).willReturn(2L);
    date = dateService.getDate(false);
    assertThat(date).isEqualTo(2L);

    verify(dateService, times(2)).getDate(false);
    verify(dateService, times(2)).getDate(eq(false));
    verify(dateService, times(2)).getDate(anyBoolean());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching(proxyTargetClass = true)
  @Import(DateService.class)
  static class Config {

    @Bean
    CacheResolver cacheResolver(CacheManager cacheManager) {
      return new SimpleCacheResolver(cacheManager);
    }

    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager("test");
    }
  }

  static class DateService {

    @Cacheable("test")
    Long getDate(boolean argument) {
      return System.nanoTime();
    }
  }

}
