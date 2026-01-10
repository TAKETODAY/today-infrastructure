/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.event;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.aop.support.AopUtils;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.core.Ordered;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.util.ConcurrentReferenceHashMap;

/**
 * {@link GenericApplicationListener} adapter that determines supported event types
 * through introspecting the generically declared type of the target listener.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
    Assert.notNull(delegate, "Delegate listener is required");
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
      eventType = ResolvableType.forClass(listenerType)
              .as(ApplicationListener.class)
              .getGeneric();
      eventTypeCache.put(listenerType, eventType);
    }
    return eventType != ResolvableType.NONE ? eventType : null;
  }

}
