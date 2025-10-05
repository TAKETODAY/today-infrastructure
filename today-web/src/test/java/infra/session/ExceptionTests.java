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

package infra.session;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 23:58
 */
class ExceptionTests {

  @Nested
  class WebSessionRequiredExceptionTests {

    @Test
    void constructor_withMessage_shouldSetMessage() {
      String message = "Session required";
      WebSessionRequiredException exception = new WebSessionRequiredException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void constructor_withMessageAndExpectedAttribute_shouldSetBoth() {
      String message = "Session required";
      String expectedAttribute = "user";
      WebSessionRequiredException exception = new WebSessionRequiredException(message, expectedAttribute);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedAttribute()).isEqualTo(expectedAttribute);
    }

    @Test
    void getExpectedAttribute_whenNotSet_shouldReturnNull() {
      WebSessionRequiredException exception = new WebSessionRequiredException("test");
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void getExpectedAttribute_whenSet_shouldReturnAttributeValue() {
      String expectedAttribute = "testAttribute";
      WebSessionRequiredException exception = new WebSessionRequiredException("test", expectedAttribute);
      assertThat(exception.getExpectedAttribute()).isEqualTo(expectedAttribute);
    }

    @Test
    void exception_shouldBeRuntimeException() {
      WebSessionRequiredException exception = new WebSessionRequiredException("test");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exception_shouldBeNestedRuntimeException() {
      WebSessionRequiredException exception = new WebSessionRequiredException("test");
      assertThat(exception).isInstanceOf(infra.core.NestedRuntimeException.class);
    }

    @Test
    void constructor_withNullMessage_shouldAccept() {
      WebSessionRequiredException exception = new WebSessionRequiredException(null);
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void constructor_withNullExpectedAttribute_shouldAccept() {
      WebSessionRequiredException exception = new WebSessionRequiredException("test", null);
      assertThat(exception.getMessage()).isEqualTo("test");
      assertThat(exception.getExpectedAttribute()).isNull();
    }
  }

}