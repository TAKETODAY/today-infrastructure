/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/7 09:48
 */
class SessionAttributeListenerTests {

  @Test
  void attributeAddedIsCalledWhenAttributeIsSet() {
    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    InMemorySessionRepository repository = new InMemorySessionRepository(
            eventDispatcher, new SecureRandomSessionIdGenerator());
    Session session = repository.createSession();

    TestSessionAttributeListener listener = new TestSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new SessionAttributeListener() {

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
    Session session = repository.createSession();

    TestSessionAttributeListener listener = new TestSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new SessionAttributeListener() {

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
    Session session = repository.createSession();

    TestSessionAttributeListener listener = new TestSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new SessionAttributeListener() {

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
    Session session = repository.createSession();

    TestSessionAttributeListener listener1 = new TestSessionAttributeListener();
    TestSessionAttributeListener listener2 = new TestSessionAttributeListener();
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
    Session session = repository.createSession();

    TestSessionAttributeListener listener = new TestSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new SessionAttributeListener() {

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
    Session session = repository.createSession();

    TestSessionAttributeListener listener = new TestSessionAttributeListener();
    eventDispatcher.addAttributeListeners(listener);
    eventDispatcher.addAttributeListeners(new SessionAttributeListener() {

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

  static class TestSessionAttributeListener implements SessionAttributeListener {
    final java.util.List<SessionAttributeEvent> events = new java.util.ArrayList<>();

    @Override
    public void attributeAdded(Session session, String attributeName, Object value) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.ADDED, session, attributeName, value, null));
    }

    @Override
    public void attributeRemoved(Session session, String attributeName, Object value) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.REMOVED, session, attributeName, value, null));
    }

    @Override
    public void attributeReplaced(Session session, String attributeName, Object oldValue, Object newValue) {
      events.add(new SessionAttributeEvent(SessionAttributeEvent.Type.REPLACED, session, attributeName, newValue, oldValue));
    }
  }

  static class SessionAttributeEvent {
    enum Type {ADDED, REMOVED, REPLACED}

    final Type type;
    final Session session;
    final String attributeName;
    final Object value;
    final Object oldValue;

    SessionAttributeEvent(Type type, Session session, String attributeName, Object value, Object oldValue) {
      this.type = type;
      this.session = session;
      this.attributeName = attributeName;
      this.value = value;
      this.oldValue = oldValue;
    }
  }

}