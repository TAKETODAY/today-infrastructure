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
class SessionAttributeParameterResolverTests {

  @Test
  void constructorWithNullSessionManagerThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new SessionAttributeParameterResolver(null, null))
            .withMessageContaining("sessionManager is required");
  }

  @Test
  void supportsParameterWithSessionAttributeAnnotationReturnsTrue() {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionAttributeParameterResolver resolver = new SessionAttributeParameterResolver(sessionManager, null);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.hasParameterAnnotation(SessionAttribute.class)).thenReturn(true);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isTrue();
  }

  @Test
  void supportsParameterWithoutSessionAttributeAnnotationReturnsFalse() {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionAttributeParameterResolver resolver = new SessionAttributeParameterResolver(sessionManager, null);

    ResolvableMethodParameter parameter = mock(ResolvableMethodParameter.class);
    when(parameter.hasParameterAnnotation(SessionAttribute.class)).thenReturn(false);

    boolean result = resolver.supportsParameter(parameter);

    assertThat(result).isFalse();
  }

  @Test
  void resolveNameReturnsAttributeValueWhenSessionExists() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionAttributeParameterResolver resolver = new SessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);

    when(sessionManager.getSession(context, false)).thenReturn(session);
    when(session.getAttribute("testAttribute")).thenReturn("testValue");

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isEqualTo("testValue");
  }

  @Test
  void resolveNameReturnsNullWhenSessionDoesNotExist() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionAttributeParameterResolver resolver = new SessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);

    when(sessionManager.getSession(context, false)).thenReturn(null);

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isNull();
  }

  @Test
  void resolveNameReturnsNullWhenAttributeDoesNotExist() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    SessionAttributeParameterResolver resolver = new SessionAttributeParameterResolver(sessionManager, null);

    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);

    when(sessionManager.getSession(context, false)).thenReturn(session);
    when(session.getAttribute("testAttribute")).thenReturn(null);

    Object result = resolver.resolveName("testAttribute", mock(ResolvableMethodParameter.class), context);

    assertThat(result).isNull();
  }

}