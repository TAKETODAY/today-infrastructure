/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.validation;

import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidatorFactory;

/**
 * @author TODAY 2019-07-21 19:44
 */
public class DefaultJavaxValidator implements Validator {
  private javax.validation.Validator validator;

  public DefaultJavaxValidator(Configuration<?> configuration) {
    this(configuration.buildValidatorFactory());
  }

  public DefaultJavaxValidator(ValidatorFactory validatorFactory) {
    this(validatorFactory.getValidator());
  }

  public DefaultJavaxValidator(javax.validation.Validator validator) {
    this.validator = validator;
  }

  @Override
  public boolean supports(Object obj) {
    return true;
  }

  @Override
  public void validate(Object object, Errors errors) {
    final Set<ConstraintViolation<Object>> violations = validator.validate(object);
    if (!violations.isEmpty()) {
      fillErrors(errors, violations);
    }
  }

  protected void fillErrors(Errors errors, Set<ConstraintViolation<Object>> violations) {
    for (final ConstraintViolation<Object> violation : violations) {
      errors.addError(buildError(violation));
    }
  }

  /**
   * Build an error object
   *
   * @param violation
   *         ConstraintViolation
   *
   * @return A {@link ObjectError}
   */
  protected ObjectError buildError(final ConstraintViolation<Object> violation) {
    return new ObjectError(violation.getMessage(), getField(violation));
  }

  protected String getField(ConstraintViolation<Object> violation) {
    Path path = violation.getPropertyPath();
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Path.Node node : path) {
      if (node.isInIterable()) {
        sb.append('[');
        Object index = node.getIndex();
        if (index == null) {
          index = node.getKey();
        }
        if (index != null) {
          sb.append(index);
        }
        sb.append(']');
      }
      String name = node.getName();
      if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
        if (!first) {
          sb.append('.');
        }
        first = false;
        sb.append(name);
      }
    }
    return sb.toString();
  }

  public void setValidator(javax.validation.Validator validator) {
    this.validator = validator;
  }

  public javax.validation.Validator getValidator() {
    return validator;
  }

}
