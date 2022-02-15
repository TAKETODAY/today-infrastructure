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
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Constant;
import cn.taketoday.http.MediaType;

/**
 * @author TODAY 2020/12/8 21:47
 */
@Retention(RetentionPolicy.RUNTIME)
@ActionMapping(method = HttpMethod.DELETE)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface DeleteMapping {

  /** urls */
  @AliasFor(annotation = ActionMapping.class)
  String[] value() default Constant.BLANK;

  /**
   * Alias for {@link ActionMapping#path}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] path() default Constant.BLANK;

  /** Exclude url on class */
  @AliasFor(annotation = ActionMapping.class)
  boolean exclude() default false;

  /**
   * Narrows the primary mapping by media types that can be
   * consumed by the mapped handler. Consists of one or more
   * media types one of which must match to the request
   * Content-Type header.
   * Examples:
   * <pre>
   * consumes = "text/plain"
   * consumes = {"text/plain", "application/*"}
   * consumes = MediaType.TEXT_PLAIN_VALUE
   * </pre>
   * Expressions can be negated by using the "!" operator,
   * as in "!text/plain", which matches all requests with
   * a Content-Type other than "text/plain". Supported at
   * the type level as well as at the method level! If
   * specified at both levels, the method level consumes
   * condition overrides the type level condition.
   *
   * @see MediaType
   * @since 3.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] consumes() default {};

  /**
   * The parameters of the mapped request, narrowing the primary mapping.
   * <p>
   * Same format for any environment: a sequence of "myParam=myValue" style
   * expressions, with a request only mapped if each such parameter is found
   * to have the given value. Expressions can be negated by using the
   * "!=" operator, as in "myParam!=myValue". "myParam" style expressions
   * are also supported, with such parameters having to be present in the
   * request (allowed to have any value). Finally, "!myParam" style
   * expressions indicate that the specified parameter is not supposed
   * to be present in the request.
   * </p>
   * <p>
   * <b>
   * Supported at the type level as well as at the method level! When used
   * at the type level, all method-level mappings inherit this parameter restriction.
   * </b>
   * </p>
   *
   * @since 3.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] params() default {};

  /**
   * Narrows the primary mapping by media types that can be
   * produced by the mapped handler.
   * <p>
   * Consists of one or more media types one of which must be chosen via content
   * negotiation against the "acceptable" media types of the
   * request. Typically those are extracted from the "Accept"
   * header but may be derived from query parameters, or other.
   * </p>
   * <pre>
   * Examples:
   * produces = "text/plain"
   * produces = {"text/plain", "application/*"}
   * produces = MediaType.TEXT_PLAIN_VALUE
   * produces = "text/plain;charset=UTF-8"
   * </pre>
   * <p>
   * If a declared media type contains a parameter
   * (e.g. "charset=UTF-8", "type=feed", "type=entry") and
   * if a compatible media type from the request has that parameter too,
   * then the parameter values must match. Otherwise if the media type
   * from the request does not contain the parameter, it is assumed the
   * client accepts any value.
   * </p>
   * <p>
   * Expressions can be negated by using the "!" operator, as in "!text/plain",
   * which matches all requests with a Accept other than "text/plain".
   * </p>
   *
   * </p>
   * <b>
   * Supported at the type level as well as at the method level! If
   * specified at both levels, the method level produces condition
   * overrides the type level condition.
   * </b>
   * </p>
   *
   * @since 3.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] produces() default {};

}
