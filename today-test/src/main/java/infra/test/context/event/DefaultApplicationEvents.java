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

package infra.test.context.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import infra.context.ApplicationEvent;
import infra.context.PayloadApplicationEvent;

/**
 * Default implementation of {@link ApplicationEvents}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 4.0
 */
class DefaultApplicationEvents implements ApplicationEvents {

  private final List<ApplicationEvent> events = new CopyOnWriteArrayList<>();

  void addEvent(ApplicationEvent event) {
    this.events.add(event);
  }

  @Override
  public Stream<ApplicationEvent> stream() {
    return this.events.stream();
  }

  @Override
  public <T> Stream<T> stream(Class<T> type) {
    return this.events.stream()
            .map(this::unwrapPayloadEvent)
            .filter(type::isInstance)
            .map(type::cast);
  }

  @Override
  public void clear() {
    this.events.clear();
  }

  private Object unwrapPayloadEvent(Object source) {
    return ((source instanceof PayloadApplicationEvent<?> payloadApplicationEvent) ?
            payloadApplicationEvent.getPayload() : source);
  }

}
