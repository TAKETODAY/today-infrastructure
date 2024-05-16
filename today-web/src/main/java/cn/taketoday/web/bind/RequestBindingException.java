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

package cn.taketoday.web.bind;

import java.net.URI;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.ErrorResponse;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 *
 * <p>Extends RuntimeException for convenient throwing in any resource
 * (such as a Filter), and NestedServletException for proper root cause handling
 * (as the plain Exception doesn't expose its root cause at all).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 22:54
 */
public class RequestBindingException extends NestedRuntimeException implements ErrorResponse {

  private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   */
  public RequestBindingException(String msg) {
    super(msg);
  }

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public RequestBindingException(String msg, Throwable cause) {
    super(msg, cause);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
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

}

