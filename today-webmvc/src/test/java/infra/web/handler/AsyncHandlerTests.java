/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 23:18
 */
class AsyncHandlerTests {
  @Test
  void wrapConcurrentResultWithNullValue() {
    AsyncHandler asyncHandler = new AsyncHandler() { };

    Object result = asyncHandler.wrapConcurrentResult(null);

    assertThat(result).isSameAs(asyncHandler);
  }

  @Test
  void wrapConcurrentResultWithNonNullValue() {
    AsyncHandler asyncHandler = new AsyncHandler() { };
    Object asyncResult = new Object();

    Object result = asyncHandler.wrapConcurrentResult(asyncResult);

    assertThat(result).isSameAs(asyncHandler);
  }

  @Test
  void wrapConcurrentResultWithStringValue() {
    AsyncHandler asyncHandler = new AsyncHandler() { };
    String asyncResult = "testResult";

    Object result = asyncHandler.wrapConcurrentResult(asyncResult);

    assertThat(result).isSameAs(asyncHandler);
  }

  @Test
  void wrapConcurrentResultWithIntegerValue() {
    AsyncHandler asyncHandler = new AsyncHandler() { };
    Integer asyncResult = 42;

    Object result = asyncHandler.wrapConcurrentResult(asyncResult);

    assertThat(result).isSameAs(asyncHandler);
  }

}