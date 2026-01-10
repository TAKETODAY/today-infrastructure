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

package infra.web.accept;

import infra.web.RequestContext;

/**
 * Contract to add handling of requests with a deprecated API version. Typically,
 * this involves use of response headers to send hints and information about
 * the deprecated version to clients.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see StandardApiVersionDeprecationHandler
 * @since 5.0
 */
public interface ApiVersionDeprecationHandler {

  /**
   * Check if the requested API version is deprecated, and if so handle it
   * accordingly, e.g. by setting response headers to signal the deprecation,
   * to specify relevant dates and provide links to further details.
   *
   * @param version the resolved and parsed request version
   * @param handler the handler chosen for the request
   * @param request the current request
   */
  void handleVersion(Comparable<?> version, Object handler, RequestContext request);

}
