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

import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:11
 */
class SessionMethodArgumentResolverTests {

  @Test
  void supportsParameterWithWebSessionTypeReturnsTrue() {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionMethodArgumentResolver resolver = new SessionMethodArgumentResolver(sessionManager);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.isAssignableTo(Session.class)).thenReturn(true);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isTrue();
  }

  @Test
  void supportsParameterWithNonWebSessionTypeReturnsFalse() {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionMethodArgumentResolver resolver = new SessionMethodArgumentResolver(sessionManager);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.isAssignableTo(Session.class)).thenReturn(false);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isFalse();
  }

  @Test
  void resolveArgumentWithRequiredParameterReturnsSession() {
    SessionManager sessionManager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SessionMethodArgumentResolver resolver = new SessionMethodArgumentResolver(sessionManager);

    RequestContext context = mock(RequestContext.class);
    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);

    when(parameter.isRequired()).thenReturn(true);
    when(sessionManager.getSession(context)).thenReturn(session);

    Object result = resolver.resolveArgument(context, parameter);

    assertThat(result).isSameAs(session);
  }

  @Test
  void resolveArgumentWithOptionalParameterReturnsSessionIfExists() {
    SessionManager sessionManager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SessionMethodArgumentResolver resolver = new SessionMethodArgumentResolver(sessionManager);

    RequestContext context = mock(RequestContext.class);
    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);

    when(parameter.isRequired()).thenReturn(false);
    when(sessionManager.getSession(context, false)).thenReturn(session);

    Object result = resolver.resolveArgument(context, parameter);

    assertThat(result).isSameAs(session);
  }

  @Test
  void resolveArgumentWithOptionalParameterReturnsNullWhenNoSession() {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionMethodArgumentResolver resolver = new SessionMethodArgumentResolver(sessionManager);

    RequestContext context = mock(RequestContext.class);
    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);

    when(parameter.isRequired()).thenReturn(false);
    when(sessionManager.getSession(context, false)).thenReturn(null);

    Object result = resolver.resolveArgument(context, parameter);

    assertThat(result).isNull();
  }

  @Test
  void constructorWithNullSessionManager() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new SessionMethodArgumentResolver(null))
            .withMessageContaining("SessionManager is required");
  }


}