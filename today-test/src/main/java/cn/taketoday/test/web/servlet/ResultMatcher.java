/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet;

import cn.taketoday.test.web.servlet.result.MockMvcResultMatchers;

/**
 * A {@code ResultMatcher} matches the result of an executed request against
 * some expectation.
 *
 * <p>See static factory methods in
 * {@link MockMvcResultMatchers
 * MockMvcResultMatchers}.
 *
 * <h3>Example Using Status and Content Result Matchers</h3>
 *
 * <pre class="code">
 * import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *   .andExpectAll(
 *       status().isOk(),
 *       content().mimeType(MediaType.APPLICATION_JSON));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@FunctionalInterface
public interface ResultMatcher {

  /**
   * Assert the result of an executed request.
   *
   * @param result the result of the executed request
   * @throws Exception if a failure occurs
   */
  void match(MvcResult result) throws Exception;

  /**
   * Static method for matching with an array of result matchers.
   *
   * @param matchers the matchers
   * @since 4.0
   * @deprecated as of Spring Framework 5.3.10, in favor of
   * {@link ResultActions#andExpectAll(ResultMatcher...)}
   */
  @Deprecated
  static ResultMatcher matchAll(ResultMatcher... matchers) {
    return result -> {
      for (ResultMatcher matcher : matchers) {
        matcher.match(result);
      }
    };
  }

}
