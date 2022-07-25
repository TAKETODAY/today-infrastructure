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

package cn.taketoday.cache.aspectj;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.cache.config.CacheManagementConfigUtils;
import cn.taketoday.cache.jcache.config.AbstractJCacheConfiguration;
import cn.taketoday.cache.jcache.interceptor.JCacheOperationSource;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.stereotype.Component;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable AspectJ-based annotation-driven cache management for standard JSR-107
 * annotations.
 *
 * @author Stephane Nicoll
 * @see cn.taketoday.cache.annotation.EnableCaching
 * @see cn.taketoday.cache.annotation.CachingConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJJCacheConfiguration extends AbstractJCacheConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)
  public JCacheCacheAspect cacheAspect(JCacheOperationSource jCacheOperationSource) {
    JCacheCacheAspect cacheAspect = JCacheCacheAspect.aspectOf();
    cacheAspect.setCacheOperationSource(jCacheOperationSource);
    return cacheAspect;
  }

}
