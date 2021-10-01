/*
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

package cn.taketoday.beans.factory;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.Assert;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Default SingletonBeanRegistry implementation
 *
 * <p>
 * <b>not a thread-safe implementation</b>
 * Beans are prepared at startup and generally do not add or modify beans at runtime
 * </p>
 *
 * @author TODAY 2021/10/1 22:47
 * @since 4.0
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /** Map of bean instance, keyed by bean name */
  private final HashMap<String, Object> singletons = new HashMap<>(128);

  @Override
  public void registerSingleton(final String name, final Object singleton) {
    Assert.notNull(name, "Bean name must not be null");
    Assert.notNull(singleton, "Singleton object must not be null");
    final Object oldBean = singletons.put(name, singleton);
    if (oldBean == null) {
      if (log.isDebugEnabled()) {
        log.debug("Register Singleton: [{}] = [{}]", name, ObjectUtils.toHexString(singleton));
      }
    }
    else if (oldBean != singleton) {
      log.info("Refresh Singleton: [{}] = [{}] old bean: [{}] ",
               name, ObjectUtils.toHexString(singleton), ObjectUtils.toHexString(oldBean));
    }
  }

  @Override
  public void registerSingleton(Object bean) {
    registerSingleton(createBeanName(bean.getClass()), bean);
  }

  @Override
  public Map<String, Object> getSingletons() {
    return singletons;
  }

  @Override
  public Object getSingleton(String name) {
    return singletons.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(final Class<T> requiredType) {
    final String maybe = createBeanName(requiredType);
    final Object singleton = getSingleton(maybe);
    if (singleton == null) {
      final Map<String, Object> singletons = getSingletons();
      for (final Object value : singletons.values()) {
        if (requiredType.isInstance(value)) {
          return (T) value;
        }
      }
    }
    else if (requiredType.isInstance(singleton)) {
      return (T) singleton;
    }
    return null;
  }

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type
   *         type
   *
   * @return bean name
   *
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return ClassUtils.getShortName(type);
  }

  @Override
  public void removeSingleton(String name) {
    singletons.remove(name);
  }

  @Override
  public boolean containsSingleton(String name) {
    return singletons.containsKey(name);
  }

}
