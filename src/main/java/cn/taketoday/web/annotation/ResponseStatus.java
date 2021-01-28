/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.web.Constant;
import cn.taketoday.web.http.HttpStatus;

/**
 * Marks a method or exception class with the status {@link #value} and
 * {@link #reason} that should be returned.
 *
 * <p>The status code is applied to the HTTP response when the handler
 * method is invoked and overrides status information set by other means,
 * like {@code ResponseEntity} or {@code "redirect:"}.
 *
 * <p><strong>Warning</strong>: when using this annotation on an exception
 * class, or when setting the {@code reason} attribute of this annotation,
 * the {@code RequestContext.sendError} method will be used.
 *
 * <p>With {@code RequestContext.sendError}, the response is considered
 * complete and should not be written to any further. Furthermore, the Servlet
 * container will typically write an HTML error page therefore making the
 * use of a {@code reason} unsuitable for REST APIs. For such cases it is
 * preferable to use a {@link cn.taketoday.web.http.ResponseEntity} as
 * a return type and avoid the use of {@code @ResponseStatus} altogether.
 *
 * <p>Note that a controller class may also be annotated with
 * {@code @ResponseStatus} and is then inherited by all {@code @RequestMapping}
 * methods.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author TODAY <br>
 * 2018-12-08 15:10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ResponseStatus {

  /**
   * The status <em>code</em> to use for the response.
   * <p>Default is {@link HttpStatus#INTERNAL_SERVER_ERROR}, which should
   * typically be changed to something more appropriate.
   *
   * @see javax.servlet.http.HttpServletResponse#setStatus(int)
   * @see javax.servlet.http.HttpServletResponse#sendError(int)
   * @since 3.0
   */
  HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

  /**
   * The <em>reason</em> to be used for the response.
   *
   * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
   * @since 3.0
   */
  String reason() default Constant.BLANK;

}
