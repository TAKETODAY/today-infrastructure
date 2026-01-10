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

package infra.web.socket.server;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;

/**
 * Thrown when handshake processing failed to complete due to an internal, unrecoverable
 * error. This implies a server error (HTTP status code 500) as opposed to a failure in
 * the handshake negotiation.
 *
 * <p>By contrast, when handshake negotiation fails, the response status code will be 200
 * and the response headers and body will have been updated to reflect the cause for the
 * failure. A {@link HandshakeHandler} implementation will have protected methods to
 * customize updates to the response in those cases.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HandshakeFailureException extends NestedRuntimeException {

  public HandshakeFailureException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
