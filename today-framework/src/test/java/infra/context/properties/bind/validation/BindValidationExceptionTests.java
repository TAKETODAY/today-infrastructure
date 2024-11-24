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

package infra.context.properties.bind.validation;

import org.junit.jupiter.api.Test;

import infra.context.properties.bind.validation.BindValidationException;
import infra.context.properties.bind.validation.ValidationErrors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BindValidationException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class BindValidationExceptionTests {

  @Test
  void createWhenValidationErrorsIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new BindValidationException(null))
            .withMessageContaining("ValidationErrors is required");
  }

  @Test
  void getValidationErrorsShouldReturnValidationErrors() {
    ValidationErrors errors = mock(ValidationErrors.class);
    BindValidationException exception = new BindValidationException(errors);
    assertThat(exception.getValidationErrors()).isEqualTo(errors);
  }

}
