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

package cn.taketoday.web;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/29 10:29
 */
@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class HttpRequestMethodNotSupportedException extends WebNestedRuntimeException {

  private final String method;

  @Nullable
  private final String[] supportedMethods;

  /**
   * Create a new HttpRequestMethodNotSupportedException.
   *
   * @param method the unsupported HTTP request method
   */
  public HttpRequestMethodNotSupportedException(String method) {
    this(method, (String[]) null);
  }

  /**
   * Create a new HttpRequestMethodNotSupportedException.
   *
   * @param method the unsupported HTTP request method
   * @param msg the detail message
   */
  public HttpRequestMethodNotSupportedException(String method, String msg) {
    this(method, null, msg);
  }

  /**
   * Create a new HttpRequestMethodNotSupportedException.
   *
   * @param method the unsupported HTTP request method
   * @param supportedMethods the actually supported HTTP methods (may be {@code null})
   */
  public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
    this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
  }

  /**
   * Create a new HttpRequestMethodNotSupportedException.
   *
   * @param method the unsupported HTTP request method
   * @param supportedMethods the actually supported HTTP methods (may be {@code null})
   */
  public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
    this(method, supportedMethods, "Request method '" + method + "' not supported");
  }

  /**
   * Create a new HttpRequestMethodNotSupportedException.
   *
   * @param method the unsupported HTTP request method
   * @param supportedMethods the actually supported HTTP methods
   * @param msg the detail message
   */
  public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods, String msg) {
    super(msg);
    this.method = method;
    this.supportedMethods = supportedMethods;
  }

  /**
   * Return the HTTP request method that caused the failure.
   */
  public String getMethod() {
    return this.method;
  }

  /**
   * Return the actually supported HTTP methods, or {@code null} if not known.
   */
  @Nullable
  public String[] getSupportedMethods() {
    return this.supportedMethods;
  }

  /**
   * Return the actually supported HTTP methods as {@link HttpMethod} instances,
   * or {@code null} if not known.
   */
  @Nullable
  public Set<HttpMethod> getSupportedHttpMethods() {
    if (this.supportedMethods == null) {
      return null;
    }
    Set<HttpMethod> supportedMethods = new LinkedHashSet<>(this.supportedMethods.length);
    for (String value : this.supportedMethods) {
      HttpMethod method = HttpMethod.valueOf(value);
      supportedMethods.add(method);
    }
    return supportedMethods;
  }

}
