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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.http.ProblemDetail;
import infra.web.view.ModelAndView;

/**
 * Strategy to render an RFC 9457 {@link ProblemDetail} as a view instead of
 * writing it with message converters when the client prefers HTML.
 * <p>Applies to problem details, error responses, and response entities returned
 * by exception handler methods. Error response interceptors run before resolution.
 * @author Brian Clozel
 * @since 5.0
 */
@FunctionalInterface
public interface ErrorResponseViewResolver {

  /**
   * Resolve a view for the given problem detail.
   * <p>The view may declare a status; otherwise the entity or problem status is used.
   * @param request the current request
   * @param problemDetail the detail to render
   * @param errorResponse the originating error response, if any
   * @return the view, or {@code null} to use message converters
   */
  @Nullable ModelAndView resolveErrorView(HttpContext request, ProblemDetail problemDetail,
          @Nullable ErrorResponse errorResponse);
}
