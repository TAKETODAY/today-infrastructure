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

package infra.cache.jcache.config;

import java.util.function.Supplier;

import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.AbstractCachingConfiguration;
import infra.cache.interceptor.CacheResolver;
import infra.cache.jcache.interceptor.DefaultJCacheOperationSource;
import infra.cache.jcache.interceptor.JCacheOperationSource;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.lang.Nullable;
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
