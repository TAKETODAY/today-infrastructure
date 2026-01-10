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

import java.util.function.Consumer;

import infra.context.ApplicationEvent;
import infra.core.ResolvableType;

/**
 * A {@link GenericApplicationListener} implementation that supports a single
 * event type.
 *
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class GenericApplicationListenerDelegate<E extends ApplicationEvent> implements GenericApplicationListener {

  private final Class<E> supportedEventType;

  private final Consumer<E> consumer;

  GenericApplicationListenerDelegate(Class<E> supportedEventType, Consumer<E> consumer) {
    this.supportedEventType = supportedEventType;
    this.consumer = consumer;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    this.consumer.accept(this.supportedEventType.cast(event));
  }

  @Override
  public boolean supportsEventType(ResolvableType eventType) {
    return this.supportedEventType.isAssignableFrom(eventType.toClass());
  }

}
