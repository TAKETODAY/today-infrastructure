/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.annotation;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.cache.config.CacheManagementConfigUtils;
import cn.taketoday.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.cache.interceptor.CacheOperationSource;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;

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

  @Bean(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
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

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public CacheOperationSource cacheOperationSource() {
    // Accept protected @Cacheable etc methods on CGLIB proxies, as of 6.0.
    return new AnnotationCacheOperationSource(false);
  }

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
    CacheInterceptor interceptor = new CacheInterceptor();
    interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
    interceptor.setCacheOperationSource(cacheOperationSource);
    return interceptor;
  }

}
