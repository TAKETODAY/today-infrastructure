/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.session;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.mock.MockRequestContext;

import static cn.taketoday.session.SessionIdResolver.WRITTEN_SESSION_ID_ATTR;
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