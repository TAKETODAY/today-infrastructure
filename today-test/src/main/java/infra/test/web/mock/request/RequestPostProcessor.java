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

import infra.mock.web.HttpMockRequestImpl;

/**
 * Extension point for applications or 3rd party libraries that wish to further
 * initialize a {@link HttpMockRequestImpl} instance after it has been built
 * by {@link MockHttpRequestBuilder} or its subclass
 * {@link MockMultipartHttpRequestBuilder}.
 *
 * <p>Implementations of this interface can be provided to
 * {@link MockHttpRequestBuilder#with(RequestPostProcessor)} at the time
 * when a request is about to be constructed.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
@FunctionalInterface
public interface RequestPostProcessor {

  /**
   * Post-process the given {@code MockHttpServletRequest} after its creation
   * and initialization through a {@code MockHttpServletRequestBuilder}.
   *
   * @param request the request to initialize
   * @return the request to use, either the one passed in or a wrapped one
   */
  HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request);

}
