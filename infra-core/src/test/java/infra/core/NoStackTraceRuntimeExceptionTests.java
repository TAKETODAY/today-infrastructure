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

package infra.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:07
 */
class NoStackTraceRuntimeExceptionTests {

  @Test
  void noStackTraceInErrorMessage() {
    var exception = new NoStackTraceRuntimeException("test message", null);
    assertThat(exception).hasMessage("test message")
            .hasNoSuppressedExceptions()
            .hasNoCause();
    assertThat(exception.getStackTrace()).isEmpty();
  }

  @Test
  void fillInStackTraceReturnsSameInstance() {
    var exception = new NoStackTraceRuntimeException("test", null);
    assertThat(exception.fillInStackTrace()).isSameAs(exception);
  }

  @Test
  void causeIsPreservedButStackTraceIsEmpty() {
    var cause = new RuntimeException("cause");
    var exception = new NoStackTraceRuntimeException("message", cause);
    assertThat(exception)
            .hasMessage("message")
            .hasCause(cause);
    assertThat(exception.getStackTrace()).isEmpty();
  }

  @Test
  void constructWithNullMessageAndCause() {
    var exception = new NoStackTraceRuntimeException(null, null);
    assertThat(exception)
            .hasMessage(null)
            .hasNoCause();
    assertThat(exception.getStackTrace()).isEmpty();
  }

  @Test
  void nestedExceptionBehaviorPreserved() {
    var cause = new RuntimeException("root cause");
    var exception = new NoStackTraceRuntimeException("message", cause);

    assertThat(exception.getNestedMessage())
            .isEqualTo("message; nested exception is java.lang.RuntimeException: root cause");
    assertThat(exception.getRootCause()).isSameAs(cause);
    assertThat(exception.getMostSpecificCause()).isSameAs(cause);
  }

}