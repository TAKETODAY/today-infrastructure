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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 21:12
 */
@ExtendWith(MockitoExtension.class)
class SessionEventDispatcherTests {

  @Mock
  Session session;

  @Mock
  SessionListener listener;

  SessionEventDispatcher dispatcher = new SessionEventDispatcher();

  @Test
  void shouldNotifyListenersOnSessionCreated() {
    dispatcher.addSessionListeners(new SessionListener() { });
    dispatcher.addSessionListeners(listener);
    dispatcher.onSessionCreated(session);

    verify(listener).sessionCreated(session);
  }

  @Test
  void shouldNotifyListenersOnSessionDestroyed() {
    dispatcher.addSessionListeners(new SessionListener() { });
    dispatcher.addSessionListeners(listener);
    dispatcher.onSessionDestroyed(session);

    verify(listener).sessionDestroyed(session);
  }

  @Mock
  SessionAttributeListener attributeListener;

  @Test
  void shouldNotifyAttributeListenersOnAttributeAdded() {
    dispatcher.addAttributeListeners(attributeListener);
    String name = "name";
    Object value = "value";

    dispatcher.attributeAdded(session, name, value);

    verify(attributeListener).attributeAdded(session, name, value);
  }

  @Test
  void shouldNotifyAttributeListenersOnAttributeRemoved() {
    dispatcher.addAttributeListeners(attributeListener);
    String name = "name";
    Object value = "value";

    dispatcher.attributeRemoved(session, name, value);

    verify(attributeListener).attributeRemoved(session, name, value);
  }

  @Test
  void shouldNotifyAttributeListenersOnAttributeReplaced() {
    dispatcher.addAttributeListeners(attributeListener);
    String name = "name";
    Object oldValue = "oldValue";
    Object newValue = "newValue";

    dispatcher.attributeReplaced(session, name, oldValue, newValue);

    verify(attributeListener).attributeReplaced(session, name, oldValue, newValue);
  }

  @Test
  void shouldHandleMultipleSessionListeners() {
    SessionListener listener2 = mock(SessionListener.class);
    dispatcher.addSessionListeners(listener, listener2);

    dispatcher.onSessionCreated(session);

    verify(listener).sessionCreated(session);
    verify(listener2).sessionCreated(session);
  }

  @Test
  void shouldHandleMultipleAttributeListeners() {
    SessionAttributeListener attributeListener2 = mock(SessionAttributeListener.class);
    dispatcher.addAttributeListeners(attributeListener, attributeListener2);
    String name = "name";
    Object value = "value";

    dispatcher.attributeAdded(session, name, value);

    verify(attributeListener).attributeAdded(session, name, value);
    verify(attributeListener2).attributeAdded(session, name, value);
  }

  @Test
  void shouldAddSessionListenersFromCollection() {
    dispatcher.addSessionListeners(List.of(listener));
    dispatcher.onSessionCreated(session);

    verify(listener).sessionCreated(session);
  }

  @Test
  void shouldAddAttributeListenersFromCollection() {
    dispatcher.addAttributeListeners(List.of(attributeListener));
    String name = "name";
    Object value = "value";

    dispatcher.attributeAdded(session, name, value);

    verify(attributeListener).attributeAdded(session, name, value);
  }

  @Test
  void shouldNotThrowExceptionWhenNoListenersArePresent() {
    dispatcher.onSessionCreated(session);
    dispatcher.onSessionDestroyed(session);
    dispatcher.attributeAdded(session, "name", "value");
    dispatcher.attributeRemoved(session, "name", "value");
    dispatcher.attributeReplaced(session, "name", "old", "new");
  }

  @Test
  void shouldDoNothingWhenSessionListenerListIsEmpty() {
    dispatcher.onSessionCreated(session);
    dispatcher.onSessionDestroyed(session);
    // No exception should be thrown, and no interactions should occur if there were mocks.
  }

  @Test
  void shouldDoNothingWhenAttributeListenerListIsEmpty() {
    dispatcher.attributeAdded(session, "name", "value");
    dispatcher.attributeRemoved(session, "name", "value");
    dispatcher.attributeReplaced(session, "name", "old", "new");
    // No exception should be thrown.
  }

  @Test
  void shouldNotAddAnyListenersWhenAddingEmptySessionListenerArray() {
    dispatcher.addSessionListeners(listener);
    dispatcher.addSessionListeners(); // Add empty array

    assertThat(dispatcher.getSessionListeners()).containsExactly(listener);
  }

  @Test
  void shouldNotAddAnyListenersWhenAddingEmptyAttributeListenerArray() {
    dispatcher.addAttributeListeners(attributeListener);
    dispatcher.addAttributeListeners(); // Add empty array

    assertThat(dispatcher.getAttributeListeners()).containsExactly(attributeListener);
  }

  @Test
  void shouldNotAddAnyListenersWhenAddingEmptySessionListenerCollection() {
    dispatcher.addSessionListeners(listener);
    dispatcher.addSessionListeners(Collections.emptyList());

    assertThat(dispatcher.getSessionListeners()).containsExactly(listener);
  }

  @Test
  void shouldNotAddAnyListenersWhenAddingEmptyAttributeListenerCollection() {
    dispatcher.addAttributeListeners(attributeListener);
    dispatcher.addAttributeListeners(Collections.emptyList());

    assertThat(dispatcher.getAttributeListeners()).containsExactly(attributeListener);
  }

}
