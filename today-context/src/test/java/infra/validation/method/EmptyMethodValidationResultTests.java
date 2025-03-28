/*
 * Copyright 2017 - 2024 the original author or authors.
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