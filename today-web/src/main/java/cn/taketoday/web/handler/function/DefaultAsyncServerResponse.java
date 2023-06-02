/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
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

  public static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
          "org.reactivestreams.Publisher", DefaultAsyncServerResponse.class.getClassLoader());

  private final CompletableFuture<ServerResponse> futureResponse;

  @Nullable
  private final Duration timeout;

  private DefaultAsyncServerResponse(CompletableFuture<ServerResponse> futureResponse, @Nullable Duration timeout) {
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

  @SuppressWarnings({ "unchecked" })
  public static AsyncServerResponse create(Object o, @Nullable Duration timeout) {
    Assert.notNull(o, "Argument to async is required");
    if (o instanceof CompletableFuture) {
      return new DefaultAsyncServerResponse((CompletableFuture<ServerResponse>) o, timeout);
    }
    else if (reactiveStreamsPresent) {
      ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
      ReactiveAdapter publisherAdapter = registry.getAdapter(o.getClass());
      if (publisherAdapter != null) {
        Publisher<ServerResponse> publisher = publisherAdapter.toPublisher(o);
        ReactiveAdapter futureAdapter = registry.getAdapter(CompletableFuture.class);
        if (futureAdapter != null) {
          CompletableFuture<ServerResponse> futureResponse =
                  (CompletableFuture<ServerResponse>) futureAdapter.fromPublisher(publisher);
          return new DefaultAsyncServerResponse(futureResponse, timeout);
        }
      }
    }
    throw new IllegalArgumentException("Asynchronous type not supported: " + o.getClass());
  }

}
