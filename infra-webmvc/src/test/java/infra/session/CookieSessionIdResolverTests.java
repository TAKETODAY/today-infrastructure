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

import infra.http.HttpCookie;
import infra.session.config.CookieProperties;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/2 17:59
 */
class CookieSessionIdResolverTests {

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            SessionIdResolver.forCookie((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cookie name is required");

    assertThatThrownBy(() ->
            SessionIdResolver.forCookie((CookieProperties) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cookie config is required");
  }

  @Test
  void construct() {
    assertThat(new CookieSessionIdResolver().getCookieName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
    assertThat(SessionIdResolver.forCookie("cookie-name").getCookieName()).isEqualTo("cookie-name");
    assertThat(SessionIdResolver.forCookie(new CookieProperties()).getCookieName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
    CookieProperties config = new CookieProperties();
    config.setName(null);
    assertThat(SessionIdResolver.forCookie(config).getCookieName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
  }

  @Test
  void getSessionId() {
    CookieSessionIdResolver resolver = new CookieSessionIdResolver();
    MockRequestContext context = new MockRequestContext();
    // no response cookie
    assertThat(resolver.getSessionId(context)).isNull();

    // not found
    context.addCookie("test", "session-id");
    assertThat(resolver.getSessionId(context)).isNull();

    context.addCookie(resolver.getCookieName(), "session-id");
    assertThat(resolver.getSessionId(context)).isEqualTo("session-id");

    context.addRequestCookies(new HttpCookie(resolver.getCookieName(), "session-id"));
    assertThat(resolver.getSessionId(context)).isEqualTo("session-id");
    assertThat(context.getAttribute(CookieSessionIdResolver.WRITTEN_SESSION_ID_ATTR)).isNull();
  }

  @Test
  void setSessionId() {
    CookieSessionIdResolver resolver = new CookieSessionIdResolver();
    MockRequestContext context = new MockRequestContext();

    context.addRequestCookies(new HttpCookie(resolver.getCookieName(), "session-id"));
    assertThat(resolver.getSessionId(context)).isEqualTo("session-id");

    resolver.setSessionId(context, "new-session-id");

    assertThat(context.getAttribute(CookieSessionIdResolver.WRITTEN_SESSION_ID_ATTR)).isEqualTo("new-session-id");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-session-id");
  }

  @Test
  void expireSession() {
    CookieSessionIdResolver resolver = new CookieSessionIdResolver();
    MockRequestContext context = new MockRequestContext();

    resolver.setSessionId(context, "new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");

    resolver.expireSession(context);
    assertThat(resolver.getSessionId(context)).isNull();
  }

}