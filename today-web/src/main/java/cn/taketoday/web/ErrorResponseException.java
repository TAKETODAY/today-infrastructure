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

import java.io.Serial;
import java.net.URI;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;

/**
 * {@link RuntimeException} that implements {@link ErrorResponse} to expose
 * an HTTP status, response headers, and a body formatted as an RFC 7807
 * {@link ProblemDetail}.
 *
 * <p>The exception can be used as is, or it can be extended as a more specific
 * exception that populates the {@link ProblemDetail#setType(URI) type} or
 * {@link ProblemDetail#setDetail(String) detail} fields, or potentially adds
 * other non-standard properties.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 13:35
 */
public class ErrorResponseException extends NestedRuntimeException implements ErrorResponse {
  @Serial
  private static final long serialVersionUID = 1L;

  private final HttpStatusCode status;

  private final HttpHeaders headers = HttpHeaders.create();

  private final ProblemDetail body;

  /**
   * Constructor with a {@link HttpStatusCode}.
   */
  public ErrorResponseException(HttpStatusCode status) {
    this(status, null);
  }

  /**
   * Constructor with a {@link HttpStatusCode} and an optional cause.
   */
  public ErrorResponseException(HttpStatusCode status, @Nullable Throwable cause) {
    this(status, ProblemDetail.forStatus(status), cause);
  }

  /**
   * Constructor with a given {@link ProblemDetail} instance, possibly a
   * subclass of {@code ProblemDetail} with extended fields.
   */
  public ErrorResponseException(HttpStatusCode status, ProblemDetail body, @Nullable Throwable cause) {
    super(null, cause);
    this.status = status;
    this.body = body;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return this.status;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  /**
   * Set the {@link ProblemDetail#setType(URI) type} field of the response body.
   *
   * @param type the problem type
   */
  public void setType(URI type) {
    this.body.setType(type);
  }

  /**
   * Set the {@link ProblemDetail#setTitle(String) title} field of the response body.
   *
   * @param title the problem title
   */
  public void setTitle(@Nullable String title) {
    this.body.setTitle(title);
  }

  /**
   * Set the {@link ProblemDetail#setDetail(String) detail} field of the response body.
   *
   * @param detail the problem detail
   */
  public void setDetail(@Nullable String detail) {
    this.body.setDetail(detail);
  }

  /**
   * Set the {@link ProblemDetail#setInstance(URI) instance} field of the response body.
   *
   * @param instance the problem instance
   */
  public void setInstance(@Nullable URI instance) {
    this.body.setInstance(instance);
  }

  /**
   * Return the body for the response. To customize the body content, use:
   * <ul>
   * <li>{@link #setType(URI)}
   * <li>{@link #setTitle(String)}
   * <li>{@link #setDetail(String)}
   * <li>{@link #setInstance(URI)}
   * </ul>
   * <p>By default, the status field of {@link ProblemDetail} is initialized
   * from the status provided to the constructor, which in turn may also
   * initialize the title field from the status reason phrase, if the status
   * is well-known. The instance field, if not set, is initialized from the
   * request path when a {@code ProblemDetail} is returned from an
   * {@code @ExceptionHandler} method.
   */
  @Override
  public final ProblemDetail getBody() {
    return this.body;
  }

  @Override
  public String getMessage() {
    return this.status + (!this.headers.isEmpty() ? ", headers=" + this.headers : "") + ", " + this.body;
  }

}
