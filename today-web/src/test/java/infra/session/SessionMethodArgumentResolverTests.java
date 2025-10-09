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