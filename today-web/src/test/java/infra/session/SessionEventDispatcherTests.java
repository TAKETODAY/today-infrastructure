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
  WebSession session;

  @Mock
  WebSessionListener listener;

  SessionEventDispatcher dispatcher = new SessionEventDispatcher();

  @Test
  void shouldNotifyListenersOnSessionCreated() {
    dispatcher.addSessionListeners(listener);
    dispatcher.onSessionCreated(session);

    verify(listener).sessionCreated(session);
  }

  @Test
  void shouldNotifyListenersOnSessionDestroyed() {
    dispatcher.addSessionListeners(listener);
    dispatcher.onSessionDestroyed(session);

    verify(listener).sessionDestroyed(session);
  }

  @Mock
  WebSessionAttributeListener attributeListener;

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
    WebSessionListener listener2 = mock(WebSessionListener.class);
    dispatcher.addSessionListeners(listener, listener2);

    dispatcher.onSessionCreated(session);

    verify(listener).sessionCreated(session);
    verify(listener2).sessionCreated(session);
  }

  @Test
  void shouldHandleMultipleAttributeListeners() {
    WebSessionAttributeListener attributeListener2 = mock(WebSessionAttributeListener.class);
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
