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

import java.security.Principal;

/**
 * An optional capability interface for {@link HttpContext} implementations
 * that provide access to the authenticated user associated with the request.
 *
 * <p>Unlike adding these methods directly to {@link HttpContext}, this interface
 * lets individual implementations opt in, keeping authentication concerns out of
 * the core request contract. Security infrastructure can detect whether the
 * current request context supports this capability and act accordingly:
 *
 * <pre>{@code
 * if (context instanceof AuthHttpContext authenticated) {
 *   Principal user = authenticated.getPrincipal();
 *   if (authenticated.isUserInRole("ADMIN")) {
 *     ...
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface AuthHttpContext {

  /**
   * Return the {@link Principal} of the authenticated user, or {@code null}
   * if the user has not been authenticated.
   *
   * @return the authenticated user's {@link Principal}, or {@code null}
   * @since 5.0
   */
  @Nullable
  Principal getPrincipal();

  /**
   * Return the username of the user making this request, or {@code null} if the
   * user has not been authenticated.
   *
   * <p>This is a convenience derived from {@link #getPrincipal()}.
   *
   * @return the username of the authenticated user, or {@code null}
   * @since 5.0
   */
  default @Nullable String getUsername() {
    Principal principal = getPrincipal();
    return principal != null ? principal.getName() : null;
  }

  /**
   * Return whether the authenticated user is included in the specified role.
   *
   * @param role the role to check; must not be {@code null}
   * @return {@code true} if the user is in the specified role, {@code false} otherwise
   * @since 5.0
   */
  boolean isUserInRole(String role);

}
