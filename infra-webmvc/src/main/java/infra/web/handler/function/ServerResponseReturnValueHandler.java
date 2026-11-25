/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.List;

import infra.http.converter.HttpMessageConverter;
import infra.util.Assert;
import infra.web.HttpContext;
import infra.web.ReturnValueHandler;
import infra.web.async.DeferredResult;
import infra.web.handler.method.support.RequestResponseBodyMethodProcessor;
import infra.web.handler.result.ResponseBodyEmitterReturnValueHandler;
import infra.web.handler.result.SmartReturnValueHandler;

/**
 * {@link ReturnValueHandler} for functional web {@link ServerResponse} values.
 *
 * <p>Writes the response through {@link ServerResponse#writeTo} and then
 * continues with the value returned by it, mirroring the way other return
 * values are further processed by the {@code ReturnValueHandler} chain. This
 * makes {@code ServerResponse} a first-class return value, including for
 * asynchronous concurrent results and exception handlers.
 *
 * <p>Streaming responses, such as
 * {@link infra.web.handler.function.ServerResponse#sse}, write their status and
 * headers first and return an emitter. The emitter is then delegated to
 * {@link ResponseBodyEmitterReturnValueHandler} for initialization, timeout and
 * completion handling.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class ServerResponseReturnValueHandler implements SmartReturnValueHandler {

  private final ReturnValueHandler delegate;

  private final RequestResponseBodyMethodProcessor bodyWriter;

  private final List<HttpMessageConverter<?>> converters;

  /**
   * Create a new handler.
   *
   * @param delegate the handler to continue with the value produced by
   * {@link ServerResponse#writeTo}; usually the full return-value handler list
   * @param bodyWriter the MVC processor for writing entity bodies
   * @param converters the converters for streaming bodies
   */
  public ServerResponseReturnValueHandler(ReturnValueHandler delegate,
          RequestResponseBodyMethodProcessor bodyWriter, List<HttpMessageConverter<?>> converters) {
    Assert.notNull(delegate, "ReturnValueHandler delegate is required");
    this.delegate = delegate;
    this.bodyWriter = bodyWriter;
    this.converters = converters;
  }

  @Override
  public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    return supportsReturnValue(returnValue);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof ServerResponse || returnValue instanceof DefaultEntityResponseBuilder.Body;
  }

  @Override
  public void handleReturnValue(HttpContext request, @Nullable Object handler,
          @Nullable Object returnValue) throws Exception {

    Object result = returnValue instanceof ServerResponse response ? response.writeTo(request) : returnValue;
    if (result == NONE_RETURN_VALUE) {
      return;
    }
    if (result instanceof DefaultEntityResponseBuilder.Body body) {
      try {
        this.bodyWriter.writeBody(request, body.value(), body.type());
      }
      catch (Throwable failure) {
        if (request.isCommitted()) {
          throw failure;
        }
        this.delegate.handleReturnValue(request, handler, body.response().handleError(failure, request));
      }
    }
    else if (result instanceof StreamingServerResponse.Stream stream) {
      stream.start(request, handler, this.converters);
    }
    else if (result instanceof DefaultEntityResponseBuilder.PublisherBody body) {
      DeferredResult<Object> deferredResult = new DeferredResult<>();
      request.asyncManager().startDeferredResultProcessing(deferredResult, handler);
      body.publisher().subscribe(new EntitySubscriber(request, body, deferredResult));
    }
    else {
      this.delegate.handleReturnValue(request, handler, result);
    }
  }

  private final class EntitySubscriber implements Subscriber<Object> {

    private final HttpContext request;

    private final DefaultEntityResponseBuilder.PublisherBody body;

    private final DeferredResult<Object> result;

    private @Nullable Subscription subscription;

    EntitySubscriber(HttpContext request, DefaultEntityResponseBuilder.PublisherBody body, DeferredResult<Object> result) {
      this.request = request;
      this.body = body;
      this.result = result;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      if (this.subscription != null) {
        subscription.cancel();
        return;
      }
      this.subscription = subscription;
      this.result.onCompletion(subscription::cancel);
      subscription.request(1);
    }

    @Override
    public void onNext(Object value) {
      Subscription subscription = this.subscription;
      Assert.state(subscription != null, "No subscription");
      try {
        bodyWriter.writeBody(this.request, value, this.body.elementType());
        this.request.flush();
        subscription.request(1);
      }
      catch (Throwable failure) {
        subscription.cancel();
        onError(failure);
      }
    }

    @Override
    public void onError(Throwable failure) {
      if (this.request.isCommitted()) {
        this.result.setErrorResult(failure);
        return;
      }
      try {
        this.result.setResult(this.body.response().handleError(failure, this.request));
      }
      catch (Throwable unhandled) {
        this.result.setErrorResult(unhandled);
      }
    }

    @Override
    public void onComplete() {
      try {
        this.request.flush();
        this.result.setResult(null);
      }
      catch (IOException failure) {
        this.result.setErrorResult(failure);
      }
    }
  }
}
