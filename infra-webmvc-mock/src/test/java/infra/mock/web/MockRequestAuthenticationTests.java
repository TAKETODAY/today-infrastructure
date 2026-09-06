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
import java.util.Set;

import infra.web.mock.MockRequest;
import infra.web.mock.api.MockException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for authentication-related methods of {@link MockRequest}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class MockRequestAuthenticationTests {

  private final MockRequest request = new MockRequest();

  @Test
  void userPrincipalIsNullByDefault() {
    assertThat(request.getUserPrincipal()).isNull();
  }

  @Test
  void setUserPrincipalReturnsSameInstance() {
    Principal principal = () -> "sadmin";

    request.setUserPrincipal(principal);

    assertThat(request.getUserPrincipal()).isSameAs(principal);
  }

  @Test
  void setUserPrincipalToNullClearsPrincipal() {
    request.setUserPrincipal(() -> "sadmin");
    request.setUserPrincipal(null);

    assertThat(request.getUserPrincipal()).isNull();
  }

  @Test
  void addUserRoleMakesUserInRole() {
    request.addUserRole("ADMIN");

    assertThat(request.isUserInRole("ADMIN")).isTrue();
  }

  @Test
  void isUserInRoleReturnsFalseWithoutRoles() {
    assertThat(request.isUserInRole("ADMIN")).isFalse();
  }

  @Test
  void setUserRolesReplacesPreviousRoles() {
    request.addUserRole("ADMIN");
    request.setUserRoles("USER", "GUEST");

    assertThat(request.getUserRoles()).containsExactly("USER", "GUEST");
    assertThat(request.isUserInRole("ADMIN")).isFalse();
    assertThat(request.isUserInRole("USER")).isTrue();
  }

  @Test
  void varArgsUserRoles() {
    request.setUserRoles("ADMIN", "USER");

    assertThat(request.getUserRoles()).containsExactly("ADMIN", "USER");
  }

  @Test
  void getUserRolesIsUnmodifiable() {
    request.addUserRole("ADMIN");

    Set<String> roles = request.getUserRoles();

    assertThatThrownBy(() -> roles.add("USER")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void logoutClearsPrincipalAndRoles() throws MockException {
    request.setUserPrincipal(() -> "sadmin");
    request.addUserRole("ADMIN");

    request.logout();

    assertThat(request.getUserPrincipal()).isNull();
    assertThat(request.getUserRoles()).isEmpty();
    assertThat(request.isUserInRole("ADMIN")).isFalse();
  }

}