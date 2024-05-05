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

package cn.taketoday.mock.api;

import cn.taketoday.mock.api.annotation.HttpMethodConstraint;

/**
 * Java Class representation of an {@link HttpMethodConstraint} annotation value.
 *
 * @since Servlet 3.0
 */
public class HttpMethodConstraintElement extends HttpConstraintElement {

  private String methodName;

  /**
   * Constructs an instance with default {@link HttpConstraintElement} value.
   *
   * @param methodName the name of an HTTP protocol method. The name must not be null, or the empty string, and must be a
   * legitimate HTTP Method name as defined by RFC 7231
   */
  public HttpMethodConstraintElement(String methodName) {
    if (methodName == null || methodName.length() == 0) {
      throw new IllegalArgumentException("invalid HTTP method name");
    }
    this.methodName = methodName;
  }

  /**
   * Constructs an instance with specified {@link HttpConstraintElement} value.
   *
   * @param methodName the name of an HTTP protocol method. The name must not be null, or the empty string, and must be a
   * legitimate HTTP Method name as defined by RFC 7231
   * @param constraint the HTTPconstraintElement value to assign to the named HTTP method
   */
  public HttpMethodConstraintElement(String methodName, HttpConstraintElement constraint) {
    super(constraint.getEmptyRoleSemantic(), constraint.getTransportGuarantee(), constraint.getRolesAllowed());
    if (methodName == null || methodName.length() == 0) {
      throw new IllegalArgumentException("invalid HTTP method name");
    }
    this.methodName = methodName;
  }

  /**
   * Gets the HTTP method name.
   *
   * @return the Http method name
   */
  public String getMethodName() {
    return this.methodName;
  }
}
