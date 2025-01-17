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
