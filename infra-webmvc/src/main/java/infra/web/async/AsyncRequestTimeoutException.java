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

package infra.web.async;

import infra.http.HttpStatus;
import infra.http.ProblemDetail;
import infra.web.ErrorResponse;

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
public class AsyncRequestTimeoutException extends RuntimeException implements ErrorResponse {

  private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.SERVICE_UNAVAILABLE;
  }

  @Override
  public ProblemDetail getBody() {
    return body;
  }

}
