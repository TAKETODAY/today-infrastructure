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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.web.handler.HandlerMethodMappingNamingStrategy;

/**
 * Annotation for mapping web requests onto methods in request-handling classes
 * with flexible method signatures.
 *
 * <p><strong>Note:</strong> This annotation can be used both at the class and
 * at the method level. In most cases, at the method level applications will
 * prefer to use one of the HTTP method specific variants
 * {@link GetMapping @GetMapping}, {@link PostMapping @PostMapping},
 * {@link PutMapping @PutMapping}, {@link DeleteMapping @DeleteMapping}, or
 * {@link PatchMapping @PatchMapping}.</p>
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @RequestMapping} and {@code @SessionAttributes} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * <p>
 * like Spring's RequestMapping
 * </p>
 *
 * @author TODAY <br>
 * 2018-08-23 10:18 change
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ActionMapping {

  /**
   * Assign a name to this mapping.
   * <p><b>Supported at the type level as well as at the method level!</b>
   * When used on both levels, a combined name is derived by concatenation
   * with "#" as separator.
   *
   * @see HandlerMethodMappingNamingStrategy
   * @since 4.0
   */
  String name() default "";

  /**
   * The primary mapping expressed by this annotation.
   * <p>This is an alias for {@link #path}. For example,
   * {@code @RequestMapping("/foo")} is equivalent to
   * {@code @RequestMapping(path="/foo")}.
   * <p><b>Supported at the type level as well as at the method level!</b>
   * When used at the type level, all method-level mappings inherit
   * this primary mapping, narrowing it for a specific handler method.
   * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
   * explicitly is effectively mapped to an empty path.
   */
  @AliasFor("path")
  String[] value() default {};

  /**
   * The path mapping URIs (e.g. {@code "/profile"}).
   * <p>Ant-style path patterns are also supported (e.g. {@code "/profile/**"}).
   * At the method level, relative paths (e.g. {@code "edit"}) are supported
   * within the primary mapping expressed at the type level.
   * Path mapping URIs may contain placeholders (e.g. <code>"/${profile_path}"</code>).
   * <p><b>Supported at the type level as well as at the method level!</b>
   * When used at the type level, all method-level mappings inherit
   * this primary mapping, narrowing it for a specific handler method.
   * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
   * explicitly is effectively mapped to an empty path.
   *
   * @since 4.0
   */
  @AliasFor("value")
  String[] path() default {};

  /**
   * Combine this condition with another such as conditions from a
   * type-level and method-level {@code @RequestMapping} annotation.
   *
   * @since 3.0
   */
  boolean combine() default true;

  /**
   * The HTTP request methods to map to, narrowing the primary mapping:
   * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
   * <p><b>Supported at the type level as well as at the method level!</b>
   * When used at the type level, all method-level mappings inherit this
   * HTTP method restriction.
   */
  HttpMethod[] method() default {};

  /**
   * Narrows the primary mapping by media types that can be consumed by the
   * mapped handler. Consists of one or more media types one of which must
   * match to the request {@code Content-Type} header. Examples:
   * <pre class="code">
   * consumes = "text/plain"
   * consumes = {"text/plain", "application/*"}
   * consumes = MediaType.TEXT_PLAIN_VALUE
   * </pre>
   * <p>If a declared media type contains a parameter, and if the
   * {@code "content-type"} from the request also has that parameter, then
   * the parameter values must match. Otherwise, if the media type from the
   * request {@code "content-type"} does not contain the parameter, then the
   * parameter is ignored for matching purposes.
   * <p>Expressions can be negated by using the "!" operator, as in
   * "!text/plain", which matches all requests with a {@code Content-Type}
   * other than "text/plain".
   * <p><b>Supported at the type level as well as at the method level!</b>
   * If specified at both levels, the method level consumes condition overrides
   * the type level condition.
   *
   * @see MediaType
   * @see jakarta.servlet.http.HttpServletRequest#getContentType()
   * @since 3.0
   */
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
  String[] produces() default {};

  /**
   * The headers of the mapped request, narrowing the primary mapping.
   * <p>Same format for any environment: a sequence of "My-Header=myValue" style
   * expressions, with a request only mapped if each such header is found
   * to have the given value. Expressions can be negated by using the "!=" operator,
   * as in "My-Header!=myValue". "My-Header" style expressions are also supported,
   * with such headers having to be present in the request (allowed to have
   * any value). Finally, "!My-Header" style expressions indicate that the
   * specified header is <i>not</i> supposed to be present in the request.
   * <p>Also supports media type wildcards (*), for headers such as Accept
   * and Content-Type. For instance,
   * <pre class="code">
   * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
   * </pre>
   * will match requests with a Content-Type of "text/html", "text/plain", etc.
   * <p><b>Supported at the type level as well as at the method level!</b>
   * When used at the type level, all method-level mappings inherit this
   * header restriction.
   *
   * @see MediaType
   * @since 4.0
   */
  String[] headers() default {};
}
