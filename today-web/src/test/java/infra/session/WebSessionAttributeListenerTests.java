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

package infra.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/7 09:48
 */
class WebSessionAttributeListenerTests {

  @Test
  void attributeAddedIsCalledWhenAttributeIsSet() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(
            eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new WebSessionAttributeListener() {

    });

    session.setAttribute("testAttribute", "testValue");

    assertThat(listener.events).hasSize(1);
    SessionAttributeEvent event = listener.events.get(0);
    assertThat(event.type).isEqualTo(SessionAttributeEvent.Type.ADDED);
    assertThat(event.session).isSameAs(session);
    assertThat(event.attributeName).isEqualTo("testAttribute");
    assertThat(event.value).isEqualTo("testValue");
  }

  @Test
  void attributeRemovedIsCalledWhenAttributeIsRemoved() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new WebSessionAttributeListener() {

    });

    session.setAttribute("testAttribute", "testValue");
    session.removeAttribute("testAttribute");

    assertThat(listener.events).hasSize(2);
    SessionAttributeEvent event = listener.events.get(1);
    assertThat(event.type).isEqualTo(SessionAttributeEvent.Type.REMOVED);
    assertThat(event.session).isSameAs(session);
    assertThat(event.attributeName).isEqualTo("testAttribute");
    assertThat(event.value).isEqualTo("testValue");
  }

  @Test
  void attributeReplacedIsCalledWhenAttributeIsReplaced() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new WebSessionAttributeListener() {

    });

    session.setAttribute("testAttribute", "oldValue");
    session.setAttribute("testAttribute", "newValue");

    assertThat(listener.events).hasSize(2);
    SessionAttributeEvent event = listener.events.get(1);
    assertThat(event.type).isEqualTo(SessionAttributeEvent.Type.REPLACED);
    assertThat(event.session).isSameAs(session);
    assertThat(event.attributeName).isEqualTo("testAttribute");
    assertThat(event.oldValue).isEqualTo("oldValue");
    assertThat(event.value).isEqualTo("newValue");
  }

  @Test
  void multipleListenersAreNotified() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener1 = new TestWebSessionAttributeListener();
    TestWebSessionAttributeListener listener2 = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener1);
    eventDispatcher.addAttributeListeners(listener2);

    session.setAttribute("testAttribute", "testValue");

    assertThat(listener1.events).hasSize(1);
    assertThat(listener2.events).hasSize(1);
  }

  @Test
  void sessionInvalidateTriggersAttributeRemovedEvents() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new WebSessionAttributeListener() {

    });

    session.setAttribute("attr1", "value1");
    session.setAttribute("attr2", "value2");
    session.invalidate();

    assertThat(listener.events).hasSize(4); // 2 added + 2 removed
    SessionAttributeEvent event1 = listener.events.get(2);
    SessionAttributeEvent event2 = listener.events.get(3);
    assertThat(event1.type).isEqualTo(SessionAttributeEvent.Type.REMOVED);
    assertThat(event2.type).isEqualTo(SessionAttributeEvent.Type.REMOVED);
  }

  @Test
  void replaceWithSameValueStillTriggersReplaceEvent() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(eventDispatcher, new SecureRandomSessionIdGenerator());
    WebSession session = repository.createSession();

    TestWebSessionAttributeListener listener = new TestWebSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new WebSessionAttributeListener() {

    });

    String sameValue = "sameValue";
    session.setAttribute("testAttribute", sameValue);
    session.setAttribute("testAttribute", sameValue);

    assertThat(listener.events).hasSize(2);
    SessionAttributeEvent event = listener.events.get(1);
    assertThat(event.type).isEqualTo(SessionAttributeEvent.Type.REPLACED);
    assertThat(event.oldValue).isSameAs(sameValue);
    assertThat(event.value).isSameAs(sameValue);
  }

  static class TestWebSessionAttributeListener implements WebSessionAttributeListener {
    final java.util.List<SessionAttributeEvent> events = new java.util.ArrayList<>();

    @Override
    public void attributeAdded(WebSession session, String attributeName, Object value) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.ADDED, session, attributeName, value, null));
    }

    @Override
    public void attributeRemoved(WebSession session, String attributeName, Object value) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.REMOVED, session, attributeName, value, null));
    }

    @Override
    public void attributeReplaced(WebSession session, String attributeName, Object oldValue, Object newValue) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.REPLACED, session, attributeName, newValue, oldValue));
    }
  }

  static class SessionAttributeEvent {
    enum Type {ADDED, REMOVED, REPLACED}

    final Type type;
    final WebSession session;
    final String attributeName;
    final Object value;
    final Object oldValue;

    SessionAttributeEvent(Type type, WebSession session, String attributeName, Object value, Object oldValue) {
      this.type = type;
      this.session = session;
      this.attributeName = attributeName;
      this.value = value;
      this.oldValue = oldValue;
    }
  }

}