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

package infra.cache.interceptor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.cache.annotation.EnableCaching;
import infra.cache.concurrent.ConcurrentMapCacheManager;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

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
    var applicationContext =
            new AnnotationConfigApplicationContext(CacheProxyFactoryBeanConfiguration.class);
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
