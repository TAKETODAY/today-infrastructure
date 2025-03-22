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
class NestedCheckedExceptionTests {

  @Test
  void nestedMessageIncludesRootCauseMessage() {
    IllegalArgumentException rootCause = new IllegalArgumentException("Root cause");
    CustomNestedException exception = new CustomNestedException("Outer message", rootCause);
    assertThat(exception.getNestedMessage()).isEqualTo("Outer message; nested exception is java.lang.IllegalArgumentException: Root cause");
  }

  @Test
  void nestedMessageWithNoCauseReturnsMainMessage() {
    CustomNestedException exception = new CustomNestedException("Main message");
    assertThat(exception.getNestedMessage()).isEqualTo("Main message");
  }

  @Test
  void getRootCauseReturnsDeepestNestedException() {
    IllegalArgumentException deepest = new IllegalArgumentException("Deepest");
    RuntimeException middle = new RuntimeException("Middle", deepest);
    CustomNestedException exception = new CustomNestedException("Outer", middle);
    assertThat(exception.getRootCause()).isSameAs(deepest);
  }

  @Test
  void getRootCauseWithNoCauseReturnsNull() {
    CustomNestedException exception = new CustomNestedException("Message");
    assertThat(exception.getRootCause()).isNull();
  }

  @Test
  void getMostSpecificCauseReturnsRootCause() {
    IllegalArgumentException rootCause = new IllegalArgumentException("Root");
    CustomNestedException exception = new CustomNestedException("Outer", rootCause);
    assertThat(exception.getMostSpecificCause()).isSameAs(rootCause);
  }

  @Test
  void getMostSpecificCauseWithNoCauseReturnsSelf() {
    CustomNestedException exception = new CustomNestedException("Message");
    assertThat(exception.getMostSpecificCause()).isSameAs(exception);
  }

  @Test
  void containsReturnsTrueForExactExceptionType() {
    CustomNestedException exception = new CustomNestedException("Message");
    assertThat(exception.contains(CustomNestedException.class)).isTrue();
  }

  @Test
  void containsReturnsTrueForNestedExceptionType() {
    IllegalArgumentException cause = new IllegalArgumentException("Cause");
    CustomNestedException exception = new CustomNestedException("Message", cause);
    assertThat(exception.contains(IllegalArgumentException.class)).isTrue();
  }

  @Test
  void containsReturnsFalseForUnrelatedType() {
    CustomNestedException exception = new CustomNestedException("Message");
    assertThat(exception.contains(NullPointerException.class)).isFalse();
  }

  @Test
  void containsWithNullTypeReturnsFalse() {
    CustomNestedException exception = new CustomNestedException("Message");
    assertThat(exception.contains(null)).isFalse();
  }

  @Test
  void getMessageReturnsOriginalMessage() {
    CustomNestedException exception = new CustomNestedException("Test message", new RuntimeException());
    assertThat(exception.getMessage()).isEqualTo("Test message");
  }

  @Test
  void constructWithNullMessageAndCause() {
    CustomNestedException exception = new CustomNestedException(null, new RuntimeException());
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
  }

  @Test
  void constructWithOnlyCause() {
    RuntimeException cause = new RuntimeException("Cause");
    CustomNestedException exception = new CustomNestedException(cause);
    assertThat(exception.getCause()).isSameAs(cause);
  }

  @Test
  void constructWithNoArguments() {
    CustomNestedException exception = new CustomNestedException();
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.getCause()).isNull();
  }

  private static class CustomNestedException extends NestedCheckedException {
    CustomNestedException(String message) {
      super(message);
    }

    CustomNestedException(String message, Throwable cause) {
      super(message, cause);
    }

    CustomNestedException(Throwable cause) {
      super(cause);
    }

    CustomNestedException() {
      super();
    }
  }
}