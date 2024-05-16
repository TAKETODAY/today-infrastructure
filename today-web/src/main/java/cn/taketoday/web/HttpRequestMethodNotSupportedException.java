/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/29 10:29
 */
public class HttpRequestMethodNotSupportedException extends NestedRuntimeException implements ErrorResponse {

  private final String method;

  @Nullable
  private final String[] supportedMethods;

  private final ProblemDetail body;

  /**
   * Create a new {@code HttpRequestMethodNotSupportedException}.
   *
   * @param method the unsupported HTTP request method
   */
  public HttpRequestMethodNotSupportedException(String method) {
    this(method, (String[]) null);
  }

  /**
   * Create a new {@code HttpRequestMethodNotSupportedException}.
   *
   * @param method the unsupported HTTP request method
   * @param supportedMethods the actually supported HTTP methods (possibly {@code null})
   */
  public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
    this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
  }

  /**
   * Create a new {@code HttpRequestMethodNotSupportedException}.
   *
   * @param method the unsupported HTTP request method
   * @param supportedMethods the actually supported HTTP methods (possibly {@code null})
   */
  private HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
    super("Request method '%s' is not supported".formatted(method));
    this.method = method;
    this.supportedMethods = supportedMethods;

    String detail = "Method '%s' is not supported.".formatted(method);
    this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), detail);
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
    var supportedMethods = new LinkedHashSet<HttpMethod>(this.supportedMethods.length);
    for (String value : this.supportedMethods) {
      HttpMethod method = HttpMethod.valueOf(value);
      supportedMethods.add(method);
    }
    return supportedMethods;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.METHOD_NOT_ALLOWED;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (ObjectUtils.isEmpty(this.supportedMethods)) {
      return HttpHeaders.empty();
    }
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setAllow(supportedMethods);
    return headers;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

  @Override
  public Object[] getDetailMessageArguments() {
    return new Object[] { getMethod(), getSupportedHttpMethods() };
  }

}
