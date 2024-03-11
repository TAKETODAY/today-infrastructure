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

package cn.taketoday.web.handler.function;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.WebAsyncManager;

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
  public MultiValueMap<String, HttpCookie> cookies() {
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
    WebAsyncManager asyncManager = request.getAsyncManager();
    asyncManager.startDeferredResultProcessing(deferredResult);
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
