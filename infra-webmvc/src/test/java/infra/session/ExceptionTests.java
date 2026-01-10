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
  class SessionRequiredExceptionTests {

    @Test
    void constructor_withMessage_shouldSetMessage() {
      String message = "Session required";
      SessionRequiredException exception = new SessionRequiredException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void constructor_withMessageAndExpectedAttribute_shouldSetBoth() {
      String message = "Session required";
      String expectedAttribute = "user";
      SessionRequiredException exception = new SessionRequiredException(message, expectedAttribute);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedAttribute()).isEqualTo(expectedAttribute);
    }

    @Test
    void getExpectedAttribute_whenNotSet_shouldReturnNull() {
      SessionRequiredException exception = new SessionRequiredException("test");
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void getExpectedAttribute_whenSet_shouldReturnAttributeValue() {
      String expectedAttribute = "testAttribute";
      SessionRequiredException exception = new SessionRequiredException("test", expectedAttribute);
      assertThat(exception.getExpectedAttribute()).isEqualTo(expectedAttribute);
    }

    @Test
    void exception_shouldBeRuntimeException() {
      SessionRequiredException exception = new SessionRequiredException("test");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exception_shouldBeNestedRuntimeException() {
      SessionRequiredException exception = new SessionRequiredException("test");
      assertThat(exception).isInstanceOf(infra.core.NestedRuntimeException.class);
    }

    @Test
    void constructor_withNullMessage_shouldAccept() {
      SessionRequiredException exception = new SessionRequiredException(null);
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getExpectedAttribute()).isNull();
    }

    @Test
    void constructor_withNullExpectedAttribute_shouldAccept() {
      SessionRequiredException exception = new SessionRequiredException("test", null);
      assertThat(exception.getMessage()).isEqualTo("test");
      assertThat(exception.getExpectedAttribute()).isNull();
    }
  }

}