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

package cn.taketoday.web.async;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.web.ErrorResponse;

/**
 * Exception to be thrown when an async request times out.
 * Alternatively an applications can register a
 * {@link DeferredResultProcessingInterceptor} or a
 * {@link CallableProcessingInterceptor} to handle the timeout through
 * the MVC Java config or the MVC XML namespace or directly through properties
 * of the {@code RequestMappingHandlerAdapter}.
 *
 * <p>By default the exception will be handled as a 503 error.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 18:10
 */
@SuppressWarnings("serial")
public class AsyncRequestTimeoutException extends RuntimeException implements ErrorResponse {

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.SERVICE_UNAVAILABLE;
  }

  @Override
  public ProblemDetail getBody() {
    return ProblemDetail.forStatus(getStatusCode());
  }

}
