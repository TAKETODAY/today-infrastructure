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

package infra.validation.method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/12 22:43
 */
class EmptyMethodValidationResultTests {

  @Test
  void test() {
    assertThatThrownBy(() -> new EmptyMethodValidationResult()
            .getMethod()).isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> new EmptyMethodValidationResult()
            .getTarget()).isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> new EmptyMethodValidationResult()
            .isForReturnValue()).isInstanceOf(UnsupportedOperationException.class);

    assertThat(new EmptyMethodValidationResult().getParameterValidationResults()).isEmpty();
    assertThat(new EmptyMethodValidationResult().toString()).isEqualTo("0 validation errors");
  }

}