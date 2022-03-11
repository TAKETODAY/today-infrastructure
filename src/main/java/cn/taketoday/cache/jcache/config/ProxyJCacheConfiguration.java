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

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.cache.annotation.CachingConfigurationSelector;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.config.CacheManagementConfigUtils;
import cn.taketoday.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor;
import cn.taketoday.cache.jcache.interceptor.JCacheInterceptor;
import cn.taketoday.cache.jcache.interceptor.JCacheOperationSource;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;

/**
 * {@code @Configuration} class that registers the Framework infrastructure beans necessary
 * to enable proxy-based annotation-driven JSR-107 cache management.
 *
 * <p>Can safely be used alongside Framework's caching support.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see EnableCaching
 * @see CachingConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyJCacheConfiguration extends AbstractJCacheConfiguration {

  @Bean(name = CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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

  @Bean(name = "jCacheInterceptor")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public JCacheInterceptor cacheInterceptor(JCacheOperationSource jCacheOperationSource) {
    JCacheInterceptor interceptor = new JCacheInterceptor(this.errorHandler);
    interceptor.setCacheOperationSource(jCacheOperationSource);
    return interceptor;
  }

}
