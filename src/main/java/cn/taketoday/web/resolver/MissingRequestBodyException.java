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

package cn.taketoday.web.resolver;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.MissingRequestValueException;

/**
 * Missing RequestBody in current request
 *
 * @author TODAY 2021/3/10 12:51
 * @since 3.0
 */
public class MissingRequestBodyException extends MissingRequestValueException {

  private final String bodyName;

  private final MethodParameter parameter;

  /**
   * Constructor for MissingRequestCookieException.
   *
   * @param bodyName the name of the missing request body
   * @param parameter the method parameter
   */
  public MissingRequestBodyException(String bodyName, MethodParameter parameter) {
    this(bodyName, parameter, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param bodyName the name of the missing request body
   * @param parameter the method parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingRequestBodyException(
          String bodyName, MethodParameter parameter, boolean missingAfterConversion) {
    super("", missingAfterConversion);
    this.bodyName = bodyName;
    this.parameter = parameter;
  }

  @Override
  public String getMessage() {
    return "Required body '" + this.bodyName + "' for method parameter type " +
            this.parameter.getNestedParameterType().getSimpleName() + " is " +
            (isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the expected name of the request body.
   */
  public final String getBodyName() {
    return this.bodyName;
  }

  /**
   * Return the method parameter bound to the request cookie.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
  }

}
