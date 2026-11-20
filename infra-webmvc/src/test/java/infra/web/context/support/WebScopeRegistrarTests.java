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

package infra.web.context.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.session.Session;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class WebScopeRegistrarTests {

  @AfterEach
  void cleanup() {
    HttpContextHolder.set(null);
  }

  @Test
  void registersScopesAndLazyDependenciesWithoutActiveRequest() {
    HttpContextHolder.set(null);
    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    WebScopeRegistrar.registerScopes(beanFactory);

    then(beanFactory).should().registerScope(HttpContext.SCOPE_REQUEST, RequestScope.instance);
    then(beanFactory).should().registerScope(eq(HttpContext.SCOPE_SESSION), isA(SessionScope.class));
    ArgumentCaptor<Object> requestDependency = ArgumentCaptor.forClass(Object.class);
    ArgumentCaptor<Object> sessionDependency = ArgumentCaptor.forClass(Object.class);
    then(beanFactory).should().registerResolvableDependency(eq(HttpContext.class), requestDependency.capture());
    then(beanFactory).should().registerResolvableDependency(eq(Session.class), sessionDependency.capture());
    then(beanFactory).shouldHaveNoMoreInteractions();

    HttpContext request = (HttpContext) requestDependency.getValue();
    Supplier<?> session = (Supplier<?>) sessionDependency.getValue();
    assertThatIllegalStateException().isThrownBy(request::getRequestURI);
    assertThatIllegalStateException().isThrownBy(session::get);

    MockHttpContext first = new MockHttpContext();
    first.setRequestURI("/first");
    HttpContextHolder.set(first);
    assertThat(request.getRequestURI()).isEqualTo("/first");
    assertThat(first.getSession(false)).isNull();
    assertThat(session.get()).isSameAs(first.getSession());

    MockHttpContext second = new MockHttpContext();
    second.setRequestURI("/second");
    HttpContextHolder.set(second);
    assertThat(request.getRequestURI()).isEqualTo("/second");
    assertThat(second.getSession(false)).isNull();
    assertThat(session.get()).isSameAs(second.getSession()).isNotSameAs(first.getSession());
  }

}
