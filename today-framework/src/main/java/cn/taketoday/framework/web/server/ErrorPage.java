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

package cn.taketoday.framework.web.server;

import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.util.ObjectUtils;

/**
 * Simple server-independent abstraction for error pages. Roughly equivalent to the
 * {@literal &lt;error-page&gt;} element traditionally found in web.xml.
 *
 * @author Dave Syer
 * @since 4.0
 */
public class ErrorPage {

  private final HttpStatusCode status;

  private final Class<? extends Throwable> exception;

  private final String path;

  public ErrorPage(String path) {
    this.status = null;
    this.exception = null;
    this.path = path;
  }

  public ErrorPage(HttpStatusCode status, String path) {
    this.status = status;
    this.exception = null;
    this.path = path;
  }

  public ErrorPage(Class<? extends Throwable> exception, String path) {
    this.status = null;
    this.exception = exception;
    this.path = path;
  }

  /**
   * The path to render (usually implemented as a forward), starting with "/". A custom
   * controller or servlet path can be used, or if the server supports it, a template
   * path (e.g. "/error.jsp").
   *
   * @return the path that will be rendered for this error
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Returns the exception type (or {@code null} for a page that matches by status).
   *
   * @return the exception type or {@code null}
   */
  public Class<? extends Throwable> getException() {
    return this.exception;
  }

  /**
   * The HTTP status value that this error page matches (or {@code null} for a page that
   * matches by exception).
   *
   * @return the status or {@code null}
   */
  public HttpStatusCode getStatus() {
    return this.status;
  }

  /**
   * The HTTP status value that this error page matches.
   *
   * @return the status value (or 0 for a page that matches any status)
   */
  public int getStatusCode() {
    return (this.status != null) ? this.status.value() : 0;
  }

  /**
   * The exception type name.
   *
   * @return the exception type name (or {@code null} if there is none)
   */
  public String getExceptionName() {
    return (this.exception != null) ? this.exception.getName() : null;
  }

  /**
   * Return if this error page is a global one (matches all unmatched status and
   * exception types).
   *
   * @return if this is a global error page
   */
  public boolean isGlobal() {
    return (this.status == null && this.exception == null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof ErrorPage other) {
      return ObjectUtils.nullSafeEquals(getExceptionName(), other.getExceptionName())
              && ObjectUtils.nullSafeEquals(this.path, other.path) && this.status == other.status;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ObjectUtils.nullSafeHashCode(getExceptionName());
    result = prime * result + ObjectUtils.nullSafeHashCode(this.path);
    result = prime * result + getStatusCode();
    return result;
  }

}
