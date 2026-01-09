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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.async.DeferredResult;

/**
 * Default {@link AsyncServerResponse} implementation.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultAsyncServerResponse extends ErrorHandlingServerResponse implements AsyncServerResponse {

  private final CompletableFuture<ServerResponse> futureResponse;

  @Nullable
  private final Duration timeout;

  DefaultAsyncServerResponse(CompletableFuture<ServerResponse> futureResponse, @Nullable Duration timeout) {
    this.futureResponse = futureResponse;
    this.timeout = timeout;
  }

  @Override
  public ServerResponse block() {
    try {
      if (timeout != null) {
        return futureResponse.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      }
      else {
        return futureResponse.get();
      }
    }
    catch (InterruptedException | ExecutionException | TimeoutException ex) {
      throw new IllegalStateException("Failed to get future response", ex);
    }
  }

  @Override
  public HttpStatusCode statusCode() {
    return delegate(ServerResponse::statusCode);
  }

  @Override
  public int rawStatusCode() {
    return delegate(ServerResponse::rawStatusCode);
  }

  @Override
  public HttpHeaders headers() {
    return delegate(ServerResponse::headers);
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    return delegate(ServerResponse::cookies);
  }

  private <R> R delegate(Function<ServerResponse, R> function) {
    ServerResponse response = futureResponse.getNow(null);
    if (response != null) {
      return function.apply(response);
    }
    else {
      throw new IllegalStateException("Future ServerResponse has not yet completed");
    }
  }

  @Override
  public Object writeTo(RequestContext request, Context context) throws Exception {
    writeAsync(request, createDeferredResult(request));
    return NONE_RETURN_VALUE;
  }

  static void writeAsync(RequestContext request, DeferredResult<?> deferredResult) throws Exception {
    request.asyncManager().startDeferredResultProcessing(deferredResult);
  }

  private DeferredResult<ServerResponse> createDeferredResult(RequestContext request) {
    DeferredResult<ServerResponse> result;
    if (timeout != null) {
      result = new DeferredResult<>(timeout.toMillis());
    }
    else {
      result = new DeferredResult<>();
    }
    futureResponse.whenComplete((value, ex) -> {
      if (ex != null) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
          ex = ex.getCause();
        }
        ServerResponse errorResponse = errorResponse(ex, request);
        if (errorResponse != null) {
          result.setResult(errorResponse);
        }
        else {
          result.setErrorResult(ex);
        }
      }
      else {
        result.setResult(value);
      }
    });
    return result;
  }

}
