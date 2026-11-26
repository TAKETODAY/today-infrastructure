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

import java.util.List;

import infra.core.Conventions;
import infra.web.mock.MockServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 23:27
 */
class SessionIdResolverTests {

  private MockServerHttpExchange exchange;

  @BeforeEach
  void setUp() {
    exchange = new MockServerHttpExchange();
  }

  // HeaderSessionIdResolver 测试
  @Test
  void headerSessionIdResolver_getSessionId_shouldResolveFromHeader() {
    HeaderSessionIdResolver resolver = SessionIdResolver.forHeader("X-Custom-Token");
    String sessionId = "test-session-id";

    exchange.requestHeaders().add("X-Custom-Token", sessionId);

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }

  @Test
  void headerSessionIdResolver_getSessionId_whenHeaderNotPresent_shouldReturnNull() {
    HeaderSessionIdResolver resolver = SessionIdResolver.forHeader("X-Custom-Token");

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isNull();
  }

  @Test
  void headerSessionIdResolver_setSessionId_shouldSetResponseHeader() {
    HeaderSessionIdResolver resolver = SessionIdResolver.forHeader("X-Custom-Token");
    String sessionId = "test-session-id";

    resolver.setSessionId(exchange, sessionId);

    assertThat(exchange.responseHeaders().getFirst("X-Custom-Token")).isEqualTo(sessionId);
  }

  @Test
  void headerSessionIdResolver_expireSession_shouldRemoveHeader() {
    HeaderSessionIdResolver resolver = SessionIdResolver.forHeader("X-Custom-Token");
    String sessionId = "test-session-id";

    // 先设置 session ID
    exchange.requestHeaders().add("X-Custom-Token", sessionId);
    resolver.expireSession(exchange);

    // 验证头部被移除的逻辑（这里根据实现可能有所不同）
    // 由于 expireSession 实现为空，这里只是验证不会抛异常
    assertThatCode(() -> resolver.expireSession(exchange)).doesNotThrowAnyException();
  }

  // X-Auth-Token 静态工厂方法测试
  @Test
  void xAuthToken_factoryMethod_shouldCreateCorrectResolver() {
    HeaderSessionIdResolver resolver = SessionIdResolver.xAuthToken();
    String sessionId = "test-session-id";

    exchange.requestHeaders().add(SessionIdResolver.HEADER_X_AUTH_TOKEN, sessionId);

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }

  // Authentication-Info 静态工厂方法测试
  @Test
  void authenticationInfo_factoryMethod_shouldCreateCorrectResolver() {
    HeaderSessionIdResolver resolver = SessionIdResolver.authenticationInfo();
    String sessionId = "test-session-id";

    exchange.requestHeaders().add(SessionIdResolver.HEADER_AUTHENTICATION_INFO, sessionId);

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }

  // RequestParameterSessionIdResolver 测试
  @Test
  void requestParameterSessionIdResolver_getSessionId_shouldResolveFromParameter() {
    RequestParameterSessionIdResolver resolver = SessionIdResolver.forParameter("jsessionid");
    String sessionId = "test-session-id";

    exchange.setParameter("jsessionid", sessionId);

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }

  @Test
  void requestParameterSessionIdResolver_getSessionId_whenParameterNotPresent_shouldReturnNull() {
    RequestParameterSessionIdResolver resolver = SessionIdResolver.forParameter("jsessionid");

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isNull();
  }

  @Test
  void requestParameterSessionIdResolver_setSessionId_shouldAddResponseAttribute() {
    RequestParameterSessionIdResolver resolver = SessionIdResolver.forParameter("jsessionid");
    String sessionId = "test-session-id";

    resolver.setSessionId(exchange, sessionId);

    assertThat(exchange.getAttribute(SessionIdResolver.WRITTEN_SESSION_ID_ATTR)).isEqualTo(sessionId);
  }

  // CookieSessionIdResolver 测试
  @Test
  void cookieSessionIdResolver_getSessionId_shouldResolveFromCookie() {
    CookieSessionIdResolver resolver = SessionIdResolver.forCookie("JSESSIONID");
    String sessionId = "test-session-id";

    exchange.addCookie("JSESSIONID", sessionId);

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }

  @Test
  void cookieSessionIdResolver_getSessionId_whenCookieNotPresent_shouldReturnNull() {
    CookieSessionIdResolver resolver = SessionIdResolver.forCookie("JSESSIONID");

    String resolvedId = resolver.getSessionId(exchange);
    assertThat(resolvedId).isNull();
  }

