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
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used on a Servlet implementation class to specify security constraints to be enforced by a Servlet
 * container on HTTP protocol messages. The Servlet container will enforce these constraints on the url-patterns mapped
 * to the servlets mapped to the annotated class.
 *
 * @since Servlet 3.0
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletSecurity {

  /**
   * Defines the access semantic to be applied to an empty rolesAllowed array.
   */
  enum EmptyRoleSemantic {
    /**
     * access is to be permitted independent of authentication state and identity.
     */
    PERMIT,
    /**
     * access is to be denied independent of authentication state and identity.
     */
    DENY
  }

  /**
   * Defines the data protection requirements that must be satisfied by the transport
   */
  enum TransportGuarantee {
    /**
     * no protection of user data must be performed by the transport.
     */
    NONE,
    /**
     * All user data must be encrypted by the transport (typically using SSL/TLS).
     */
    CONFIDENTIAL
  }

  /**
   * Get the {@link HttpConstraint} that defines the protection that is to be applied to all HTTP methods that are NOT
   * represented in the array returned by <tt>httpMethodConstraints</tt>.
   *
   * @return a <code>HttpConstraint</code> object.
   */
  HttpConstraint value() default @HttpConstraint;

  /**
   * Get the HTTP method specific constraints. Each {@link HttpMethodConstraint} names an HTTP protocol method and defines
   * the protection to be applied to it.
   *
   * @return an array of {@link HttpMethodConstraint} elements each defining the protection to be applied to one HTTP
   * protocol method. For any HTTP method name, there must be at most one corresponding element in the returned array. If
   * the returned array is of zero length, it indicates that no HTTP method specific constraints are defined.
   */
  HttpMethodConstraint[] httpMethodConstraints() default {};
}
