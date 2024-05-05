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

import cn.taketoday.mock.api.annotation.HttpConstraint;
import cn.taketoday.mock.api.annotation.MockSecurity.EmptyRoleSemantic;
import cn.taketoday.mock.api.annotation.MockSecurity.TransportGuarantee;

/**
 * Java Class representation of an {@link HttpConstraint} annotation value.
 *
 * @since Servlet 3.0
 */
public class HttpConstraintElement {

  private EmptyRoleSemantic emptyRoleSemantic;
  private TransportGuarantee transportGuarantee;
  private String[] rolesAllowed;

  /**
   * Constructs a default HTTP constraint element
   */
  public HttpConstraintElement() {
    this(EmptyRoleSemantic.PERMIT);
  }

  /**
   * Convenience constructor to establish <tt>EmptyRoleSemantic.DENY</tt>
   *
   * @param semantic should be EmptyRoleSemantic.DENY
   */
  public HttpConstraintElement(EmptyRoleSemantic semantic) {
    this(semantic, TransportGuarantee.NONE, new String[0]);
  }

  /**
   * Constructor to establish non-empty getRolesAllowed and/or <tt>TransportGuarantee.CONFIDENTIAL</tt>.
   *
   * @param guarantee <tt>TransportGuarantee.NONE</tt> or <tt>TransportGuarantee.CONFIDENTIAL</tt>
   * @param roleNames the names of the roles that are to be allowed access
   */
  public HttpConstraintElement(TransportGuarantee guarantee, String... roleNames) {
    this(EmptyRoleSemantic.PERMIT, guarantee, roleNames);
  }

  /**
   * Constructor to establish all of getEmptyRoleSemantic, getRolesAllowed, and getTransportGuarantee.
   *
   * @param semantic <tt>EmptyRoleSemantic.DENY</tt> or <tt>EmptyRoleSemantic.PERMIT</tt>
   * @param guarantee <tt>TransportGuarantee.NONE</tt> or <tt>TransportGuarantee.CONFIDENTIAL</tt>
   * @param roleNames the names of the roles that are to be allowed access, or missing if the semantic is
   * <tt>EmptyRoleSemantic.DENY</tt>
   */
  public HttpConstraintElement(EmptyRoleSemantic semantic, TransportGuarantee guarantee, String... roleNames) {
    if (semantic == EmptyRoleSemantic.DENY && roleNames.length > 0) {
      throw new IllegalArgumentException("Deny semantic with rolesAllowed");
    }
    this.emptyRoleSemantic = semantic;
    this.transportGuarantee = guarantee;
    this.rolesAllowed = copyStrings(roleNames);
  }

  /**
   * Gets the default authorization semantic.
   *
   * <p>
   * This value is insignificant when <code>getRolesAllowed</code> returns a non-empty array, and should not be specified
   * when a non-empty array is specified for <tt>getRolesAllowed</tt>.
   *
   * @return the {@link EmptyRoleSemantic} to be applied when <code>getRolesAllowed</code> returns an empty (that is,
   * zero-length) array
   */
  public EmptyRoleSemantic getEmptyRoleSemantic() {
    return this.emptyRoleSemantic;
  }

  /**
   * Gets the data protection requirement (i.e., whether or not SSL/TLS is required) that must be satisfied by the
   * transport connection.
   *
   * @return the {@link TransportGuarantee} indicating the data protection that must be provided by the connection
   */
  public TransportGuarantee getTransportGuarantee() {
    return this.transportGuarantee;
  }

  /**
   * Gets the names of the authorized roles.
   *
   * <p>
   * Duplicate role names appearing in getRolesAllowed are insignificant and may be discarded. The String <tt>"*"</tt> has
   * no special meaning as a role name (should it occur in getRolesAllowed).
   *
   * @return a (possibly empty) array of role names. When the array is empty, its meaning depends on the value of
   * {@link #getEmptyRoleSemantic}. If its value is <tt>DENY</tt>, and <code>getRolesAllowed</code> returns an empty
   * array, access is to be denied independent of authentication state and identity. Conversely, if its value is
   * <code>PERMIT</code>, it indicates that access is to be allowed independent of authentication state and identity. When
   * the array contains the names of one or more roles, it indicates that access is contingent on membership in at least
   * one of the named roles (independent of the value of {@link #getEmptyRoleSemantic}).
   */
  public String[] getRolesAllowed() {
    return copyStrings(this.rolesAllowed);
  }

  private String[] copyStrings(String[] strings) {
    String[] arr = null;
    if (strings != null) {
      int len = strings.length;
      arr = new String[len];
      if (len > 0) {
        System.arraycopy(strings, 0, arr, 0, len);
      }
    }

    return ((arr != null) ? arr : new String[0]);
  }
}
