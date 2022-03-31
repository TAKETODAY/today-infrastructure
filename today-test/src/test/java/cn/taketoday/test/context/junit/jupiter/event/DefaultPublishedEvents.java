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

package cn.taketoday.test.context.junit.jupiter.event;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.test.context.event.ApplicationEventsHolder;

/**
 * Default implementation of {@link PublishedEvents}.
 *
 * <p>Copied from the Moduliths project.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 4.0
 */
class DefaultPublishedEvents implements PublishedEvents {

  @Override
  public <T> TypedPublishedEvents<T> ofType(Class<T> type) {
    return SimpleTypedPublishedEvents.of(ApplicationEventsHolder.getRequiredApplicationEvents().stream(type));
  }

  private static class SimpleTypedPublishedEvents<T> implements TypedPublishedEvents<T> {

    private final List<T> events;

    private SimpleTypedPublishedEvents(List<T> events) {
      this.events = events;
    }

    static <T> SimpleTypedPublishedEvents<T> of(Stream<T> stream) {
      return new SimpleTypedPublishedEvents<>(stream.collect(Collectors.toList()));
    }

    @Override
    public <S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType) {
      return SimpleTypedPublishedEvents.of(getFilteredEvents(subType::isInstance)//
              .map(subType::cast));
    }

    @Override
    public TypedPublishedEvents<T> matching(Predicate<? super T> predicate) {
      return SimpleTypedPublishedEvents.of(getFilteredEvents(predicate));
    }

    @Override
    public <S> TypedPublishedEvents<T> matchingMapped(Function<T, S> mapper, Predicate<? super S> predicate) {
      return SimpleTypedPublishedEvents.of(this.events.stream().flatMap(it -> {
        S mapped = mapper.apply(it);
        return predicate.test(mapped) ? Stream.of(it) : Stream.empty();
      }));
    }

    private Stream<T> getFilteredEvents(Predicate<? super T> predicate) {
      return this.events.stream().filter(predicate);
    }

    @Override
    public Iterator<T> iterator() {
      return this.events.iterator();
    }

    @Override
    public String toString() {
      return this.events.toString();
    }
  }

}
