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

package infra.cache.aspectj;

import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.AbstractCachingConfiguration;
import infra.cache.config.CacheManagementConfigUtils;
import infra.cache.annotation.CachingConfigurationSelector;
import infra.cache.annotation.EnableCaching;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers the infrastructure beans
 * necessary to enable AspectJ-based annotation-driven cache management.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see EnableCaching
 * @see CachingConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJCachingConfiguration extends AbstractCachingConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(name = CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)
  public AnnotationCacheAspect cacheAspect() {
    AnnotationCacheAspect cacheAspect = AnnotationCacheAspect.aspectOf();
    cacheAspect.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
    return cacheAspect;
  }

}
