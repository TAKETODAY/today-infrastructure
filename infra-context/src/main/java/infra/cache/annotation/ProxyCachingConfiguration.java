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

package infra.cache.annotation;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.cache.config.CacheManagementConfigUtils;
import infra.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import infra.cache.interceptor.CacheInterceptor;
import infra.cache.interceptor.CacheOperationSource;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers the Framework infrastructure beans necessary
 * to enable proxy-based annotation-driven cache management.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @see CachingConfigurationSelector
 * @since 4.0
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyCachingConfiguration extends AbstractCachingConfiguration {

  @Component(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor(
          CacheOperationSource cacheOperationSource, CacheInterceptor cacheInterceptor) {

    BeanFactoryCacheOperationSourceAdvisor advisor = new BeanFactoryCacheOperationSourceAdvisor();
    advisor.setCacheOperationSource(cacheOperationSource);
    advisor.setAdvice(cacheInterceptor);
    if (this.enableCaching != null) {
      advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
    }
    return advisor;
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public CacheOperationSource cacheOperationSource() {
    // Accept protected @Cacheable etc methods on CGLIB proxies, as of 6.0.
    return new AnnotationCacheOperationSource(false);
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
    CacheInterceptor interceptor = new CacheInterceptor();
    interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
    interceptor.setCacheOperationSource(cacheOperationSource);
    return interceptor;
  }

}
