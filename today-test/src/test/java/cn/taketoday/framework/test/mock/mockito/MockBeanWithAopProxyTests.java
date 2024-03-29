/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.stereotype.Service;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

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
