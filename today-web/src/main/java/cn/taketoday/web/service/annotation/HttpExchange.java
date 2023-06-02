/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.web.service.invoker.RequestPartArgumentResolver;

/**
 * Annotation to declare a method on an HTTP service interface as an HTTP
 * endpoint. The endpoint details are defined statically through attributes of
 * the annotation, as well as through the input method argument types.
 *
 * <p>Supported at the type level to express common attributes, to be inherited
 * by all methods, such as a base URL path.
 *
 * <p>At the method level, it's more common to use one of the following HTTP method
 * specific, shortcut annotations, each of which is itself <em>meta-annotated</em>
 * with {@code HttpExchange}:
 *
 * <ul>
 * <li>{@link GetExchange}
 * <li>{@link PostExchange}
 * <li>{@link PutExchange}
 * <li>{@link PatchExchange}
 * <li>{@link DeleteExchange}
 * </ul>
 *
 * <p>Supported method arguments:
 * <table border="1">
 * <tr>
 * <th>Method Argument</th>
 * <th>Description</th>
 * <th>Resolver</th>
 * </tr>
 * <tr>
 * <td>{@link java.net.URI URI}</td>
 * <td>Dynamically set the URL for the request, overriding the annotation's
 * {@link #url()} attribute</td>
 * <td>{@link cn.taketoday.web.service.invoker.UrlArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.http.HttpMethod HttpMethod}</td>
 * <td>Dynamically set the HTTP method for the request, overriding the annotation's
 * {@link #method()} attribute</td>
 * <td>{@link cn.taketoday.web.service.invoker.HttpMethodArgumentResolver
 * HttpMethodArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.RequestHeader @RequestHeader}</td>
 * <td>Add a request header</td>
 * <td>{@link cn.taketoday.web.service.invoker.RequestHeaderArgumentResolver
 * RequestHeaderArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.PathVariable @PathVariable}</td>
 * <td>Add a path variable for the URI template</td>
 * <td>{@link cn.taketoday.web.service.invoker.PathVariableArgumentResolver
 * PathVariableArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.RequestBody @RequestBody}</td>
 * <td>Set the body of the request</td>
 * <td>{@link cn.taketoday.web.service.invoker.RequestBodyArgumentResolver
 * RequestBodyArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.RequestParam @RequestParam}</td>
 * <td>Add a request parameter, either form data if {@code "Content-Type"} is
 * {@code "application/x-www-form-urlencoded"} or query params otherwise</td>
 * <td>{@link cn.taketoday.web.service.invoker.RequestParamArgumentResolver
 * RequestParamArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.RequestPart @RequestPart}</td>
 * <td>Add a request part, which may be a String (form field),
 * {@link cn.taketoday.core.io.Resource} (file part), Object (entity to be
 * encoded, e.g. as JSON), {@link HttpEntity} (part content and headers), a
 * {@link cn.taketoday.http.codec.multipart.Part}, or a
 * {@link org.reactivestreams.Publisher} of any of the above.
 * (</td>
 * <td>{@link RequestPartArgumentResolver
 * RequestPartArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link cn.taketoday.web.annotation.CookieValue @CookieValue}</td>
 * <td>Add a cookie</td>
 * <td>{@link cn.taketoday.web.service.invoker.CookieValueArgumentResolver
 * CookieValueArgumentResolver}</td>
 * </tr>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpExchange {

  /**
   * This is an alias for {@link #url}.
   */
  @AliasFor("url")
  String value() default "";

  /**
   * The URL for the request, either a full URL or a path only that is relative
   * to a URL declared in a type-level {@code @HttpExchange}, and/or a globally
   * configured base URL.
   * <p>By default, this is empty.
   */
  @AliasFor("value")
  String url() default "";

  /**
   * The HTTP method to use.
   * <p>Supported at the type level as well as at the method level.
   * When used at the type level, all method-level mappings inherit this value.
   * <p>By default, this is empty.
   */
  String method() default "";

  /**
   * The media type for the {@code "Content-Type"} header.
   * <p>Supported at the type level as well as at the method level, in which
   * case the method-level values override type-level values.
   * <p>By default, this is empty.
   */
  String contentType() default "";

  /**
   * The media types for the {@code "Accept"} header.
   * <p>Supported at the type level as well as at the method level, in which
   * case the method-level values override type-level values.
   * <p>By default, this is empty.
   */
  String[] accept() default {};

}
