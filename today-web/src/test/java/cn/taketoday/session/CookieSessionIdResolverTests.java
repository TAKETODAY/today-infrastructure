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

import cn.taketoday.http.HttpCookie;
import cn.taketoday.session.config.CookieProperties;
import cn.taketoday.web.mock.MockRequestContext;

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

    CookieProperties config = new CookieProperties();
    config.setName(null);
    assertThatThrownBy(() ->
            SessionIdResolver.forCookie(config))
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
    assertThat(new CookieSessionIdResolver("cookie-name").getCookieName()).isEqualTo("cookie-name");
    assertThat(new CookieSessionIdResolver(new CookieProperties()).getCookieName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
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