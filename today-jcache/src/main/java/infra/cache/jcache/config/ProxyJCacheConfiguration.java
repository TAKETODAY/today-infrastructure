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
