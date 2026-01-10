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

import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.test.web.mock.request.MockMvcRequestBuilders;

/**
 * Builds a {@link HttpMockRequestImpl}.
 *
 * <p>See static factory methods in
 * {@link MockMvcRequestBuilders MockMvcRequestBuilders}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RequestBuilder {

  /**
   * Build the request.
   *
   * @param mockContext the {@link MockContext} to use to create the request
   * @return the request
   */
  HttpMockRequestImpl buildRequest(MockContext mockContext);

}
