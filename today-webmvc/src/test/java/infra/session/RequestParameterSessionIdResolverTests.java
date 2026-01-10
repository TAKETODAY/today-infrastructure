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

import infra.web.mock.MockRequestContext;

import static infra.session.SessionIdResolver.WRITTEN_SESSION_ID_ATTR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/1 19:57
 */
class RequestParameterSessionIdResolverTests {

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            new RequestParameterSessionIdResolver(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("parameterName is required");

    assertThatThrownBy(() ->
            new RequestParameterSessionIdResolver(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("parameterName is required");

    assertThatThrownBy(() ->
            new RequestParameterSessionIdResolver("    "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("parameterName is required");
  }

  @Test
  void getSessionId() {
    SessionIdResolver resolver = SessionIdResolver.forParameter("auth");
    MockRequestContext context = new MockRequestContext();
    context.setParameter("auth", "sessionId");
    assertThat(resolver.getSessionId(context))
            .isEqualTo("sessionId");
  }

  @Test
  void setSessionId() {
    RequestParameterSessionIdResolver resolver = SessionIdResolver.forParameter("auth");
    MockRequestContext context = new MockRequestContext();
    context.setParameter("auth", "sessionId");

    assertThat(resolver.getSessionId(context)).isEqualTo("sessionId");

    resolver.setSessionId(context, "new-value");
    assertThat(context.getAttribute(WRITTEN_SESSION_ID_ATTR)).isEqualTo("new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");
  }

  @Test
  void expireSession() {
    RequestParameterSessionIdResolver resolver = SessionIdResolver.forParameter("auth");
    MockRequestContext context = new MockRequestContext();
    context.setParameter("auth", "sessionId");

    assertThat(resolver.getSessionId(context)).isEqualTo("sessionId");

    resolver.setSessionId(context, "new-value");
    assertThat(context.getAttribute(WRITTEN_SESSION_ID_ATTR)).isEqualTo("new-value");

    resolver.expireSession(context);
    assertThat(resolver.getSessionId(context)).isEqualTo("sessionId");
    assertThat(context.getAttribute(WRITTEN_SESSION_ID_ATTR)).isNull();
  }

}