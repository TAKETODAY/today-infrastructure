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

import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.result.MockMvcResultMatchers;
import infra.test.web.mock.setup.MockMvcBuilders;

/**
 * Builds a {@link MockMvc} instance.
 *
 * <p>See static factory methods in
 * {@link MockMvcBuilders MockMvcBuilders}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MockMvcBuilder {

  /**
   * Build a {@link MockMvc} instance.
   */
  MockMvc build();

  /**
   * Perform a request and return a type that allows chaining further
   * actions, such as asserting expectations, on the result.
   *
   * @param requestBuilder used to prepare the request to execute;
   * see static factory methods in
   * {@link MockMvcRequestBuilders}
   * @return an instance of {@link ResultActions} (never {@code null})
   * @see MockMvcRequestBuilders
   * @see MockMvcResultMatchers
   */
  default ResultActions perform(RequestBuilder requestBuilder) throws Exception {
    MockMvc mockMvc = build();
    return mockMvc.perform(requestBuilder);
  }

}
