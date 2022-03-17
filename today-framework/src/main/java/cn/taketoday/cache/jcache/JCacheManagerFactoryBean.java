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

package cn.taketoday.cache.jcache;

import java.net.URI;
import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.Caching;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;

/**
 * {@link FactoryBean} for a JCache {@link CacheManager javax.cache.CacheManager},
 * obtaining a pre-defined {@code CacheManager} by name through the standard
 * JCache {@link Caching javax.cache.Caching} class.
 *
 * <p>Note: This class has been updated for JCache 1.0.
 *
 * @author Juergen Hoeller
 * @see javax.cache.Caching#getCachingProvider()
 * @see javax.cache.spi.CachingProvider#getCacheManager()
 * @since 4.0
 */
public class JCacheManagerFactoryBean
        implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

  @Nullable
  private URI cacheManagerUri;

  @Nullable
  private Properties cacheManagerProperties;

  @Nullable
  private ClassLoader beanClassLoader;

  @Nullable
  private CacheManager cacheManager;

  /**
   * Specify the URI for the desired {@code CacheManager}.
   * <p>Default is {@code null} (i.e. JCache's default).
   */
  public void setCacheManagerUri(@Nullable URI cacheManagerUri) {
    this.cacheManagerUri = cacheManagerUri;
  }

  /**
   * Specify properties for the to-be-created {@code CacheManager}.
   * <p>Default is {@code null} (i.e. no special properties to apply).
   *
   * @see javax.cache.spi.CachingProvider#getCacheManager(URI, ClassLoader, Properties)
   */
  public void setCacheManagerProperties(@Nullable Properties cacheManagerProperties) {
    this.cacheManagerProperties = cacheManagerProperties;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void afterPropertiesSet() {
    this.cacheManager = Caching.getCachingProvider().getCacheManager(
            this.cacheManagerUri, this.beanClassLoader, this.cacheManagerProperties);
  }

  @Override
  @Nullable
  public CacheManager getObject() {
    return this.cacheManager;
  }

  @Override
  public Class<?> getObjectType() {
    return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() {
    if (this.cacheManager != null) {
      this.cacheManager.close();
    }
  }

}
