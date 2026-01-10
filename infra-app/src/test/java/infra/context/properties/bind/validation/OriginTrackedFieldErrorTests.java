/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.bind.validation;

import org.junit.jupiter.api.Test;

import infra.context.testfixture.origin.MockOrigin;
import infra.origin.Origin;
import infra.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OriginTrackedFieldError}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class OriginTrackedFieldErrorTests {

  private static final FieldError FIELD_ERROR = new FieldError("foo", "bar", "faf");

  private static final Origin ORIGIN = MockOrigin.of("afile");

  @Test
  void ofWhenFieldErrorIsNullShouldReturnNull() {
    assertThat(OriginTrackedFieldError.of(null, ORIGIN)).isNull();
  }

  @Test
  void ofWhenOriginIsNullShouldReturnFieldErrorWithoutOrigin() {
    assertThat(OriginTrackedFieldError.of(FIELD_ERROR, null)).isSameAs(FIELD_ERROR);
  }

  @Test
  void ofShouldReturnOriginCapableFieldError() {
    FieldError fieldError = OriginTrackedFieldError.of(FIELD_ERROR, ORIGIN);
    assertThat(fieldError.getObjectName()).isEqualTo("foo");
    assertThat(fieldError.getField()).isEqualTo("bar");
    assertThat(Origin.from(fieldError)).isEqualTo(ORIGIN);
  }

  @Test
  void toStringShouldAddOrigin() {
    assertThat(OriginTrackedFieldError.of(FIELD_ERROR, ORIGIN).toString())
            .isEqualTo("Field error in object 'foo' on field 'bar': rejected value [null]"
                    + "; codes []; arguments []; default message [faf]; origin afile");
  }

}
