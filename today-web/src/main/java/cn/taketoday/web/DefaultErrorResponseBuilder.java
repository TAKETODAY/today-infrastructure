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

import java.net.URI;
import java.util.function.Consumer;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  @Nullable
  private Object[] detailMessageArguments;

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
  public ErrorResponse.Builder detailMessageArguments(Object... messageArguments) {
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

    @Nullable
    private final Object[] detailMessageArguments;

    SimpleErrorResponse(Throwable ex, HttpStatusCode statusCode, @Nullable HttpHeaders headers,
            ProblemDetail problemDetail, String typeMessageCode, String titleMessageCode, String detailMessageCode,
            @Nullable Object[] detailMessageArguments) {

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
    @Nullable
    public Object[] getDetailMessageArguments() {
      return this.detailMessageArguments;
    }

    @Override
    public String toString() {
      return "ErrorResponse{status=%s, headers=%s, body=%s, exception=%s}"
              .formatted(this.statusCode, this.headers, this.problemDetail, this.exception);
    }
  }

}
