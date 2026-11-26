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

package infra.web;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link AuthHttpExchange}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class AuthServerHttpExchangeTests {

  @Test
  void getUsernameWhenPrincipalPresent() {
    Principal principal = () -> "sadmin";
    AuthHttpExchange context = new SimpleAuthHttpExchange(principal, Set.of());

    assertThat(context.getUsername()).isEqualTo("sadmin");
  }

  @Test
  void getUsernameWhenNoPrincipal() {
    AuthHttpExchange context = new SimpleAuthHttpExchange(null, Set.of());

    assertThat(context.getUsername()).isNull();
  }

  @Test
  void isUserInRoleReturnsTrueForGrantedRole() {
    AuthHttpExchange context = new SimpleAuthHttpExchange(() -> "sadmin", Set.of("ADMIN"));

    assertThat(context.isUserInRole("ADMIN")).isTrue();
  }

  @Test
  void isUserInRoleReturnsFalseForUngrantedRole() {
    AuthHttpExchange context = new SimpleAuthHttpExchange(() -> "sadmin", Set.of("USER"));

    assertThat(context.isUserInRole("ADMIN")).isFalse();
  }

  @Test
  void isUserInRoleReturnsFalseWhenNoRoles() {
    AuthHttpExchange context = new SimpleAuthHttpExchange(() -> "sadmin", Set.of());

    assertThat(context.isUserInRole("ADMIN")).isFalse();
  }

  @Test
  void getPrincipalReturnsPrincipal() {
    Principal principal = () -> "sadmin";
    AuthHttpExchange context = new SimpleAuthHttpExchange(principal, Set.of("ADMIN"));

    assertThat(context.getPrincipal()).isSameAs(principal);
  }

  @Test
  void getNativeContextFindsAuthHttpContextDirectly() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class, withSettings().extraInterfaces(AuthHttpExchange.class));

    AuthHttpExchange found = WebUtils.getNativeContext(delegate, AuthHttpExchange.class);

    assertThat(found).isSameAs(delegate);
  }

  @Test
  void getNativeContextUnwrapsDecoratorToFindAuthHttpContext() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class, withSettings().extraInterfaces(AuthHttpExchange.class));
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    AuthHttpExchange found = WebUtils.getNativeContext(wrapper, AuthHttpExchange.class);

    assertThat(found).isSameAs(delegate);
    assertThat(wrapper).isNotInstanceOf(AuthHttpExchange.class);
  }

  @Test
  void getNativeContextReturnsNullForPlainHttpContext() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    AuthHttpExchange found = WebUtils.getNativeContext(wrapper, AuthHttpExchange.class);

    assertThat(found).isNull();
  }

  private record SimpleAuthHttpExchange(@Nullable Principal principal, Set<String> roles) implements AuthHttpExchange {

    private SimpleAuthHttpExchange(@Nullable Principal principal, Set<String> roles) {
      this.principal = principal;
      this.roles = new LinkedHashSet<>(roles);
    }

    @Override
    public @Nullable Principal getPrincipal() {
      return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
      return roles.contains(role);
    }
  }

}
