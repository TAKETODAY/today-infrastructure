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

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.cache.annotation.CachingConfigurationSelector;
import infra.cache.annotation.EnableCaching;
import infra.cache.config.CacheManagementConfigUtils;
import infra.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor;
import infra.cache.jcache.interceptor.JCacheInterceptor;
import infra.cache.jcache.interceptor.JCacheOperationSource;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers the Framework infrastructure beans necessary
 * to enable proxy-based annotation-driven JSR-107 cache management.
 *
 * <p>Can safely be used alongside Framework's caching support.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @see CachingConfigurationSelector
 * @since 4.0
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyJCacheConfiguration extends AbstractJCacheConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)
  public BeanFactoryJCacheOperationSourceAdvisor cacheAdvisor(
          JCacheOperationSource jCacheOperationSource, JCacheInterceptor jCacheInterceptor) {

    BeanFactoryJCacheOperationSourceAdvisor advisor = new BeanFactoryJCacheOperationSourceAdvisor();
    advisor.setCacheOperationSource(jCacheOperationSource);
    advisor.setAdvice(jCacheInterceptor);
    if (this.enableCaching != null) {
      advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
    }
    return advisor;
  }

  @Component("jCacheInterceptor")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public JCacheInterceptor cacheInterceptor(JCacheOperationSource jCacheOperationSource) {
    JCacheInterceptor interceptor = new JCacheInterceptor(this.errorHandler);
    interceptor.setCacheOperationSource(jCacheOperationSource);
    return interceptor;
  }

}
