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

package cn.taketoday.test.context.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.PayloadApplicationEvent;

/**
 * Default implementation of {@link ApplicationEvents}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 4.0
 */
class DefaultApplicationEvents implements ApplicationEvents {

  private final List<ApplicationEvent> events = new ArrayList<>();

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
