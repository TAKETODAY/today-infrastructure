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
import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} when mixed with Infra AOP.
 *
 * @author Phillip Webb
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5837">5837</a>
 */
@ExtendWith(InfraExtension.class)
class SpyBeanWithAopProxyTests {

  @SpyBean
  private DateService dateService;

  @Test
  void verifyShouldUseProxyTarget() throws Exception {
    Long d1 = this.dateService.getDate(false);
    Thread.sleep(200);
    Long d2 = this.dateService.getDate(false);
    assertThat(d1).isEqualTo(d2);
    then(this.dateService).should().getDate(false);
    then(this.dateService).should().getDate(eq(false));
    then(this.dateService).should().getDate(anyBoolean());
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
  public static class DateService {

    @Cacheable(cacheNames = "test")
    public Long getDate(boolean arg) {
      return System.nanoTime();
    }

  }

}
