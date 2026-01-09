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

package infra.web.server;

import org.jspecify.annotations.Nullable;

import infra.web.DispatcherHandler;
import infra.web.RequestContext;

/**
 * Strategy interface for determining whether a request is expected to continue.
 *
 * <p>This resolver evaluates incoming requests and determines if they are
 * expected to continue processing based on specific criteria defined by
 * implementations.
 *
 * <p>The HTTP/1.1 specification defines the {@code Expect: 100-continue} mechanism to allow
 * a client to send a request header first and wait for server approval before sending the
 * request body. This helps avoid sending large request bodies when the server might reject
 * the request based on headers alone.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html#sec8.2.3">HTTP/1.1 100 Continue</a>
 * @since 5.0 2025/11/27 22:55
 */
public interface RequestContinueExpectedResolver {

  /**
   * Determine whether a request with an {@code Expect: 100-continue} header should proceed
   * with sending its request body.
   *
   * <p>This method is invoked when a request contains the {@code Expect: 100-continue} header.
   * It evaluates the provided {@link RequestContext} and returns a Boolean indicating the
   * server's decision:
   * <ul>
   *   <li>{@code true} - The server accepts the request; client should proceed with the request body</li>
   *   <li>{@code false} - The server rejects the request; client should not send the request body</li>
   *   <li>{@code null} - Decision cannot be made; defer to other mechanisms (e.g., content length checking)</li>
   * </ul>
   *
   * <p>The HTTP/1.1 {@code Expect: 100-continue} mechanism allows clients to send request headers
   * first and wait for server approval before transmitting the request body. This prevents
   * unnecessary transmission of large request bodies that the server might reject based on headers.
   *
   * <p>In the framework's processing flow, this resolver
   * determines whether to send a 100 (Continue) response or a 417 (Expectation Failed) response:
   * <ul>
   *   <li>{@code true}: Sends 100 (Continue), allowing the client to proceed with the request body</li>
   *   <li>{@code false}: Sends 417 (Expectation Failed), rejecting the request before body transmission</li>
   *   <li>{@code null}: Defers decision to alternative mechanisms like content length validation</li>
   * </ul>
   *
   * <p>Implementations typically examine request properties such as content type, content length,
   * authentication credentials, or other application-specific criteria to make the decision.
   *
   * @param request the {@link RequestContext} containing the request with {@code Expect: 100-continue} header
   * @return Boolean result indicating if request should continue, or null if undetermined
   * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html#sec8.2.3">HTTP/1.1 100 Continue</a>
   * @see DispatcherHandler#requestContinueExpected(RequestContext)
   */
  @Nullable
  Boolean shouldContinue(RequestContext request);

}
