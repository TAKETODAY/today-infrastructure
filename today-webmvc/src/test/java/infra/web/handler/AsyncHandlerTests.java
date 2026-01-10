/*
 * Copyright 2017 - 2026 the TODAY authors.
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