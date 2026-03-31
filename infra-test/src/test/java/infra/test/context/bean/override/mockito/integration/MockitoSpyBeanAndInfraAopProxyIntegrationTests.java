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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.aop.support.AopUtils;
import infra.cache.CacheManager;
import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.EnableCaching;
import infra.cache.concurrent.ConcurrentMapCacheManager;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.util.AopTestUtils;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MockitoSpyBean @MockitoSpyBean} used in combination with Infra AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoBeanAndInfraAopProxyIntegrationTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoSpyBeanAndInfraAopProxyIntegrationTests {

  @MockitoSpyBean
  DateService dateService;

  @BeforeEach
  void resetCache() {
    // We have to clear the "test" cache before each test. Otherwise, method
    // invocations on the Infra AOP proxy will never make it to the Mockito spy.
    dateService.clearCache();
  }

  /**
   * Stubbing and verification for a Mockito spy that is wrapped in a Infra AOP
   * proxy should always work when performed via the ultimate target of the Infra
   * AOP proxy (i.e., the actual spy instance).
   */
  // We need to run this test at least twice to ensure the Mockito spy can be reused
  // across test method invocations without using @DirtestContext.
  @RepeatedTest(2)
  void stubAndVerifyOnUltimateTargetOfInfraAopProxy() {
    assertThat(AopUtils.isAopProxy(dateService)).as("is Infra AOP proxy").isTrue();
    DateService spy = AopTestUtils.getUltimateTargetObject(dateService);
    assertIsSpy(dateService, "ultimate target");

    given(spy.getDate(false)).willReturn(1L);
    Long date = dateService.getDate(false);
    assertThat(date).isOne();

    given(spy.getDate(false)).willReturn(2L);
    date = dateService.getDate(false);
    assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

    // Each of the following verifies times(1), because the AOP proxy caches the
    // original value and does not delegate to the spy on subsequent invocations.
    verify(spy, times(1)).getDate(false);
    verify(spy, times(1)).getDate(eq(false));
    verify(spy, times(1)).getDate(anyBoolean());
  }

  /**
   * Verification for a Mockito spy that is wrapped in a Infra AOP proxy should
   * always work when performed via the Infra AOP proxy. However, stubbing
   * does not currently work via the Infra AOP proxy.
   *
   * <p>Consequently, this test method supplies the ultimate target of the Infra
   * AOP proxy to stubbing calls, while supplying the Infra AOP proxy to verification
   * calls.
   */
  // We need to run this test at least twice to ensure the Mockito spy can be reused
  // across test method invocations without using @DirtestContext.
  @RepeatedTest(2)
  void stubOnUltimateTargetAndVerifyOnInfraAopProxy() {
    assertThat(AopUtils.isAopProxy(dateService)).as("is Infra AOP proxy").isTrue();
    assertIsSpy(dateService, "Infra AOP proxy");

    DateService spy = AopTestUtils.getUltimateTargetObject(dateService);
    given(spy.getDate(false)).willReturn(1L);
    Long date = dateService.getDate(false);
    assertThat(date).isOne();

    given(spy.getDate(false)).willReturn(2L);
    date = dateService.getDate(false);
    assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

    // Each of the following verifies times(1), because the AOP proxy caches the
    // original value and does not delegate to the spy on subsequent invocations.
    verify(dateService, times(1)).getDate(false);
    verify(dateService, times(1)).getDate(eq(false));
    verify(dateService, times(1)).getDate(anyBoolean());
  }

  /**
   * Ideally, both stubbing and verification should work transparently when a Mockito
   * spy is wrapped in a Infra AOP proxy. However, Mockito currently does not provide
   * support for transparent stubbing of a proxied spy. For example, implementing a
   * custom {@link org.mockito.plugins.MockResolver} will not result in successful
   * stubbing for a proxied mock.
   */
  @Disabled("Disabled until Mockito provides support for transparent stubbing of a proxied spy")
  // We need to run this test at least twice to ensure the Mockito spy can be reused
  // across test method invocations without using @DirtestContext.
  @RepeatedTest(2)
  void stubAndVerifyDirectlyOnInfraAopProxy() throws Exception {
    assertThat(AopUtils.isCglibProxy(dateService)).as("is Infra AOP CGLIB proxy").isTrue();
    assertIsSpy(dateService);

    doReturn(1L).when(dateService).getDate(false);
    Long date = dateService.getDate(false);
    assertThat(date).isOne();

    doReturn(2L).when(dateService).getDate(false);
    date = dateService.getDate(false);
    assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

    // Each of the following verifies times(1), because the AOP proxy caches the
    // original value and does not delegate to the spy on subsequent invocations.
    verify(dateService, times(1)).getDate(false);
    verify(dateService, times(1)).getDate(eq(false));
    verify(dateService, times(1)).getDate(anyBoolean());
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

    @CacheEvict(cacheNames = "test", allEntries = true)
    void clearCache() {
    }

  }

}
