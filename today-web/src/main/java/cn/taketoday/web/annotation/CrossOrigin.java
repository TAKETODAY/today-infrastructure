/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.cors.CorsConfiguration;

/**
 * Annotation for permitting cross-origin requests on specific handler classes
 * and/or handler methods. Processed if an appropriate {@code HandlerMapping} is
 * configured.
 *
 * <p>
 * The rules for combining global and local configuration are generally additive
 * -- e.g. all global and all local origins. For those attributes where only a
 * single value can be accepted such as {@code allowCredentials} and
 * {@code maxAge}, the local overrides the global value. See
 * {@link CorsConfiguration#combine(CorsConfiguration)} for more details.
 *
 * @author Russell Allen
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-08 18:14
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossOrigin {

  /**
   * The list of allowed origins that be specific origins, e.g.
   * {@code "https://domain1.com"}, or {@code "*"} for all origins.
   * <p>
   * A matched origin is listed in the {@code Access-Control-Allow-Origin}
   * response header of preflight actual CORS requests.
   * <p>
   * By default all origins are allowed.
   * <p>
   * <strong>Note:</strong> CORS checks use values from "Forwarded"
   * (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
   * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers, if
   * present, in order to reflect the client-originated address. Consider using
   * the {@code ForwardedHeaderFilter} in order to choose from a central place
   * whether to extract and use, or to discard such headers.
   *
   * @see #value
   */
  @AliasFor("origins")
  String[] value() default {};

  /**
   * A list of origins for which cross-origin requests are allowed. Please,
   * see {@link CorsConfiguration#setAllowedOrigins(List)} for details.
   * <p>By default all origins are allowed unless {@link #originPatterns} is
   * also set in which case {@code originPatterns} is used instead.
   */
  @AliasFor("value")
  String[] origins() default {};

  /**
   * Alternative to {@link #origins} that supports more flexible origin
   * patterns. Please, see {@link CorsConfiguration#setAllowedOriginPatterns(List)}
   * for details.
   * <p>By default this is not set.
   *
   * @since 4.0
   */
  String[] originPatterns() default {};

  /**
   * The list of request headers that are permitted in actual requests, possibly
   * {@code "*"} to allow all headers.
   * <p>
   * Allowed headers are listed in the {@code Access-Control-Allow-Headers}
   * response header of preflight requests.
   * <p>
   * A header name is not required to be listed if it is one of:
   * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
   * {@code Last-Modified}, or {@code Pragma} as per the CORS spec.
   * <p>
   * By default all requested headers are allowed.
   */
  String[] allowedHeaders() default {};

  /**
   * The List of response headers that the user-agent will allow the client to
   * access on an actual response, other than "simple" headers, i.e.
   * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
   * {@code Expires}, {@code Last-Modified}, or {@code Pragma},
   * <p>
   * Exposed headers are listed in the {@code Access-Control-Expose-Headers}
   * response header of actual CORS requests.
   * <p>
   * By default no headers are listed as exposed.
   */
  String[] exposedHeaders() default {};

  /**
   * The list of supported HTTP request methods.
   * <p>
   * By default the supported methods are the same as the ones to which a
   * controller method is mapped.
   */
  HttpMethod[] methods() default {};

  /**
   * Whether the browser should send credentials, such as cookies along with cross
   * domain requests, to the annotated endpoint. The configured value is set on
   * the {@code Access-Control-Allow-Credentials} response header of preflight
   * requests.
   * <p>
   * <strong>NOTE:</strong> Be aware that this option establishes a high level of
   * trust with the configured domains and also increases the surface attack of
   * the web application by exposing sensitive user-specific information such as
   * cookies and CSRF tokens.
   * <p>
   * By default this is not set in which case the
   * {@code Access-Control-Allow-Credentials} header is also not set and
   * credentials are therefore not allowed.
   */
  String allowCredentials() default Constant.BLANK;

  /**
   * The maximum age (in seconds) of the cache duration for preflight responses.
   * <p>
   * This property controls the value of the {@code Access-Control-Max-Age}
   * response header of preflight requests.
   * <p>
   * Setting this to a reasonable value can reduce the number of preflight
   * request/response interactions required by the browser. A negative value means
   * <em>undefined</em>.
   * <p>
   * By default this is set to {@code 1800} seconds (30 minutes).
   */
  long maxAge() default -1;

}
