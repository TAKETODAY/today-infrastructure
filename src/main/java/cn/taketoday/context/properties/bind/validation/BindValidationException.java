/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind.validation;

import cn.taketoday.util.Assert;

/**
 * Error thrown when validation fails during a bind operation.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see ValidationErrors
 * @see ValidationBindHandler
 * @since 4.0
 */
public class BindValidationException extends RuntimeException {

  private final ValidationErrors validationErrors;

  BindValidationException(ValidationErrors validationErrors) {
    super(getMessage(validationErrors));
    Assert.notNull(validationErrors, "ValidationErrors must not be null");
    this.validationErrors = validationErrors;
  }

  /**
   * Return the validation errors that caused the exception.
   *
   * @return the validationErrors the validation errors
   */
  public ValidationErrors getValidationErrors() {
    return this.validationErrors;
  }

  private static String getMessage(ValidationErrors errors) {
    StringBuilder message = new StringBuilder("Binding validation errors");
    if (errors != null) {
      message.append(" on ").append(errors.getName());
      errors.getAllErrors().forEach((error) -> message.append(String.format("%n   - %s", error)));
    }
    return message.toString();
  }

}
