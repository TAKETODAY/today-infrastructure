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

package infra.context.properties;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.validation.Errors;
import infra.validation.ValidationUtils;
import infra.validation.Validator;

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
