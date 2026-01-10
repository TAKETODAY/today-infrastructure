/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api;

import infra.mock.api.annotation.HttpMethodConstraint;

/**
 * Java Class representation of an {@link HttpMethodConstraint} annotation value.
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
    if (methodName == null || methodName.isEmpty()) {
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
    if (methodName == null || methodName.isEmpty()) {
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
