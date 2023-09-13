/*
 * Copyright 2017 - 2023 the original author or authors.
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
   * Alias for {@link #origins}.
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
   */
  String[] originPatterns() default {};

  /**
   * The list of request headers that are permitted in actual requests,
   * possibly {@code "*"} to allow all headers. Please, see
   * {@link CorsConfiguration#setAllowedHeaders(List)} for details.
   * <p>By default all requested headers are allowed.
   */
  String[] allowedHeaders() default {};

  /**
   * The List of response headers that the user-agent will allow the client
   * to access on an actual response, possibly {@code "*"} to expose all headers.
   * Please, see {@link CorsConfiguration#setExposedHeaders(List)} for details.
   * <p>By default no headers are listed as exposed.
   */
  String[] exposedHeaders() default {};

  /**
   * The list of supported HTTP request methods. Please, see
   * {@link CorsConfiguration#setAllowedMethods(List)} for details.
   * <p>By default the supported methods are the same as the ones to which a
   * controller method is mapped.
   */
  HttpMethod[] methods() default {};

  /**
   * Whether the browser should send credentials, such as cookies along with
   * cross domain requests, to the annotated endpoint. Please, see
   * {@link CorsConfiguration#setAllowCredentials(Boolean)} for details.
   * <p><strong>NOTE:</strong> Be aware that this option establishes a high
   * level of trust with the configured domains and also increases the surface
   * attack of the web application by exposing sensitive user-specific
   * information such as cookies and CSRF tokens.
   * <p>By default this is not set in which case the
   * {@code Access-Control-Allow-Credentials} header is also not set and
   * credentials are therefore not allowed.
   */
  String allowCredentials() default "";

  /**
   * The maximum age (in seconds) of the cache duration for preflight responses.
   * <p>This property controls the value of the {@code Access-Control-Max-Age}
   * response header of preflight requests.
   * <p>Setting this to a reasonable value can reduce the number of preflight
   * request/response interactions required by the browser.
   * A negative value means <em>undefined</em>.
   * <p>By default this is set to {@code 1800} seconds (30 minutes).
   */
  long maxAge() default -1;

}
