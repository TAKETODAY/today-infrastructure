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

/**
 * Raised when the part of a "multipart/form-data" request identified by its
 * name cannot be found.
 *
 * <p>This may be because the request is not a multipart/form-data request,
 * because the part is not present in the request.
 *
 * @author TODAY 2021/1/17 10:30
 * @since 3.0
 */
public class MissingMultipartFileException extends MissingRequestValueException {

  private final String multipartName;

  private final MethodParameter parameter;

  /**
   * Constructor for MissingMatrixVariableException.
   *
   * @param variableName the name of the missing matrix variable
   * @param parameter the method parameter
   */
  public MissingMultipartFileException(String variableName, MethodParameter parameter) {
    this(variableName, parameter, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param variableName the name of the missing matrix variable
   * @param parameter the method parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingMultipartFileException(
          String variableName, MethodParameter parameter, boolean missingAfterConversion) {

    super("", missingAfterConversion);
    this.multipartName = variableName;
    this.parameter = parameter;
  }

  @Override
  public String getMessage() {
    return "Required multipart name '" + this.multipartName + "' for method parameter type " +
            this.parameter.getNestedParameterType().getSimpleName() + " is " +
            (isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the method parameter bound to the matrix variable.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
  }

  /**
   * Return the expected name of the multipart.
   */
  public final String getRequiredMultipartName() {
    return multipartName;
  }

}
