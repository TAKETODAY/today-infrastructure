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

package cn.taketoday.context.event;

import java.util.Map;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * {@link GenericApplicationListener} adapter that determines supported event types
 * through introspecting the generically declared type of the target listener.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationListener#onApplicationEvent
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class GenericApplicationListenerAdapter implements GenericApplicationListener {

  private static final Map<Class<?>, ResolvableType> eventTypeCache = new ConcurrentReferenceHashMap<>();

  private final ApplicationListener delegate;

  @Nullable
  private final ResolvableType declaredEventType;

  /**
   * Create a new GenericApplicationListener for the given delegate.
   *
   * @param delegate the delegate listener to be invoked
   */
  public GenericApplicationListenerAdapter(ApplicationListener delegate) {
    Assert.notNull(delegate, "Delegate listener must not be null");
    this.delegate = delegate;
    this.declaredEventType = resolveDeclaredEventType(this.delegate);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onApplicationEvent(ApplicationEvent event) {
    this.delegate.onApplicationEvent(event);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean supportsEventType(ResolvableType eventType) {
    if (this.delegate instanceof GenericApplicationListener) {
      return ((GenericApplicationListener) this.delegate).supportsEventType(eventType);
    }
    else if (this.delegate instanceof SmartApplicationListener) {
      Class<? extends ApplicationEvent> eventClass = (Class<? extends ApplicationEvent>) eventType.resolve();
      return eventClass != null && ((SmartApplicationListener) this.delegate).supportsEventType(eventClass);
    }
    else {
      return this.declaredEventType == null || this.declaredEventType.isAssignableFrom(eventType);
    }
  }

  @Override
  public boolean supportsSourceType(@Nullable Class<?> sourceType) {
    return !(this.delegate instanceof SmartApplicationListener)
            || ((SmartApplicationListener) this.delegate).supportsSourceType(sourceType);
  }

  @Override
  public int getOrder() {
    return this.delegate instanceof Ordered ? ((Ordered) this.delegate).getOrder() : LOWEST_PRECEDENCE;
  }

  @Override
  public String getListenerId() {
    return this.delegate instanceof SmartApplicationListener smart ? smart.getListenerId() : "";
  }

  @Nullable
  private static ResolvableType resolveDeclaredEventType(ApplicationListener<?> listener) {
    ResolvableType declaredEventType = resolveDeclaredEventType(listener.getClass());
    if (declaredEventType == null || declaredEventType.isAssignableFrom(ApplicationEvent.class)) {
      Class<?> targetClass = AopUtils.getTargetClass(listener);
      if (targetClass != listener.getClass()) {
        declaredEventType = resolveDeclaredEventType(targetClass);
      }
    }
    return declaredEventType;
  }

  @Nullable
  static ResolvableType resolveDeclaredEventType(Class<?> listenerType) {
    ResolvableType eventType = eventTypeCache.get(listenerType);
    if (eventType == null) {
      eventType = ResolvableType.fromClass(listenerType)
              .as(ApplicationListener.class)
              .getGeneric();
      eventTypeCache.put(listenerType, eventType);
    }
    return eventType != ResolvableType.NONE ? eventType : null;
  }

}
