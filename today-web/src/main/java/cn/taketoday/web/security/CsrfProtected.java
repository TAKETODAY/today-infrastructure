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

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.http.MediaType;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>A controller annotation to validate a CSRF token value received
 * in a request whenever the property {@link Csrf#CSRF_PROTECTION} is
 * set to {@link CsrfOptions#EXPLICIT}. If the property {@link Csrf#CSRF_PROTECTION}
 * is set to {@link CsrfOptions#IMPLICIT}, then the use of this annotation
 * is redundant. TODAY MVC implementations are only REQUIRED to
 * enforce CSRF for POST controllers that consume payloads of type
 * {@link MediaType#APPLICATION_FORM_URLENCODED}, but other HTTP methods
 * and payloads may be optionally supported by the underlying
 * implementation. If declared at the type level, it applies to all
 * methods in the type.</p>
 *
 * @author TODAY 2021/10/4 14:16
 * @see Csrf
 * @since 4.0
 */
@Target({ METHOD, TYPE })
@Retention(RUNTIME)
@Documented
@Inherited
public @interface CsrfProtected {

}
