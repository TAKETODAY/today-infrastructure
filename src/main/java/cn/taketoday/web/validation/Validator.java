/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2019-07-21 14:22
 */
public interface Validator {
  String KEY_VALIDATION_ERRORS = ParameterResolvingStrategy.class.getName() + "-context-validation-errors";

  /**
   * supports input object?
   *
   * @param obj input object
   * @since 3.0
   */
  boolean supports(Object obj);

  /**
   * Validates all constraints on {@code object}.
   *
   * @param object object to validate
   * @param errors a set of the constraint violations caused by this validation; will be
   * null if no error occurs
   */
  void validate(Object object, Errors errors);

}
