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