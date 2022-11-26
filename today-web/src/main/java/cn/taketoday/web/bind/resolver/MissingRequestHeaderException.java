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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.bind.MissingRequestValueException;
import cn.taketoday.web.bind.RequestBindingException;

/**
 * {@link RequestBindingException} subclass that indicates
 * that a request header expected in the method parameters of an
 * {@code @RequestMapping} method is not present.
 *
 * @author TODAY 2021/3/10 20:39
 */
public class MissingRequestHeaderException extends MissingRequestValueException {

  private final String headerName;

  private final MethodParameter parameter;

  /**
   * Constructor for MissingRequestHeaderException.
   *
   * @param headerName the name of the missing request header
   * @param parameter the method parameter
   */
  public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
    this(headerName, parameter, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param headerName the name of the missing request header
   * @param parameter the method parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingRequestHeaderException(
          String headerName, MethodParameter parameter, boolean missingAfterConversion) {
    super("", missingAfterConversion);
    this.headerName = headerName;
    this.parameter = parameter;
    setDetail("Required header '" + this.headerName + "' is not present.");
  }

  @Override
  public String getMessage() {
    String typeName = this.parameter.getNestedParameterType().getSimpleName();
    return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " +
            (isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the expected name of the request header.
   */
  public final String getHeaderName() {
    return this.headerName;
  }

  /**
   * Return the method parameter bound to the request header.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
  }

}
