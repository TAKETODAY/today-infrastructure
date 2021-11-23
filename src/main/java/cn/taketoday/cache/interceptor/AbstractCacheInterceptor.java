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
package cn.taketoday.cache.interceptor;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.Method;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.NoSuchCacheException;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-02-27 19:03
 */
public abstract class AbstractCacheInterceptor
        extends CacheOperations implements MethodInterceptor, Ordered, ApplicationContextAware {

  private CacheManager cacheManager;
  private final OrderedSupport ordered = new OrderedSupport();

  protected CacheExpressionOperations expressionOperations;

  public AbstractCacheInterceptor() { }

  public AbstractCacheInterceptor(CacheManager cacheManager) {
    setCacheManager(cacheManager);
  }

  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public final CacheManager getCacheManager() {
    return cacheManager;
  }

  @Override
  public int getOrder() {
    return ordered.getOrder();
  }

  public void setOrder(int order) {
    ordered.setOrder(order);
  }

  /**
   * Prepare {@link Cache} name
   *
   * @param method Target method
   * @param cacheName {@link CacheConfig#cacheName()}
   * @return A not empty cache name
   */
  protected String prepareCacheName(Method method, String cacheName) {
    // if cache name is empty use declaring class full name
    if (StringUtils.isEmpty(cacheName)) {
      return method.getDeclaringClass().getName();
    }
    return cacheName;
  }

  protected Cache getCache(String name, CacheConfig cacheConfig) {
    return getCacheManager().getCache(name, cacheConfig);
  }

  /**
   * Obtain a Target method's {@link Cache} object
   *
   * @param method Target method
   * @param cacheConfig {@link CacheConfig}
   * @return {@link Cache}
   * @throws NoSuchCacheException If there isn't a {@link Cache}
   */
  protected Cache obtainCache(Method method, CacheConfig cacheConfig) {
    String name = prepareCacheName(method, cacheConfig.cacheName());
    Cache cache = getCache(name, cacheConfig);
    if (cache == null) {
      throw new NoSuchCacheException(name);
    }
    return cache;
  }

  /**
   * @see cn.taketoday.cache.annotation.ProxyCachingConfiguration
   */
  @Override
  public void setApplicationContext(ApplicationContext context) {
    if (getCacheManager() == null) {
      setCacheManager(context.getBean(CacheManager.class));
    }
    if (getExceptionResolver() == null) {
      setExceptionResolver(context.getBean(CacheExceptionResolver.class));
    }

    Assert.state(getCacheManager() != null, "You must provide a 'CacheManager'");
    Assert.state(getExceptionResolver() != null, "You must provide a 'CacheExceptionResolver'");
  }

  public void setExpressionOperations(CacheExpressionOperations expressionOperations) {
    this.expressionOperations = expressionOperations;
  }

  public CacheExpressionOperations getExpressionOperations() {
    return expressionOperations;
  }

}
