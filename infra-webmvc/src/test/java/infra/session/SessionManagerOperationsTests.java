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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionManagerOperationsTests {

  private SessionManagerOperations sessionManagerOperations;

  @Mock
  private SessionManager sessionManager;

  private MockRequestContext requestContext;

  private Session session;

  @BeforeEach
  void setUp() {
    sessionManagerOperations = new SessionManagerOperations(sessionManager);
    requestContext = new MockRequestContext();
    session = new MapSession();
  }

  @Test
  void constructor_shouldInitializeSessionManager() {
    assertThat(sessionManagerOperations.getSessionManager()).isEqualTo(sessionManager);
  }

  @Test
  void constructor_withNullSessionManager_shouldThrowException() {
    assertThatThrownBy(() -> new SessionManagerOperations(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SessionManager is required");
  }

  @Test
  void getSession_withoutCreateFlag_shouldDelegateToSessionManager() {
    when(sessionManager.getSession(requestContext)).thenReturn(session);

    Session result = sessionManagerOperations.getSession(requestContext);

    assertThat(result).isEqualTo(session);
    verify(sessionManager).getSession(requestContext);
  }

  @Test
  void getSession_withCreateTrue_shouldDelegateToSessionManager() {
    when(sessionManager.getSession(requestContext, true)).thenReturn(session);

    Session result = sessionManagerOperations.getSession(requestContext, true);

    assertThat(result).isEqualTo(session);
    verify(sessionManager).getSession(requestContext, true);
  }

  @Test
  void getSession_withCreateFalse_shouldDelegateToSessionManager() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(null);

    Session result = sessionManagerOperations.getSession(requestContext, false);

    assertThat(result).isNull();
    verify(sessionManager).getSession(requestContext, false);
  }

  @Test
  void getAttribute_fromWebSession_shouldReturnAttributeValue() {
    String attributeName = "testAttribute";
    String attributeValue = "testValue";
    session.setAttribute(attributeName, attributeValue);

    Object result = sessionManagerOperations.getAttribute(session, attributeName);

    assertThat(result).isEqualTo(attributeValue);
  }

  @Test
  void getAttribute_fromWebSession_whenAttributeNotExists_shouldReturnNull() {
    Object result = sessionManagerOperations.getAttribute(session, "nonExistentAttribute");

    assertThat(result).isNull();
  }

  @Test
  void getAttribute_fromRequestContext_whenSessionExists_shouldReturnAttributeValue() {
    String attributeName = "testAttribute";
    String attributeValue = "testValue";
    session.setAttribute(attributeName, attributeValue);

    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    Object result = sessionManagerOperations.getAttribute(requestContext, attributeName);

    assertThat(result).isEqualTo(attributeValue);
  }

  @Test
  void getAttribute_fromRequestContext_whenSessionNotExists_shouldReturnNull() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(null);

    Object result = sessionManagerOperations.getAttribute(requestContext, "testAttribute");

    assertThat(result).isNull();
  }

  @Test
  void setAttribute_whenSessionExists_shouldSetAttribute() {
    String attributeName = "testAttribute";
    String attributeValue = "testValue";

    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    sessionManagerOperations.setAttribute(requestContext, attributeName, attributeValue);

    assertThat(session.getAttribute(attributeName)).isEqualTo(attributeValue);
  }

  @Test
  void setAttribute_whenSessionNotExists_shouldNotThrowException() {
    String attributeName = "testAttribute";
    String attributeValue = "testValue";

    when(sessionManager.getSession(requestContext, false)).thenReturn(null);

    assertThatCode(() -> sessionManagerOperations.setAttribute(requestContext, attributeName, attributeValue))
            .doesNotThrowAnyException();
  }

  @Test
  void removeAttribute_whenSessionExists_shouldRemoveAndReturnAttributeValue() {
    String attributeName = "testAttribute";
    String attributeValue = "testValue";
    session.setAttribute(attributeName, attributeValue);

    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    Object result = sessionManagerOperations.removeAttribute(requestContext, attributeName);

    assertThat(result).isEqualTo(attributeValue);
    assertThat(session.hasAttribute(attributeName)).isFalse();
  }

  @Test
  void removeAttribute_whenSessionNotExists_shouldReturnNull() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(null);

    Object result = sessionManagerOperations.removeAttribute(requestContext, "testAttribute");

    assertThat(result).isNull();
  }

  @Test
  void removeAttribute_whenAttributeNotExists_shouldReturnNull() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    Object result = sessionManagerOperations.removeAttribute(requestContext, "nonExistentAttribute");

    assertThat(result).isNull();
  }

  // 边界情况测试
  @Test
  void getAttribute_withNullAttributeName_shouldHandleGracefully() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    assertThatCode(() -> sessionManagerOperations.getAttribute(requestContext, null))
            .doesNotThrowAnyException();
  }

  @Test
  void setAttribute_withNullAttributeName_shouldHandleGracefully() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    assertThatCode(() -> sessionManagerOperations.setAttribute(requestContext, null, "value"))
            .doesNotThrowAnyException();
  }

  @Test
  void removeAttribute_withNullAttributeName_shouldHandleGracefully() {
    when(sessionManager.getSession(requestContext, false)).thenReturn(session);

    assertThatCode(() -> sessionManagerOperations.removeAttribute(requestContext, null))
            .doesNotThrowAnyException();
  }

  @Test
  void getSessionManager_shouldReturnCorrectInstance() {
    assertThat(sessionManagerOperations.getSessionManager()).isSameAs(sessionManager);
  }

}
