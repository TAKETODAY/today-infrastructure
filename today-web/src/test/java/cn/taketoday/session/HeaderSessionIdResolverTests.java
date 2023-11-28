/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.session;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/2 17:05
 */
class HeaderSessionIdResolverTests {

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            new HeaderSessionIdResolver(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("headerName is required");
  }

  @Test
  void getSessionId() {
    HeaderSessionIdResolver resolver = HeaderSessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();
    context.requestHeaders().set(HeaderSessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context))
            .isEqualTo("value");
  }

  @Test
  void setSessionId() {
    HeaderSessionIdResolver resolver = HeaderSessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();

    context.requestHeaders().set(HeaderSessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context)).isEqualTo("value");

    resolver.setSessionId(context, "new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");
  }

  @Test
  void expireSession() {
    HeaderSessionIdResolver resolver = HeaderSessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();

    context.requestHeaders().set(HeaderSessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context)).isEqualTo("value");

    resolver.setSessionId(context, "new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");

    resolver.expireSession(context);
    assertThat(resolver.getSessionId(context)).isEqualTo("value");
  }

  @Test
  void authenticationInfo() {
    HeaderSessionIdResolver resolver = HeaderSessionIdResolver.authenticationInfo();
    assertThat(resolver).extracting("headerName").isEqualTo(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO);
  }
}