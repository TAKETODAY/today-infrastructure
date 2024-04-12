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

package cn.taketoday.cache.concurrent;

import java.util.concurrent.ConcurrentMap;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link FactoryBean} for easy configuration of a {@link ConcurrentMapCache}
 * when used within a Framework container. Can be configured through bean properties;
 * uses the assigned Framework bean name as the default cache name.
 *
 * <p>Useful for testing or simple caching scenarios, typically in combination
 * with {@link SimpleCacheManager} or
 * dynamically through {@link ConcurrentMapCacheManager}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConcurrentMapCacheFactoryBean
        implements FactoryBean<ConcurrentMapCache>, BeanNameAware, InitializingBean {

  private String name = "";

  @Nullable
  private ConcurrentMap<Object, Object> store;

  private boolean allowNullValues = true;

  @Nullable
  private ConcurrentMapCache cache;

  /**
   * Specify the name of the cache.
   * <p>Default is "" (empty String).
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Specify the ConcurrentMap to use as an internal store
   * (possibly pre-populated).
   * <p>Default is a standard {@link java.util.concurrent.ConcurrentHashMap}.
   */
  public void setStore(ConcurrentMap<Object, Object> store) {
    this.store = store;
  }

  /**
   * Set whether to allow {@code null} values
   * (adapting them to an internal null holder value).
   * <p>Default is "true".
   */
  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  @Override
  public void setBeanName(String beanName) {
    if (StringUtils.isEmpty(this.name)) {
      setName(beanName);
    }
  }

  @Override
  public void afterPropertiesSet() {
    this.cache = (this.store != null ? new ConcurrentMapCache(this.name, this.store, this.allowNullValues) :
            new ConcurrentMapCache(this.name, this.allowNullValues));
  }

  @Override
  @Nullable
  public ConcurrentMapCache getObject() {
    return this.cache;
  }

  @Override
  public Class<?> getObjectType() {
    return ConcurrentMapCache.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
