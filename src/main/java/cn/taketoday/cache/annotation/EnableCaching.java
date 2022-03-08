/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.support.annotation.Advice;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.support.CaffeineCacheManager;
import cn.taketoday.cache.support.DefaultCacheManager;
import cn.taketoday.cache.interceptor.CacheEvictInterceptor;
import cn.taketoday.cache.interceptor.CacheExceptionResolver;
import cn.taketoday.cache.interceptor.CacheExpressionOperations;
import cn.taketoday.cache.interceptor.CachePutInterceptor;
import cn.taketoday.cache.interceptor.CacheableInterceptor;
import cn.taketoday.cache.interceptor.DefaultCacheExceptionResolver;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Singleton;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY <br>
 * 2019-10-07 22:54
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(ProxyCachingConfiguration.class)
public @interface EnableCaching {

}

@Configuration(proxyBeanMethods = false)
@DisableAllDependencyInjection
class ProxyCachingConfiguration {

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(CacheManager.class)
  CacheManager cacheManager() {
    if (ClassUtils.isPresent("com.github.benmanes.caffeine.cache.Caffeine")) {
      return new CaffeineCacheManager();
    }
    return new DefaultCacheManager();
  }

  @Aspect
  @Component
  @Advice(CachePut.class)
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  CachePutInterceptor cachePutInterceptor(CacheManager cacheManager, CacheExpressionOperations operations) {
    CachePutInterceptor cachePutInterceptor = new CachePutInterceptor(cacheManager);
    cachePutInterceptor.setExpressionOperations(operations);
    return cachePutInterceptor;
  }

  @Aspect
  @Component
  @Advice(Cacheable.class)
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  CacheableInterceptor cacheableInterceptor(CacheManager cacheManager, CacheExpressionOperations operations) {
    CacheableInterceptor cacheableInterceptor = new CacheableInterceptor(cacheManager);
    cacheableInterceptor.setExpressionOperations(operations);
    return cacheableInterceptor;
  }

  @Aspect
  @Component
  @ConditionalOnMissingBean
  @Advice(CacheEvict.class)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  CacheEvictInterceptor cacheEvictInterceptor(
          CacheManager cacheManager, CacheExpressionOperations operations) {
    CacheEvictInterceptor cacheEvictInterceptor = new CacheEvictInterceptor(cacheManager);
    cacheEvictInterceptor.setExpressionOperations(operations);
    return cacheEvictInterceptor;
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(CacheExceptionResolver.class)
  DefaultCacheExceptionResolver cacheExceptionResolver() {
    return new DefaultCacheExceptionResolver();
  }

  @Singleton
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  CacheExpressionOperations cacheExpressionOperations() {
    return new CacheExpressionOperations();
  }

}
