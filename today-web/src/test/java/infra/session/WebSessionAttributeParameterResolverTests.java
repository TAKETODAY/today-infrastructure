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
import infra.web.annotation.SessionAttribute;
import infra.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:12
 */
class WebSessionAttributeParameterResolverTests {

  @Test
  void constructorWithNullSessionManagerThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new WebSessionAttributeParameterResolver(null, null))
            .withMessageContaining("sessionManager is required");
  }

  @Test
  void supportsParameterWithSessionAttributeAnnotationReturnsTrue() {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSessionAttributeParameterResolver resolver = new WebSessionAttributeParameterResolver(sessionManager, null);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.hasParameterAnnotation(SessionAttribute.class)).thenReturn(true);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isTrue();
  }

  @Test
  void supportsParameterWithoutSessionAttributeAnnotationReturnsFalse() {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSessionAttributeParameterResolver resolver = new WebSessionAttributeParameterResolver(sessionManager, null);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.hasParameterAnnotation(SessionAttribute.class)).thenReturn(false);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isFalse();
  }

  @Test
  void resolveNameReturnsAttributeValueWhenSessionExists() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSessionAttributeParameterResolver resolver = new WebSessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);
    WebSession session = mock(WebSession.class);

    when(sessionManager.getSession(context, false)).thenReturn(session);
    when(session.getAttribute("testAttribute")).thenReturn("testValue");

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isEqualTo("testValue");
  }

  @Test
  void resolveNameReturnsNullWhenSessionDoesNotExist() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSessionAttributeParameterResolver resolver = new WebSessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);

    when(sessionManager.getSession(context, false)).thenReturn(null);

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isNull();
  }

  @Test
  void resolveNameReturnsNullWhenAttributeDoesNotExist() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    WebSessionAttributeParameterResolver resolver = new WebSessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);
    WebSession session = mock(WebSession.class);

    when(sessionManager.getSession(context, false)).thenReturn(session);
    when(session.getAttribute("testAttribute")).thenReturn(null);

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isNull();
  }

}