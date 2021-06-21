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

import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.exception.WebNestedRuntimeException;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY <br>
 * 2019-07-21 14:35
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends WebNestedRuntimeException implements Errors {
  private static final long serialVersionUID = 1L;

  private final Errors errors;

  public ValidationException() {
    this.errors = new DefaultErrors();
  }

  public ValidationException(Errors errors) {
    this.errors = errors;
  }

  @Override
  public boolean hasErrors() {
    return !errors.hasErrors();
  }

  @Override
  public int getErrorCount() {
    return errors.getErrorCount();
  }

  @Override
  public void addError(ObjectError error) {
    this.errors.addError(error);
  }

  @Override
  public Set<ObjectError> getAllErrors() {
    return errors.getAllErrors();
  }

  @Override
  public String getMessage() {
    return errors.toString();
  }
}
