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

package cn.taketoday.web.multipart;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.bind.MultipartException;

/**
 * MultipartException subclass thrown when an upload exceeds the
 * maximum upload size allowed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:49
 */
public class MaxUploadSizeExceededException extends MultipartException implements ErrorResponse {

  private final ProblemDetail body =
          ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum upload size exceeded");

  private final long maxUploadSize;

  /**
   * Constructor for MaxUploadSizeExceededException.
   *
   * @param maxUploadSize the maximum upload size allowed,
   * or -1 if the size limit isn't known
   */
  public MaxUploadSizeExceededException(long maxUploadSize) {
    this(maxUploadSize, null);
  }

  /**
   * Constructor for MaxUploadSizeExceededException.
   *
   * @param maxUploadSize the maximum upload size allowed,
   * or -1 if the size limit isn't known
   * @param ex root cause from multipart parsing API in use
   */
  public MaxUploadSizeExceededException(long maxUploadSize, @Nullable Throwable ex) {
    super("Maximum upload size %sexceeded".formatted(maxUploadSize >= 0 ? "of %d bytes ".formatted(maxUploadSize) : ""), ex);
    this.maxUploadSize = maxUploadSize;
  }

  /**
   * Return the maximum upload size allowed,
   * or -1 if the size limit isn't known.
   */
  public long getMaxUploadSize() {
    return this.maxUploadSize;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.PAYLOAD_TOO_LARGE;
  }

  @Override
  public ProblemDetail getBody() {
    return body;
  }

}
