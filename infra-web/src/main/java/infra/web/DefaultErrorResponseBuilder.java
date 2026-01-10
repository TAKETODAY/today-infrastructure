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

import java.net.URI;
import java.util.function.Consumer;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.lang.Assert;

/**
 * Default implementation of {@link ErrorResponse.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class DefaultErrorResponseBuilder implements ErrorResponse.Builder {

  private final Throwable exception;

  private final HttpStatusCode statusCode;

  @Nullable
  private HttpHeaders headers;

  private final ProblemDetail problemDetail;

  private String typeMessageCode;

  private String titleMessageCode;

  private String detailMessageCode;

  private Object @Nullable [] detailMessageArguments;

  DefaultErrorResponseBuilder(Throwable ex, ProblemDetail problemDetail) {
    Assert.notNull(ex, "Throwable is required");
    this.exception = ex;
    this.statusCode = HttpStatusCode.valueOf(problemDetail.getStatus());
    this.problemDetail = problemDetail;
    this.typeMessageCode = ErrorResponse.getDefaultTypeMessageCode(ex.getClass());
    this.titleMessageCode = ErrorResponse.getDefaultTitleMessageCode(ex.getClass());
    this.detailMessageCode = ErrorResponse.getDefaultDetailMessageCode(ex.getClass(), null);
  }

  @Override
  public ErrorResponse.Builder header(String headerName, String... headerValues) {
    httpHeaders().setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public ErrorResponse.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(httpHeaders());
    return this;
  }

  @Override
  public ErrorResponse.Builder headers(@Nullable HttpHeaders headers) {
    httpHeaders().setAll(headers);
    return this;
  }

  private HttpHeaders httpHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.forWritable();
    }
    return this.headers;
  }

  @Override
  public ErrorResponse.Builder type(URI type) {
    this.problemDetail.setType(type);
    return this;
  }

  @Override
  public ErrorResponse.Builder typeMessageCode(String messageCode) {
    this.typeMessageCode = messageCode;
    return this;
  }

  @Override
  public ErrorResponse.Builder title(@Nullable String title) {
    this.problemDetail.setTitle(title);
    return this;
  }

  @Override
  public ErrorResponse.Builder titleMessageCode(String messageCode) {
    Assert.notNull(messageCode, "`titleMessageCode` is required");
    this.titleMessageCode = messageCode;
    return this;
  }

  @Override
  public ErrorResponse.Builder instance(@Nullable URI instance) {
    this.problemDetail.setInstance(instance);
    return this;
  }

  @Override
  public ErrorResponse.Builder detail(String detail) {
    this.problemDetail.setDetail(detail);
    return this;
  }

  @Override
  public ErrorResponse.Builder detailMessageCode(String messageCode) {
    Assert.notNull(messageCode, "`detailMessageCode` is required");
    this.detailMessageCode = messageCode;
    return this;
  }

  @Override
  public ErrorResponse.Builder detailMessageArguments(Object @Nullable ... messageArguments) {
    this.detailMessageArguments = messageArguments;
    return this;
  }

  @Override
  public ErrorResponse.Builder property(String name, @Nullable Object value) {
    this.problemDetail.setProperty(name, value);
    return this;
  }

  @Override
  public ErrorResponse build() {
    return new SimpleErrorResponse(
            this.exception, this.statusCode, this.headers, this.problemDetail,
            this.typeMessageCode, this.titleMessageCode, this.detailMessageCode,
            this.detailMessageArguments);
  }

  /**
   * Simple container for {@code ErrorResponse} values.
   */
  private static class SimpleErrorResponse implements ErrorResponse {

    private final Throwable exception;

    private final HttpStatusCode statusCode;

    private final HttpHeaders headers;

    private final ProblemDetail problemDetail;

    private final String typeMessageCode;

    private final String titleMessageCode;

    private final String detailMessageCode;

    private final Object @Nullable [] detailMessageArguments;

    SimpleErrorResponse(Throwable ex, HttpStatusCode statusCode, @Nullable HttpHeaders headers,
            ProblemDetail problemDetail, String typeMessageCode, String titleMessageCode, String detailMessageCode,
            Object @Nullable [] detailMessageArguments) {

      this.exception = ex;
      this.statusCode = statusCode;
      this.headers = (headers != null ? headers : HttpHeaders.empty());
      this.problemDetail = problemDetail;
      this.typeMessageCode = typeMessageCode;
      this.titleMessageCode = titleMessageCode;
      this.detailMessageCode = detailMessageCode;
      this.detailMessageArguments = detailMessageArguments;
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return this.statusCode;
    }

    @Override
    public HttpHeaders getHeaders() {
      return this.headers;
    }

    @Override
    public ProblemDetail getBody() {
      return this.problemDetail;
    }

    @Override
    public String getTypeMessageCode() {
      return this.typeMessageCode;
    }

    @Override
    public String getTitleMessageCode() {
      return this.titleMessageCode;
    }

    @Override
    public String getDetailMessageCode() {
      return this.detailMessageCode;
    }

    @Override
    public Object @Nullable [] getDetailMessageArguments() {
      return this.detailMessageArguments;
    }

    @Override
    public String toString() {
      return "ErrorResponse{status=%s, headers=%s, body=%s, exception=%s}"
              .formatted(this.statusCode, this.headers, this.problemDetail, this.exception);
    }
  }

}
