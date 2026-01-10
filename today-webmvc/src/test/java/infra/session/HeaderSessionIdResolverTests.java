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

    assertThatThrownBy(() ->
            new HeaderSessionIdResolver(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("headerName is required");
    assertThatThrownBy(() ->
            new HeaderSessionIdResolver("  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("headerName is required");
  }

  @Test
  void getSessionId() {
    HeaderSessionIdResolver resolver = SessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();
    context.requestHeaders().setOrRemove(SessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context))
            .isEqualTo("value");
  }

  @Test
  void setSessionId() {
    HeaderSessionIdResolver resolver = SessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();

    context.requestHeaders().setOrRemove(SessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context)).isEqualTo("value");

    resolver.setSessionId(context, "new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");
  }

  @Test
  void expireSession() {
    HeaderSessionIdResolver resolver = SessionIdResolver.xAuthToken();
    MockRequestContext context = new MockRequestContext();

    context.requestHeaders().setOrRemove(SessionIdResolver.HEADER_X_AUTH_TOKEN, "value");
    assertThat(resolver.getSessionId(context)).isEqualTo("value");

    resolver.setSessionId(context, "new-value");
    assertThat(resolver.getSessionId(context)).isEqualTo("new-value");

    resolver.expireSession(context);
    assertThat(resolver.getSessionId(context)).isEqualTo("value");
  }

  @Test
  void authenticationInfo() {
    HeaderSessionIdResolver resolver = SessionIdResolver.authenticationInfo();
    assertThat(resolver).extracting("headerName").isEqualTo(SessionIdResolver.HEADER_AUTHENTICATION_INFO);
  }
}