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