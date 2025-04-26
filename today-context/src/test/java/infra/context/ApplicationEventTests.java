/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 19:42
 */
class ApplicationEventTests {

  @Test
  void nullSourceThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new TestApplicationEvent(null));
  }

  @Test
  void sourceIsAccessibleAfterCreation() {
    Object source = new Object();
    TestApplicationEvent event = new TestApplicationEvent(source);
    assertThat(event.getSource()).isSameAs(source);
  }

  @Test
  void timestampIsSetOnCreation() {
    long beforeCreation = System.currentTimeMillis();
    TestApplicationEvent event = new TestApplicationEvent("test");
    long afterCreation = System.currentTimeMillis();

    assertThat(event.getTimestamp())
            .isGreaterThanOrEqualTo(beforeCreation)
            .isLessThanOrEqualTo(afterCreation);
  }

  @Test
  void customClockIsUsed() {
    Clock fixedClock = Clock.fixed(Instant.parse("2024-04-02T10:15:30.00Z"), ZoneOffset.UTC);
    TestApplicationEvent event = new TestApplicationEvent("test", fixedClock);
    assertThat(event.getTimestamp()).isEqualTo(fixedClock.millis());
  }

  @Test
  void genericListenerReceivesOnlyMatchingEvents() {
    TestApplicationListener<CustomEvent> listener = new TestApplicationListener<>();
    CustomEvent event = new CustomEvent("test");
    Object otherEvent = new OtherEvent("other");

    listener.onApplicationEvent(event);
    assert listener.receivedEvent == event;

    assertThatCode(() -> listener.onApplicationEvent((CustomEvent) otherEvent))
            .isInstanceOf(ClassCastException.class);
  }

  @Test
  void asyncListenerSupportsAsyncExecution() {
    TestAsyncListener listener = new TestAsyncListener();
    assertThat(listener.supportsAsyncExecution()).isTrue();
  }

  @Test
  void syncListenerDoesNotSupportAsyncExecution() {
    TestSyncListener listener = new TestSyncListener();
    assertThat(listener.supportsAsyncExecution()).isFalse();
  }

  @Test
  void payloadListenerReceivesPayload() {
    List<String> receivedPayloads = new ArrayList<>();
    ApplicationListener<PayloadApplicationEvent<String>> listener =
            ApplicationListener.forPayload(receivedPayloads::add);

    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
    listener.onApplicationEvent(event);

    assertThat(receivedPayloads).containsExactly("test");
  }

  private static class CustomEvent extends ApplicationEvent {
    CustomEvent(Object source) {
      super(source);
    }
  }

  private static class OtherEvent extends ApplicationEvent {
    OtherEvent(Object source) {
      super(source);
    }
  }

  private static class TestApplicationListener<E extends ApplicationEvent> implements ApplicationListener<E> {
    private E receivedEvent;

    @Override
    public void onApplicationEvent(E event) {
      this.receivedEvent = event;
    }
  }

  private static class TestAsyncListener implements ApplicationListener<ApplicationEvent> {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
  }

  private static class TestSyncListener implements ApplicationListener<ApplicationEvent> {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }

    @Override
    public boolean supportsAsyncExecution() {
      return false;
    }
  }

  private static class TestApplicationEvent extends ApplicationEvent {
    TestApplicationEvent(Object source) {
      super(source);
    }

    TestApplicationEvent(Object source, Clock clock) {
      super(source, clock);
    }
  }

}