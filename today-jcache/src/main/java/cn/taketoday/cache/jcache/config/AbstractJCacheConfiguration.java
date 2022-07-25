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

package cn.taketoday.cache.jcache.config;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.cache.annotation.AbstractCachingConfiguration;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.jcache.interceptor.DefaultJCacheOperationSource;
import cn.taketoday.cache.jcache.interceptor.JCacheOperationSource;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;

/**
 * Abstract JSR-107 specific {@code @Configuration} class providing common
 * structure for enabling JSR-107 annotation-driven cache management capability.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see JCacheConfigurer
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

  @Nullable
  protected Supplier<CacheResolver> exceptionCacheResolver;

  @Override
  protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
    super.useCachingConfigurer(cachingConfigurerSupplier);
    this.exceptionCacheResolver = cachingConfigurerSupplier.adapt(config -> {
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
