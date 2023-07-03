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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties;

import cn.taketoday.validation.Errors;
import cn.taketoday.validation.ValidationUtils;
import cn.taketoday.validation.Validator;

/**
 * Used for testing validation of properties that have default field values.
 *
 * @author Madhura Bhave
 */
@EnableConfigurationProperties
@ConfigurationProperties
class ValidatorPropertiesWithDefaultValues implements Validator {

  private String bar = "a";

  @Override
  public boolean supports(Class<?> type) {
    return type == ValidatorPropertiesWithDefaultValues.class;
  }

  @Override
  public void validate(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "bar", "foo.empty");
  }

  public String getBar() {
    return this.bar;
  }

  public void setBar(String bar) {
    this.bar = bar;
  }

}
