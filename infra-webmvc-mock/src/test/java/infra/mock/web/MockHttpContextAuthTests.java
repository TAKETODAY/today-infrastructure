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

package infra.mock.web;

import org.junit.jupiter.api.Test;

import java.security.Principal;

import infra.web.AuthHttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link AuthHttpContext} implementation of {@link MockHttpContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class MockHttpContextAuthTests {

  private final MockHttpContext context = new MockHttpContext();

  private final MockRequest request = context.request;

  @Test
  void isAuthenticatedCapable() {
    assertThat(context).isInstanceOf(AuthHttpContext.class);
  }

  @Test
  void getPrincipalIsNullByDefault() {
    assertThat(context.getPrincipal()).isNull();
  }

  @Test
  void getPrincipalDelegatesToRequest() {
    Principal principal = () -> "sadmin";
    request.setUserPrincipal(principal);

    assertThat(context.getPrincipal()).isSameAs(principal);
  }

  @Test
  void getUsernameIsNullByDefault() {
    assertThat(context.getUsername()).isNull();
  }

  @Test
  void getUsernameDerivesFromPrincipal() {
    request.setUserPrincipal(() -> "sadmin");

    assertThat(context.getUsername()).isEqualTo("sadmin");
  }

  @Test
  void isUserInRoleDelegatesToRequest() {
    request.addUserRole("ADMIN");

    assertThat(context.isUserInRole("ADMIN")).isTrue();
    assertThat(context.isUserInRole("USER")).isFalse();
  }

  @Test
  void isUserInRoleReturnsFalseWithoutRoles() {
    assertThat(context.isUserInRole("ADMIN")).isFalse();
  }

}