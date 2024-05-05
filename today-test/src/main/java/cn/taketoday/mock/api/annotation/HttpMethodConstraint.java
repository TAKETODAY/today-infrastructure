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

/**
 * This annotation is used within the {@link MockSecurity} annotation to represent security constraints on specific
 * HTTP protocol messages.
 *
 * @since Servlet 3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpMethodConstraint {

  /**
   * Http protocol method name
   *
   * @return the name of an HTTP protocol method. <code>value</code> may not be null, or the empty string, and must be a
   * legitimate HTTP Method name as defined by RFC 7231.
   */
  String value();

  /**
   * The default authorization semantic. This value is insignificant when <code>rolesAllowed</code> returns a non-empty
   * array, and should not be specified when a non-empty array is specified for <tt>rolesAllowed</tt>.
   *
   * @return the {@link MockSecurity.EmptyRoleSemantic} to be applied when <code>rolesAllowed</code> returns an empty (that is,
   * zero-length) array.
   */
  MockSecurity.EmptyRoleSemantic emptyRoleSemantic() default MockSecurity.EmptyRoleSemantic.PERMIT;

  /**
   * The data protection requirements (i.e., whether or not SSL/TLS is required) that must be satisfied by the connections
   * on which requests arrive.
   *
   * @return the {@link MockSecurity.TransportGuarantee} indicating the data protection that must be provided by the connection.
   */
  MockSecurity.TransportGuarantee transportGuarantee() default MockSecurity.TransportGuarantee.NONE;

  /**
   * The names of the authorized roles.
   *
   * Duplicate role names appearing in rolesAllowed are insignificant and may be discarded during runtime processing of
   * the annotation. The String <tt>"*"</tt> has no special meaning as a role name (should it occur in rolesAllowed).
   *
   * @return an array of zero or more role names. When the array contains zero elements, its meaning depends on the value
   * returned by <code>emptyRoleSemantic</code>. If <code>emptyRoleSemantic</code> returns <tt>DENY</tt>, and
   * <code>rolesAllowed</code> returns a zero length array, access is to be denied independent of authentication state and
   * identity. Conversely, if <code>emptyRoleSemantic</code> returns <code>PERMIT</code>, it indicates that access is to
   * be allowed independent of authentication state and identity. When the array contains the names of one or more roles,
   * it indicates that access is contingent on membership in at least one of the named roles (independent of the value
   * returned by <code>emptyRoleSemantic</code>).
   */
  String[] rolesAllowed() default {};
}
