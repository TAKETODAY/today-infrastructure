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

package infra.test.web.mock;

import infra.mock.web.HttpMockRequestImpl;
import infra.test.web.mock.request.RequestPostProcessor;

/**
 * Extended variant of a {@link RequestBuilder} that applies its
 * {@link RequestPostProcessor infra.test.web.mock.request.RequestPostProcessors}
 * as a separate step from the {@link #buildRequest} method.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SmartRequestBuilder extends RequestBuilder {

  /**
   * Apply request post-processing. Typically, that means invoking one or more
   * {@link RequestPostProcessor infra.test.web.mock.request.RequestPostProcessors}.
   *
   * @param request the request to initialize
   * @return the request to use, either the one passed in or a wrapped one
   */
  HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request);

}
