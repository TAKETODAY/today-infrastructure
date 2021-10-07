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

package cn.taketoday.context.event;

import java.util.List;

import cn.taketoday.core.Assert;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/10/7 15:20
 * @since 4.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultApplicationEventPublisher implements ApplicationEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(DefaultApplicationEventPublisher.class);

  /** application listeners **/
  private final DefaultMultiValueMap<Class<?>, ApplicationListener>
          applicationListeners = new DefaultMultiValueMap<>(32);

  @Override
  public void publishEvent(Object event) {
    if (log.isDebugEnabled()) {
      log.debug("Publish event: [{}]", event);
    }

    List<ApplicationListener> listeners = applicationListeners.get(event.getClass());
    if (CollectionUtils.isNotEmpty(listeners)) {
      for (ApplicationListener applicationListener : listeners) {
        applicationListener.onApplicationEvent(event);
      }
    }
  }

  @Override
  public void addApplicationListener(Class<?> listener) {

  }

  @Override
  public void removeAllListeners() {
    applicationListeners.clear();
  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "listener can't be null");
    if (listener instanceof ApplicationEventCapable) { // @since 2.1.7
      for (Class<?> type : ((ApplicationEventCapable) listener).getApplicationEvent()) {
        addApplicationListener(listener, type);
      }
    }
    else {
      Class<?> eventType = GenericTypeResolver.resolveTypeArgument(listener.getClass(), ApplicationListener.class);
      addApplicationListener(listener, eventType);
    }
  }

  /**
   * Register to registry
   *
   * @param listener
   *         The instance of application listener
   * @param eventType
   *         The event type
   */
  protected void addApplicationListener(ApplicationListener listener, Class<?> eventType) {
    List<ApplicationListener> listeners = applicationListeners.get(eventType);
    if (listeners == null) {
      applicationListeners.add(eventType, listener);
    }
    else if (!listeners.contains(listener)) {
      listeners.add(listener);
      AnnotationAwareOrderComparator.sort(listeners);
    }
  }

}
