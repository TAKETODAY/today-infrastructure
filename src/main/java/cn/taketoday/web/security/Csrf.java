/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.security;

/**
 * Cross Site Request Forgery (CSRF) interface with access to the CSRF
 * header name and the CSRF token value.
 *
 * @author TODAY 2021/10/4 14:17
 * @see CsrfProtected
 * @since 4.0
 */
public interface Csrf {

  /**
   * Property that can be used to globally enable CSRF protection for an application.
   * Values of this property must be of type {@link jakarta.mvc.security.Csrf.CsrfOptions}.
   */
  String CSRF_PROTECTION = "cn.taketoday.web.security.CsrfProtected";

  /**
   * The default value for.
   */
  String DEFAULT_CSRF_HEADER_NAME = "X-CSRF-TOKEN";

  /**
   * Returns the name of the CSRF form field or HTTP request header. This name is typically a constant.
   *
   * @return name of CSRF header.
   */
  String getName();

  /**
   * Returns the value of the CSRF token.
   *
   * @return value of CSRF token.
   */
  String getToken();
}

