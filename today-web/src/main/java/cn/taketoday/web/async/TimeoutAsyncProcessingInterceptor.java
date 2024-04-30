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

import java.util.concurrent.Callable;

import cn.taketoday.web.RequestContext;

/**
 * Sends a 503 (SERVICE_UNAVAILABLE) in case of a timeout if the response is not
 * already committed. this is done indirectly by setting the result
 * to an {@link AsyncRequestTimeoutException} which is then handled by
 * MVC's default exception handling as a 503 error.
 *
 * <p>Registered at the end, after all other interceptors and
 * therefore invoked only if no other interceptor handles the timeout.
 *
 * <p>Note that according to RFC 7231, a 503 without a 'Retry-After' header is
 * interpreted as a 500 error and the client should not retry. Applications
 * can install their own interceptor to handle a timeout and add a 'Retry-After'
 * header if necessary.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TimeoutAsyncProcessingInterceptor implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {

  @Override
  public <T> Object handleTimeout(RequestContext request, Callable<T> task) {
    return new AsyncRequestTimeoutException();
  }

  @Override
  public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> result) {
    result.setErrorResult(new AsyncRequestTimeoutException());
    return false;
  }

}
