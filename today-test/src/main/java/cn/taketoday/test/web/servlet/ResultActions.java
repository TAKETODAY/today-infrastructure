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

import cn.taketoday.test.util.ExceptionCollector;
import cn.taketoday.test.web.servlet.result.MockMvcResultHandlers;
import cn.taketoday.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Allows applying actions, such as expectations, on the result of an executed
 * request.
 *
 * <p>See static factory methods in
 * {@link MockMvcResultMatchers} and
 * {@link MockMvcResultHandlers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Michał Rowicki
 * @since 4.0
 */
public interface ResultActions {

  /**
   * Perform an expectation.
   *
   * <h4>Example</h4>
   * <p>You can invoke {@code andExpect()} multiple times as in the following
   * example.
   * <pre class="code">
   * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
   *
   * mockMvc.perform(get("/person/1"))
   *   .andExpect(status().isOk())
   *   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
   *   .andExpect(jsonPath("$.person.name").value("Jason"));
   * </pre>
   *
   * @see #andExpectAll(ResultMatcher...)
   */
  ResultActions andExpect(ResultMatcher matcher) throws Exception;

  /**
   * Perform multiple expectations, with the guarantee that all expectations
   * will be asserted even if one or more expectations fail with an exception.
   * <p>If a single {@link Error} or {@link Exception} is thrown, it will
   * be rethrown.
   * <p>If multiple exceptions are thrown, this method will throw an
   * {@link AssertionError} whose error message is a summary of all of the
   * exceptions. In addition, each exception will be added as a
   * {@linkplain Throwable#addSuppressed(Throwable) suppressed exception} to
   * the {@code AssertionError}.
   * <p>This feature is similar to the {@code SoftAssertions} support in AssertJ
   * and the {@code assertAll()} support in JUnit Jupiter.
   *
   * <h4>Example</h4>
   * <p>Instead of invoking {@code andExpect()} multiple times, you can invoke
   * {@code andExpectAll()} as in the following example.
   * <pre class="code">
   * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
   *
   * mockMvc.perform(get("/person/1"))
   *   .andExpectAll(
   *       status().isOk(),
   *       content().contentType(MediaType.APPLICATION_JSON),
   *       jsonPath("$.person.name").value("Jason")
   *   );
   * </pre>
   *
   * @see #andExpect(ResultMatcher)
   * @since 5.3.10
   */
  default ResultActions andExpectAll(ResultMatcher... matchers) throws Exception {
    ExceptionCollector exceptionCollector = new ExceptionCollector();
    for (ResultMatcher matcher : matchers) {
      exceptionCollector.execute(() -> this.andExpect(matcher));
    }
    exceptionCollector.assertEmpty();
    return this;
  }

  /**
   * Perform a general action.
   *
   * <h4>Example</h4>
   * <pre class="code">
   * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
   *
   * mockMvc.perform(get("/form")).andDo(print());
   * </pre>
   */
  ResultActions andDo(ResultHandler handler) throws Exception;

  /**
   * Return the result of the executed request for direct access to the results.
   *
   * @return the result of the request
   */
  MvcResult andReturn();

}
