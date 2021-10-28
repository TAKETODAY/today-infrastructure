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

package cn.taketoday.context.event;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author TODAY 2021/10/7 15:20
 * @since 4.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultApplicationEventPublisher implements ApplicationEventPublisher, BeanFactoryAware {
  private static final Logger log = LoggerFactory.getLogger(DefaultApplicationEventPublisher.class);

  /** application listeners **/
  private final DefaultMultiValueMap<Class<?>, ApplicationListener>
          applicationListenerCache = new DefaultMultiValueMap<>(16);

  private final LinkedHashSet<String> listenerBeanNames = new LinkedHashSet<>();
  private final ArrayList<ApplicationListener> applicationListeners = new ArrayList<>();

  @Nullable
  private BeanFactory beanFactory;

  public DefaultApplicationEventPublisher() { }

  public DefaultApplicationEventPublisher(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void publishEvent(Object event) {
    if (log.isDebugEnabled()) {
      log.debug("Publish event: [{}]", event);
    }

    List<ApplicationListener> listeners = filterApplicationListeners(event);
    if (CollectionUtils.isNotEmpty(listeners)) {
      for (ApplicationListener applicationListener : listeners) {
        applicationListener.onApplicationEvent(event);
      }
    }
  }

  private List<ApplicationListener> filterApplicationListeners(Object event) {
    Class<?> eventClass = event.getClass();
    List<ApplicationListener> listenerList = applicationListenerCache.get(eventClass);
    if (listenerList == null) {
      synchronized(applicationListenerCache) {
        listenerList = applicationListenerCache.get(eventClass);
        if (listenerList == null) {
          // find listeners
          if (CollectionUtils.isNotEmpty(listenerBeanNames)) {
            Assert.state(beanFactory != null, "No BeanFactory");
            for (String listenerBeanName : listenerBeanNames) {
              ApplicationListener listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
              applicationListeners.add(listener);
            }
            listenerBeanNames.clear();
          }

          listenerList = new ArrayList<>();
          for (ApplicationListener listener : applicationListeners) {
            if (isTargetEvent(listener, eventClass)) {
              listenerList.add(listener);
            }
          }

          if (listenerList.isEmpty()) {
            listenerList = Collections.emptyList();
          }

          applicationListenerCache.put(eventClass, listenerList);
          applicationListenerCache.trimToSize();
        }
      }
    }
    return listenerList;
  }

  private boolean isTargetEvent(ApplicationListener listener, Class<?> eventClass) {
    if (listener instanceof EventProvider) { // @since 2.1.7
      Class<?>[] supportedEvent = ((EventProvider) listener).getSupportedEvent();
      return ObjectUtils.containsElement(supportedEvent, eventClass);
    }
    else {
      Class<?> supportedEvent = GenericTypeResolver.resolveTypeArgument(listener.getClass(), ApplicationListener.class);
      return ClassUtils.isAssignable(supportedEvent, eventClass);
    }
  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "listener can't be null");
    synchronized(applicationListenerCache) {
      invalidateCache();
      applicationListeners.add(listener);
    }
  }

  @Override
  public void addApplicationListener(String listenerBeanName) {
    synchronized(applicationListenerCache) {
      invalidateCache();
      listenerBeanNames.add(listenerBeanName);
    }
  }

  @Override
  public void removeAllListeners() {
    synchronized(applicationListenerCache) {
      invalidateCache();
      listenerBeanNames.clear();
      applicationListeners.clear();
    }
  }

  @Override
  public void removeApplicationListener(String listenerBeanName) {
    synchronized(applicationListenerCache) {
      invalidateCache();
      listenerBeanNames.remove(listenerBeanName);
    }
  }

  @Override
  public void removeApplicationListener(ApplicationListener<?> listener) {
    synchronized(applicationListenerCache) {
      invalidateCache();
      applicationListeners.remove(listener);
    }
  }

  @Override
  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  private void invalidateCache() {
    applicationListenerCache.clear();
  }

}