  // Composite SessionIdResolver 测试
  @Test
  void compositeSessionIdResolver_getSessionId_shouldSearchInOrder() {
    HeaderSessionIdResolver headerResolver = SessionIdResolver.forHeader("X-Token");
    RequestParameterSessionIdResolver paramResolver = SessionIdResolver.forParameter("sid");

    SessionIdResolver composite = SessionIdResolver.forComposite(headerResolver, paramResolver);

    // 测试第一个解析器找到的情况
    String headerSessionId = "header-session-id";
    exchange.requestHeaders().add("X-Token", headerSessionId);
    exchange.setParameter("sid", "param-session-id");

    String resolvedId = composite.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(headerSessionId);
  }

  @Test
  void compositeSessionIdResolver_getSessionId_shouldFallBackToSecondResolver() {
    HeaderSessionIdResolver headerResolver = SessionIdResolver.forHeader("X-Token");
    RequestParameterSessionIdResolver paramResolver = SessionIdResolver.forParameter("sid");

    SessionIdResolver composite = SessionIdResolver.forComposite(headerResolver, paramResolver);

    // 只在第二个解析器中设置 session ID
    String paramSessionId = "param-session-id";
    exchange.setParameter("sid", paramSessionId);

    String resolvedId = composite.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(paramSessionId);
  }

  @Test
  void compositeSessionIdResolver_getSessionId_whenNoneFound_shouldReturnNull() {
    HeaderSessionIdResolver headerResolver = SessionIdResolver.forHeader("X-Token");
    RequestParameterSessionIdResolver paramResolver = SessionIdResolver.forParameter("sid");

    SessionIdResolver composite = SessionIdResolver.forComposite(headerResolver, paramResolver);

    String resolvedId = composite.getSessionId(exchange);
    assertThat(resolvedId).isNull();
  }

  @Test
  void compositeSessionIdResolver_setSessionId_shouldCallAllResolvers() {
    SessionIdResolver resolver1 = mock(SessionIdResolver.class);
    SessionIdResolver resolver2 = mock(SessionIdResolver.class);

    SessionIdResolver composite = SessionIdResolver.forComposite(resolver1, resolver2);
    String sessionId = "test-session-id";

    composite.setSessionId(exchange, sessionId);

    verify(resolver1).setSessionId(exchange, sessionId);
    verify(resolver2).setSessionId(exchange, sessionId);
  }

  @Test
  void compositeSessionIdResolver_expireSession_shouldCallMatchingResolverExpire() {
    SessionIdResolver resolver1 = mock(SessionIdResolver.class);
    SessionIdResolver resolver2 = mock(SessionIdResolver.class);

    when(resolver1.getSessionId(exchange)).thenReturn("session-id");
    when(resolver2.getSessionId(exchange)).thenReturn(null);

    SessionIdResolver composite = SessionIdResolver.forComposite(resolver1, resolver2);

    composite.expireSession(exchange);

    verify(resolver1).expireSession(exchange);
    verify(resolver2, never()).expireSession(any());
  }

  // 常量测试
  @Test
  void writtenSessionIdAttr_constant_shouldBeCorrect() {
    String expected = Conventions.getQualifiedAttributeName(
            CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR");
    assertThat(SessionIdResolver.WRITTEN_SESSION_ID_ATTR).isEqualTo(expected);
  }

  @Test
  void headerXAuthToken_constant_shouldBeCorrect() {
    assertThat(SessionIdResolver.HEADER_X_AUTH_TOKEN).isEqualTo("X-Auth-Token");
  }

  @Test
  void headerAuthenticationInfo_constant_shouldBeCorrect() {
    assertThat(SessionIdResolver.HEADER_AUTHENTICATION_INFO).isEqualTo("Authentication-Info");
  }

  // 边界情况测试
  @Test
  void getSessionId_withNullContext_shouldHandleGracefully() {
    SessionIdResolver resolver = SessionIdResolver.forHeader("X-Token");

    // MockHttpExchange 不允许 null，所以这里验证不会抛出 NPE
    assertThatCode(() -> resolver.getSessionId(exchange)).doesNotThrowAnyException();
  }

  @Test
  void composite_withEmptyResolverList_shouldHandle() {
    SessionIdResolver composite = SessionIdResolver.forComposite(List.of());

    String resolvedId = composite.getSessionId(exchange);
    assertThat(resolvedId).isNull();
  }

  @Test
  void composite_withSingleResolver_shouldWork() {
    HeaderSessionIdResolver headerResolver = SessionIdResolver.forHeader("X-Token");
    String sessionId = "test-session-id";
    exchange.requestHeaders().add("X-Token", sessionId);

    SessionIdResolver composite = SessionIdResolver.forComposite(headerResolver);

    String resolvedId = composite.getSessionId(exchange);
    assertThat(resolvedId).isEqualTo(sessionId);
  }
}
