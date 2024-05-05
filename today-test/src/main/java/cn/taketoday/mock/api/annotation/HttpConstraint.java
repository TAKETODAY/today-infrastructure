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

package cn.taketoday.mock.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.mock.api.annotation.MockSecurity.EmptyRoleSemantic;
import cn.taketoday.mock.api.annotation.MockSecurity.TransportGuarantee;

/**
 * This annotation is used within the {@link MockSecurity} annotation to represent the security constraints to be
 * applied to all HTTP protocol methods for which a corresponding {@link HttpMethodConstraint} element does NOT occur
 * within the {@link MockSecurity} annotation.
 *
 * <p>
 * For the special case where an <code>@HttpConstraint</code> that returns all default values occurs in combination with
 * at least one {@link HttpMethodConstraint} that returns other than all default values, the
 * <code>@HttpConstraint</code> represents that no security constraint is to be applied to any of the HTTP protocol
 * methods to which a security constraint would otherwise apply. This exception is made to ensure that such potentially
 * non-specific uses of <code>@HttpConstraint</code> do not yield constraints that will explicitly establish unprotected
 * access for such methods; given that they would not otherwise be covered by a constraint.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpConstraint {

  /**
   * The default authorization semantic. This value is insignificant when <code>rolesAllowed</code> returns a non-empty
   * array, and should not be specified when a non-empty array is specified for <tt>rolesAllowed</tt>.
   *
   * @return the {@link EmptyRoleSemantic} to be applied when <code>rolesAllowed</code> returns an empty (that is,
   * zero-length) array.
   */
  EmptyRoleSemantic value() default EmptyRoleSemantic.PERMIT;

  /**
   * The data protection requirements (i.e., whether or not SSL/TLS is required) that must be satisfied by the connections
   * on which requests arrive.
   *
   * @return the {@link TransportGuarantee} indicating the data protection that must be provided by the connection.
   */
  TransportGuarantee transportGuarantee() default TransportGuarantee.NONE;

  /**
   * The names of the authorized roles.
   *
   * Duplicate role names appearing in rolesAllowed are insignificant and may be discarded during runtime processing of
   * the annotation. The String <tt>"*"</tt> has no special meaning as a role name (should it occur in rolesAllowed).
   *
   * @return an array of zero or more role names. When the array contains zero elements, its meaning depends on the
   * <code>EmptyRoleSemantic</code> returned by the <code>value</code> method. If <code>value</code> returns
   * <tt>DENY</tt>, and <code>rolesAllowed</code> returns a zero length array, access is to be denied independent of
   * authentication state and identity. Conversely, if <code>value</code> returns <code>PERMIT</code>, it indicates that
   * access is to be allowed independent of authentication state and identity. When the array contains the names of one or
   * more roles, it indicates that access is contingent on membership in at least one of the named roles (independent of
   * the <code>EmptyRoleSemantic</code> returned by the <code>value</code> method).
   */
  String[] rolesAllowed() default {};
}
