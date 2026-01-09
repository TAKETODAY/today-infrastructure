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

package infra.cache.aspectj;

import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.AbstractCachingConfiguration;
import infra.cache.annotation.CachingConfigurationSelector;
import infra.cache.annotation.EnableCaching;
import infra.cache.config.CacheManagementConfigUtils;
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
