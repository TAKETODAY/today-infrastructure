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

package infra.web.cors;

import infra.web.HttpContext;

/**
 * Handler for CORS pre-flight requests.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface PreFlightRequestHandler {

  /**
   * Handle a pre-flight request by finding and applying the CORS configuration
   * that matches the expected actual request. As a result of handling, the
   * response should be updated with CORS headers or rejected with
   * {@link infra.http.HttpStatus#FORBIDDEN}.
   *
   * @param context current HTTP context
   */
  void handlePreFlight(HttpContext context) throws Exception;

}
