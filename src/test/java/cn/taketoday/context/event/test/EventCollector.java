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
package cn.taketoday.context.event.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utility to collect and assert events.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
@Component
public class EventCollector {

  private final MultiValueMap<String, Object> content = MultiValueMap.fromLinkedHashMap();


  /**
   * Register an event for the specified listener.
   */
  public void addEvent(Identifiable listener, Object event) {
    this.content.add(listener.getId(), event);
  }

  /**
   * Return the events that the specified listener has received. The list of events
   * is ordered according to their reception order.
   */
  public List<Object> getEvents(Identifiable listener) {
    return this.content.get(listener.getId());
  }

  /**
   * Assert that the listener identified by the specified id has not received any event.
   */
  public void assertNoEventReceived(String listenerId) {
    List<Object> events = this.content.getOrDefault(listenerId, Collections.emptyList());
    assertThat(events.size()).as("Expected no events but got " + events).isEqualTo(0);
  }

  /**
   * Assert that the specified listener has not received any event.
   */
  public void assertNoEventReceived(Identifiable listener) {
    assertNoEventReceived(listener.getId());
  }

  /**
   * Assert that the listener identified by the specified id has received the
   * specified events, in that specific order.
   */
  public void assertEvent(String listenerId, Object... events) {
    List<Object> actual = this.content.getOrDefault(listenerId, Collections.emptyList());
    assertThat(actual.size()).as("Wrong number of events").isEqualTo(events.length);
    for (int i = 0; i < events.length; i++) {
      assertThat(actual.get(i)).as("Wrong event at index " + i).isEqualTo(events[i]);
    }
  }

  /**
   * Assert that the specified listener has received the specified events, in
   * that specific order.
   */
  public void assertEvent(Identifiable listener, Object... events) {
    assertEvent(listener.getId(), events);
  }

  /**
   * Assert the number of events received by this instance. Checks that
   * unexpected events have not been received. If an event is handled by
   * several listeners, each instance will be registered.
   */
  public void assertTotalEventsCount(int number) {
    int actual = 0;
    for (Map.Entry<String, List<Object>> entry : this.content.entrySet()) {
      actual += entry.getValue().size();
    }
    assertThat(actual).as("Wrong number of total events (" + this.content.size() +
            ") registered listener(s)").isEqualTo(number);
  }

  /**
   * Clear the collected events, allowing for reuse of the collector.
   */
  public void clear() {
    this.content.clear();
  }

}
