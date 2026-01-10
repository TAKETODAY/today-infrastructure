/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.server;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.security.Principal;

import infra.http.HttpInputMessage;
import infra.http.HttpRequest;

/**
 * Represents a server-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0
 */
public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

  /**
   * Return a {@link Principal} instance containing the name of the
   * authenticated user.
   * <p>If the user has not been authenticated, the method returns <code>null</code>.
   */
  @Nullable
  Principal getPrincipal();

  /**
   * Return the address on which the request was received.
   */
  InetSocketAddress getLocalAddress();

  /**
   * Return the address of the remote client.
   */
  InetSocketAddress getRemoteAddress();

}
