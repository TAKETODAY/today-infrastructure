/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
