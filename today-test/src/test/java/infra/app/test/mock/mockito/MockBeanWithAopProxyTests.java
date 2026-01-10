/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import infra.cache.CacheManager;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.EnableCaching;
import infra.cache.concurrent.ConcurrentMapCacheManager;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.stereotype.Service;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Test {@link MockBean @MockBean} when mixed with Infra AOP.
 *
 * @author Phillip Webb
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5837">5837</a>
 */
@ExtendWith(InfraExtension.class)
class MockBeanWithAopProxyTests {

  @MockBean
  private DateService dateService;

  @Test
  void verifyShouldUseProxyTarget() {
    given(this.dateService.getDate(false)).willReturn(1L);
    Long d1 = this.dateService.getDate(false);
    assertThat(d1).isEqualTo(1L);
    given(this.dateService.getDate(false)).willReturn(2L);
    Long d2 = this.dateService.getDate(false);
    assertThat(d2).isEqualTo(2L);
    then(this.dateService).should(times(2)).getDate(false);
    then(this.dateService).should(times(2)).getDate(eq(false));
    then(this.dateService).should(times(2)).getDate(anyBoolean());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching(proxyTargetClass = true)
  @Import(DateService.class)
  static class Config {

    @Bean
    CacheResolver cacheResolver(CacheManager cacheManager) {
      SimpleCacheResolver resolver = new SimpleCacheResolver();
      resolver.setCacheManager(cacheManager);
      return resolver;
    }

    @Bean
    ConcurrentMapCacheManager cacheManager() {
      ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
      cacheManager.setCacheNames(Arrays.asList("test"));
      return cacheManager;
    }

  }

  @Service
  static class DateService {

    @Cacheable(cacheNames = "test")
    Long getDate(boolean argument) {
      return System.nanoTime();
    }

  }

}
