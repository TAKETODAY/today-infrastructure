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

package cn.taketoday.context.event;

import java.util.function.Consumer;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.core.ResolvableType;

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
