/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web;

import org.junit.jupiter.api.Test;

import infra.session.Session;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class HttpContextSessionAndNativeContextTests {

  @Test
  void sessionReadsDoNotCreateSession() {
    MockHttpContext context = new MockHttpContext();

    assertThat(context.getSessionId()).isNull();
    assertThat(context.getSessionAttribute("missing")).isNull();
    assertThatIllegalStateException()
            .isThrownBy(() -> context.getRequiredSessionAttribute("missing"))
            .withMessage("No session attribute 'missing' found");
    assertThat(context.getSession(false)).isNull();
  }

  @Test
  void sessionReadsUseExistingSession() {
    MockHttpContext context = new MockHttpContext();
    Session session = context.getSession();
    Object value = new Object();
    session.setAttribute("value", value);

    assertThat(context.getSessionId()).isEqualTo(session.getId());
    assertThat(context.getSessionAttribute("value")).isSameAs(value);
    assertThat(context.getRequiredSessionAttribute("value")).isSameAs(value);
    assertThat(context.getSessionAttribute("missing")).isNull();
    assertThatIllegalStateException()
            .isThrownBy(() -> context.getRequiredSessionAttribute("missing"))
            .withMessage("No session attribute 'missing' found");
    assertThat(context.getSession(false)).isSameAs(session);
  }

  @Test
  void settingSessionAttributeCreatesSessionAndReplacesValue() {
    MockHttpContext context = new MockHttpContext();
    Object first = new Object();
    Object second = new Object();

    context.setSessionAttribute("value", first);
    Session session = context.getSession(false);
    assertThat(session).isNotNull();
    assertThat(context.getSessionAttribute("value")).isSameAs(first);

    context.setSessionAttribute("value", second);
    assertThat(context.getSession(false)).isSameAs(session);
    assertThat(context.getRequiredSessionAttribute("value")).isSameAs(second);
  }

  @Test
  void removingSessionAttributeDoesNotCreateSession() {
    MockHttpContext context = new MockHttpContext();
    context.setSessionAttribute("missing", null);
    assertThat(context.getSession(false)).isNull();

    context.setSessionAttribute("value", "remove");
    context.setSessionAttribute("other", "keep");
    Session session = context.getSession(false);
    context.setSessionAttribute("value", null);
    context.setSessionAttribute("missing", null);

    assertThat(context.getSession(false)).isSameAs(session);
    assertThat(context.getSessionAttribute("value")).isNull();
    assertThat(context.getSessionAttribute("other")).isEqualTo("keep");
  }

  @Test
  void sessionAttributeRemovalUsesRemoveRatherThanNullAssignment() {
    HttpContext context = mock(HttpContext.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    given(context.getSession(false)).willReturn(session);

    context.setSessionAttribute("value", null);

    then(session).should().removeAttribute("value");
    then(session).shouldHaveNoMoreInteractions();
    then(context).should(never()).getSession();
    then(context).should(never()).getSession(true);
  }

  @Test
  void sessionFailuresArePropagated() {
    HttpContext context = mock(HttpContext.class, CALLS_REAL_METHODS);
    IllegalStateException failure = new IllegalStateException("session unavailable");
    given(context.getSession(false)).willThrow(failure);
    given(context.getSession(true)).willThrow(failure);

    assertThatThrownBy(context::getSessionId).isSameAs(failure);
    assertThatThrownBy(() -> context.getSessionAttribute("value")).isSameAs(failure);
    assertThatThrownBy(() -> context.getRequiredSessionAttribute("value")).isSameAs(failure);
    assertThatThrownBy(() -> context.setSessionAttribute("value", null)).isSameAs(failure);
    assertThatThrownBy(() -> context.setSessionAttribute("value", "new")).isSameAs(failure);
  }

  @Test
  void sessionMethodsPreserveDelegateOverrides() {
    HttpContext target = mock(HttpContext.class);
    HttpContext wrapper = new DelegatingHttpContext(target);
    Object value = new Object();
    given(target.getSessionAttribute("value")).willReturn(value);
    given(target.getRequiredSessionAttribute("required")).willReturn(value);
    given(target.getSessionId()).willReturn("custom-id");

    assertThat(wrapper.getSessionAttribute("value")).isSameAs(value);
    assertThat(wrapper.getRequiredSessionAttribute("required")).isSameAs(value);
    assertThat(wrapper.getSessionId()).isEqualTo("custom-id");
    wrapper.setSessionAttribute("value", value);
    wrapper.setSessionAttribute("value", null);

    then(target).should().getSessionAttribute("value");
    then(target).should().getRequiredSessionAttribute("required");
    then(target).should().getSessionId();
    then(target).should().setSessionAttribute("value", value);
    then(target).should().setSessionAttribute("value", null);
    then(target).shouldHaveNoMoreInteractions();
  }

  @Test
  void nativeContextMatchesSelfOrReturnsNull() {
    MockHttpContext context = new MockHttpContext();
    assertThat(context.<Object>getNativeContext(null)).isNull();
    assertThat(context.getNativeContext(HttpContext.class)).isSameAs(context);
    assertThat(context.nativeContext(MockHttpContext.class)).isSameAs(context);
    assertThat(context.getNativeContext(String.class)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> context.nativeContext(String.class))
            .withMessage("No native context of type java.lang.String is available");
  }

  @Test
  void nativeContextUnwrapsNestedDecoratorsAndPrefersOuterMatch() {
    MockHttpContext target = new MockHttpContext();
    HttpContext inner = new DelegatingHttpContext(target);
    HttpContext outer = new DelegatingHttpContext(inner);

    assertThat(outer.<Object>getNativeContext(null)).isNull();
    assertThat(outer.getNativeContext(HttpContext.class)).isSameAs(outer);
    assertThat(outer.getNativeContext(AbstractDelegatingHttpContext.class)).isSameAs(outer);
    assertThat(outer.nativeContext(DelegatingHttpContext.class)).isSameAs(outer);
    assertThat(outer.nativeContext(MockHttpContext.class)).isSameAs(target);
    assertThat(outer.getNativeContext(String.class)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> outer.nativeContext(String.class));
  }

  @Test
  void nativeContextLookupDoesNotInvokeDelegateOverride() {
    MockHttpContext target = mock(MockHttpContext.class);
    given(target.getNativeContext(String.class)).willReturn("custom-native-context");
    HttpContext outer = new DelegatingHttpContext(new DelegatingHttpContext(target));

    assertThat(outer.getNativeContext(MockHttpContext.class)).isSameAs(target);
    assertThat(outer.getNativeContext(String.class)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> outer.nativeContext(String.class))
            .withMessage("No native context of type java.lang.String is available");
    then(target).shouldHaveNoMoreInteractions();
  }

}
