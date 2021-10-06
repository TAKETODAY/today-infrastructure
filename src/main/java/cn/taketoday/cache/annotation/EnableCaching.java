/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.DefaultCacheManager;
import cn.taketoday.cache.interceptor.CacheEvictInterceptor;
import cn.taketoday.cache.interceptor.CacheExceptionResolver;
import cn.taketoday.cache.interceptor.CachePutInterceptor;
import cn.taketoday.cache.interceptor.CacheableInterceptor;
import cn.taketoday.cache.interceptor.DefaultCacheExceptionResolver;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;

/**
 * @author TODAY <br>
 * 2019-10-07 22:54
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(ProxyCachingConfiguration.class)
public @interface EnableCaching {

}

class ProxyCachingConfiguration {

  @MissingBean(type = CacheManager.class)
  DefaultCacheManager cacheManager() {
    return new DefaultCacheManager();
  }

  @Aspect
  @MissingBean
  @Advice(CachePut.class)
  CachePutInterceptor cachePutInterceptor(CacheManager cacheManager) {
    return new CachePutInterceptor(cacheManager);
  }

  @Aspect
  @MissingBean
  @Advice(Cacheable.class)
  CacheableInterceptor cacheableInterceptor(CacheManager cacheManager) {
    return new CacheableInterceptor(cacheManager);
  }

  @Aspect
  @MissingBean
  @Advice(CacheEvict.class)
  CacheEvictInterceptor cacheEvictInterceptor(CacheManager cacheManager) {
    return new CacheEvictInterceptor(cacheManager);
  }

  @MissingBean(type = CacheExceptionResolver.class)
  DefaultCacheExceptionResolver cacheExceptionResolver() {
    return new DefaultCacheExceptionResolver();
  }

}
