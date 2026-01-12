/*
 * Copyright 2012-present the original author or authors.
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

package infra.webmvc.error;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.web.RequestContext;
import infra.web.annotation.ResponseBody;
import infra.web.view.ModelAndView;

/**
 * Provides access to error attributes which can be logged or presented to the user.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultErrorAttributes
 * @since 4.0
 */
public interface ErrorAttributes {

  /**
   * Returns a {@link Map} of the error attributes. The map can be used as the model of
   * an error page {@link ModelAndView}, or returned as a
   * {@link ResponseBody @ResponseBody}.
   *
   * @param context the source request
   * @param options options for error attribute contents
   * @return a map of error attributes
   */
  Map<String, Object> getErrorAttributes(RequestContext context, ErrorAttributeOptions options);

  /**
   * Return the underlying cause of the error or {@code null} if the error cannot be
   * extracted.
   *
   * @param webRequest the source request
   * @return the {@link Exception} that caused the error or {@code null}
   */
  @Nullable
  Throwable getError(RequestContext webRequest);

}
