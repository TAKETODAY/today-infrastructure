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

package cn.taketoday.web.config;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.http.CorsConfiguration;

/**
 * Assists with the creation of a {@link CorsConfiguration} instance for a given
 * URL path pattern.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CorsConfiguration
 * @see CorsRegistry
 * @since 4.0 2022/2/15 17:27
 */
public class CorsRegistration {

  private final String pathPattern;

  private CorsConfiguration config;

  public CorsRegistration(String pathPattern) {
    this.pathPattern = pathPattern;
    // Same implicit default values as the @CrossOrigin annotation + allows simple methods
    this.config = new CorsConfiguration().applyPermitDefaultValues();
  }

  /**
   * Set the origins for which cross-origin requests are allowed from a browser.
   * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
   * format details and other considerations.
   *
   * <p>By default, all origins are allowed, but if
   * {@link #allowedOriginPatterns(String...) allowedOriginPatterns} is also
   * set, then that takes precedence.
   *
   * @see #allowedOriginPatterns(String...)
   */
  public CorsRegistration allowedOrigins(String... origins) {
    this.config.setAllowedOrigins(Arrays.asList(origins));
    return this;
  }

  /**
   * Alternative to {@link #allowedOrigins(String...)} that supports more
   * flexible patterns for specifying the origins for which cross-origin
   * requests are allowed from a browser. Please, refer to
   * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
   * details and other considerations.
   * <p>By default this is not set.
   */
  public CorsRegistration allowedOriginPatterns(String... patterns) {
    this.config.setAllowedOriginPatterns(Arrays.asList(patterns));
    return this;
  }

  /**
   * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"}, etc.
   * <p>The special value {@code "*"} allows all methods.
   * <p>By default "simple" methods {@code GET}, {@code HEAD}, and {@code POST}
   * are allowed.
   */
  public CorsRegistration allowedMethods(String... methods) {
    this.config.setAllowedMethods(Arrays.asList(methods));
    return this;
  }

  /**
   * Set the list of headers that a pre-flight request can list as allowed
   * for use during an actual request.
   * <p>The special value {@code "*"} may be used to allow all headers.
   * <p>A header name is not required to be listed if it is one of:
   * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
   * {@code Last-Modified}, or {@code Pragma} as per the CORS spec.
   * <p>By default all headers are allowed.
   */
  public CorsRegistration allowedHeaders(String... headers) {
    this.config.setAllowedHeaders(Arrays.asList(headers));
    return this;
  }

  /**
   * Set the list of response headers other than "simple" headers, i.e.
   * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
   * {@code Expires}, {@code Last-Modified}, or {@code Pragma}, that an
   * actual response might have and can be exposed.
   * <p>The special value {@code "*"} allows all headers to be exposed for
   * non-credentialed requests.
   * <p>By default this is not set.
   */
  public CorsRegistration exposedHeaders(String... headers) {
    this.config.setExposedHeaders(Arrays.asList(headers));
    return this;
  }

  /**
   * Whether the browser should send credentials, such as cookies along with
   * cross domain requests, to the annotated endpoint. The configured value is
   * set on the {@code Access-Control-Allow-Credentials} response header of
   * preflight requests.
   * <p><strong>NOTE:</strong> Be aware that this option establishes a high
   * level of trust with the configured domains and also increases the surface
   * attack of the web application by exposing sensitive user-specific
   * information such as cookies and CSRF tokens.
   * <p>By default this is not set in which case the
   * {@code Access-Control-Allow-Credentials} header is also not set and
   * credentials are therefore not allowed.
   */
  public CorsRegistration allowCredentials(boolean allowCredentials) {
    this.config.setAllowCredentials(allowCredentials);
    return this;
  }

  /**
   * Configure how long in seconds the response from a pre-flight request
   * can be cached by clients.
   * <p>By default this is set to 1800 seconds (30 minutes).
   */
  public CorsRegistration maxAge(long maxAge) {
    this.config.setMaxAge(maxAge);
    return this;
  }

  /**
   * Apply the given {@code CorsConfiguration} to the one being configured via
   * {@link CorsConfiguration#combine(CorsConfiguration)} which in turn has been
   * initialized with {@link CorsConfiguration#applyPermitDefaultValues()}.
   *
   * @param other the configuration to apply
   */
  public CorsRegistration combine(CorsConfiguration other) {
    this.config = this.config.combine(other);
    return this;
  }

  protected String getPathPattern() {
    return this.pathPattern;
  }

  protected CorsConfiguration getCorsConfiguration() {
    return this.config;
  }

}
