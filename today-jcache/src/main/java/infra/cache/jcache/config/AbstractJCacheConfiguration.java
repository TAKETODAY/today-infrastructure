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

package infra.cache.jcache.config;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.AbstractCachingConfiguration;
import infra.cache.interceptor.CacheResolver;
import infra.cache.jcache.interceptor.DefaultJCacheOperationSource;
import infra.cache.jcache.interceptor.JCacheOperationSource;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;

/**
 * Abstract JSR-107 specific {@code @Configuration} class providing common
 * structure for enabling JSR-107 annotation-driven cache management capability.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JCacheConfigurer
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

  @Nullable
  protected Supplier<CacheResolver> exceptionCacheResolver;

  @SuppressWarnings("NullAway")
  @Override
  protected void useCachingConfigurer(CachingConfigurerSupplier supplier) {
    super.useCachingConfigurer(supplier);
    this.exceptionCacheResolver = supplier.adapt(config -> {
      if (config instanceof JCacheConfigurer jCacheConfigurer) {
        return jCacheConfigurer.exceptionCacheResolver();
      }
      return null;
    });
  }

  @Component("jCacheOperationSource")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public JCacheOperationSource cacheOperationSource() {
    return new DefaultJCacheOperationSource(
            this.cacheManager, this.cacheResolver, this.exceptionCacheResolver, this.keyGenerator);
  }

}
