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

package cn.taketoday.cache.interceptor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CacheProxyFactoryBean}.
 *
 * @author John Blum
 * @author Juergen Hoeller
 */
public class CacheProxyFactoryBeanTests {

  @Test
  public void configurationClassWithCacheProxyFactoryBean() {
    try (AnnotationConfigApplicationContext applicationContext =
            new AnnotationConfigApplicationContext(CacheProxyFactoryBeanConfiguration.class)) {
      Greeter greeter = applicationContext.getBean("greeter", Greeter.class);
      assertThat(greeter).isNotNull();
      assertThat(greeter.isCacheMiss()).isFalse();
      assertThat(greeter.greet("John")).isEqualTo("Hello John!");
      assertThat(greeter.isCacheMiss()).isTrue();
      assertThat(greeter.greet("Jon")).isEqualTo("Hello Jon!");
      assertThat(greeter.isCacheMiss()).isTrue();
      assertThat(greeter.greet("John")).isEqualTo("Hello John!");
      assertThat(greeter.isCacheMiss()).isFalse();
      assertThat(greeter.greet()).isEqualTo("Hello World!");
      assertThat(greeter.isCacheMiss()).isTrue();
      assertThat(greeter.greet()).isEqualTo("Hello World!");
      assertThat(greeter.isCacheMiss()).isFalse();
    }
  }

  @Configuration
  @EnableCaching
  static class CacheProxyFactoryBeanConfiguration {

    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager("Greetings");
    }

    @Bean
    CacheProxyFactoryBean greeter() {
      CacheProxyFactoryBean factoryBean = new CacheProxyFactoryBean();
      factoryBean.setCacheOperationSources(newCacheOperationSource("greet", newCacheOperation("Greetings")));
      factoryBean.setTarget(new SimpleGreeter());
      return factoryBean;
    }

    CacheOperationSource newCacheOperationSource(String methodName, CacheOperation... cacheOperations) {
      NameMatchCacheOperationSource cacheOperationSource = new NameMatchCacheOperationSource();
      cacheOperationSource.addCacheMethod(methodName, Arrays.asList(cacheOperations));
      return cacheOperationSource;
    }

    CacheableOperation newCacheOperation(String cacheName) {
      CacheableOperation.Builder builder = new CacheableOperation.Builder();
      builder.setCacheManager("cacheManager");
      builder.setCacheName(cacheName);
      return builder.build();
    }
  }

  interface Greeter {

    default boolean isCacheHit() {
      return !isCacheMiss();
    }

    boolean isCacheMiss();

    void setCacheMiss();

    default String greet() {
      return greet("World");
    }

    default String greet(String name) {
      setCacheMiss();
      return String.format("Hello %s!", name);
    }
  }

  static class SimpleGreeter implements Greeter {

    private final AtomicBoolean cacheMiss = new AtomicBoolean();

    @Override
    public boolean isCacheMiss() {
      return this.cacheMiss.getAndSet(false);
    }

    @Override
    public void setCacheMiss() {
      this.cacheMiss.set(true);
    }
  }

}
