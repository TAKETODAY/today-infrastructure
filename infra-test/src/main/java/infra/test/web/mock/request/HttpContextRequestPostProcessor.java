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

package infra.test.web.mock.request;

import infra.web.HttpContext;

/**
 * Extension point for applications or 3rd party libraries that wish to further
 * initialize an {@link HttpContext} instance after it has been built by a
 * {@link infra.test.web.mock.TestingHttpContext} builder.
 *
 * <p>Unlike {@link RequestPostProcessor}, which operates on the servlet-style
 * {@link infra.web.mock.MockRequest}, this processor operates on the
 * {@link HttpContext} itself &mdash; so it runs once the context (including its
 * application context, attributes and response) is fully available.
 *
 * @param <T> the concrete {@link HttpContext} type being processed
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface HttpContextRequestPostProcessor<T extends HttpContext> {

  /**
   * Post-process the given context after its creation and initialization
   * through a builder.
   *
   * @param request the context to initialize
   * @return the context to use, either the one passed in or a wrapper of it
   */
  T postProcessRequest(T request);

}