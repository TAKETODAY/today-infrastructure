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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import infra.mock.api.annotation.HttpMethodConstraint;
import infra.mock.api.annotation.MockSecurity;

/**
 * Java Class representation of a {@link MockSecurity} annotation value.
 */
public class MockSecurityElement extends HttpConstraintElement {

  private Collection<String> methodNames;
  private Collection<HttpMethodConstraintElement> methodConstraints;

  /**
   * Constructs an instance using the default <code>HttpConstraintElement</code> value as the default Constraint element
   * and with no HTTP Method specific constraint elements.
   */
  public MockSecurityElement() {
    methodConstraints = new HashSet<>();
    methodNames = Collections.emptySet();
  }

  /**
   * Constructs an instance with a default Constraint element and with no HTTP Method specific constraint elements.
   *
   * @param constraint the HttpConstraintElement to be applied to all HTTP methods other than those represented in the
   * <tt>methodConstraints</tt>
   */
  public MockSecurityElement(HttpConstraintElement constraint) {
    super(constraint.getEmptyRoleSemantic(), constraint.getTransportGuarantee(), constraint.getRolesAllowed());
    methodConstraints = new HashSet<>();
    methodNames = Collections.emptySet();
  }

  /**
   * Constructs an instance using the default <code>HttpConstraintElement</code> value as the default Constraint element
   * and with a collection of HTTP Method specific constraint elements.
   *
   * @param methodConstraints the collection of HTTP method specific constraint elements
   * @throws IllegalArgumentException if duplicate method names are detected
   */
  public MockSecurityElement(Collection<HttpMethodConstraintElement> methodConstraints) {
    this.methodConstraints = (methodConstraints == null ? new HashSet<>() : methodConstraints);
    methodNames = checkMethodNames(this.methodConstraints);
  }

  /**
   * Constructs an instance with a default Constraint element and with a collection of HTTP Method specific constraint
   * elements.
   *
   * @param constraint the HttpConstraintElement to be applied to all HTTP methods other than those represented in the
   * <tt>methodConstraints</tt>
   * @param methodConstraints the collection of HTTP method specific constraint elements.
   * @throws IllegalArgumentException if duplicate method names are detected
   */
  public MockSecurityElement(HttpConstraintElement constraint,
          Collection<HttpMethodConstraintElement> methodConstraints) {
    super(constraint.getEmptyRoleSemantic(), constraint.getTransportGuarantee(), constraint.getRolesAllowed());
    this.methodConstraints = (methodConstraints == null ? new HashSet<>() : methodConstraints);
    methodNames = checkMethodNames(this.methodConstraints);
  }

  /**
   * Constructs an instance from a {@link MockSecurity} annotation value.
   *
   * @param annotation the annotation value
   * @throws IllegalArgumentException if duplicate method names are detected
   */
  public MockSecurityElement(MockSecurity annotation) {
    super(annotation.value().value(), annotation.value().transportGuarantee(), annotation.value().rolesAllowed());
    this.methodConstraints = new HashSet<>();
    for (HttpMethodConstraint constraint : annotation.httpMethodConstraints()) {
      this.methodConstraints.add(new HttpMethodConstraintElement(constraint.value(), new HttpConstraintElement(
              constraint.emptyRoleSemantic(), constraint.transportGuarantee(), constraint.rolesAllowed())));
    }
    methodNames = checkMethodNames(this.methodConstraints);
  }

  /**
   * Gets the (possibly empty) collection of HTTP Method specific constraint elements.
   *
   * <p>
   * If permitted, any changes to the returned <code>Collection</code> must not affect this
   * <code>ServletSecurityElement</code>.
   *
   * @return the (possibly empty) collection of HttpMethodConstraintElement objects
   */
  public Collection<HttpMethodConstraintElement> getHttpMethodConstraints() {
    return Collections.unmodifiableCollection(methodConstraints);
  }

  /**
   * Gets the set of HTTP method names named by the HttpMethodConstraints.
   *
   * <p>
   * If permitted, any changes to the returned <code>Collection</code> must not affect this
   * <code>ServletSecurityElement</code>.
   *
   * @return the collection String method names
   */
  public Collection<String> getMethodNames() {
    return Collections.unmodifiableCollection(methodNames);
  }

  /**
   * Checks for duplicate method names in methodConstraints.
   *
   * @param methodConstraints
   * @return Set of method names
   * @throws IllegalArgumentException if duplicate method names are detected
   */
  private Collection<String> checkMethodNames(Collection<HttpMethodConstraintElement> methodConstraints) {
    Collection<String> methodNames = new HashSet<>();
    for (HttpMethodConstraintElement methodConstraint : methodConstraints) {
      String methodName = methodConstraint.getMethodName();
      if (!methodNames.add(methodName)) {
        throw new IllegalArgumentException("Duplicate HTTP method name: " + methodName);
      }
    }
    return methodNames;
  }
}
