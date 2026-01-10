/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.core.NestedRuntimeException;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

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

  private final String @Nullable [] supportedMethods;

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
  private HttpRequestMethodNotSupportedException(String method, String @Nullable [] supportedMethods) {
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
  public String @Nullable [] getSupportedMethods() {
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
