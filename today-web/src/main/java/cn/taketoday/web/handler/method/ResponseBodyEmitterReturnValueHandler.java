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

package cn.taketoday.web.handler.method;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.async.DeferredResult;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;

/**
 * Handler for return values of type {@link ResponseBodyEmitter} and sub-classes
 * such as {@link SseEmitter} including the same types wrapped with
 * {@link ResponseEntity}.
 *
 * <p>also supports reactive return value types for any reactive
 * library with registered adapters in {@link ReactiveAdapterRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 13:36
 */
public class ResponseBodyEmitterReturnValueHandler implements SmartReturnValueHandler {

  private final List<HttpMessageConverter<?>> sseMessageConverters;

  private final ReactiveTypeHandler reactiveHandler;

  /**
   * Simple constructor with reactive type support based on a default instance of
   * {@link ReactiveAdapterRegistry},
   * {@link cn.taketoday.core.task.SyncTaskExecutor}, and
   * {@link ContentNegotiationManager} with an Accept header strategy.
   */
  public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
    Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
    this.sseMessageConverters = initSseConverters(messageConverters);
    this.reactiveHandler = new ReactiveTypeHandler();
  }

  /**
   * Complete constructor with pluggable "reactive" type support.
   *
   * @param messageConverters converters to write emitted objects with
   * @param manager for detecting streaming media types
   */
  public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters, ContentNegotiationManager manager) {
    Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
    this.sseMessageConverters = initSseConverters(messageConverters);
    this.reactiveHandler = new ReactiveTypeHandler(manager);
  }

  /**
   * Complete constructor with pluggable "reactive" type support.
   *
   * @param messageConverters converters to write emitted objects with
   * @param registry for reactive return value type support
   * @param executor for blocking I/O writes of items emitted from reactive types
   * @param manager for detecting streaming media types
   */
  public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters,
          ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {
    Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
    this.sseMessageConverters = initSseConverters(messageConverters);
    this.reactiveHandler = new ReactiveTypeHandler(registry, executor, manager);
  }

  private static List<HttpMessageConverter<?>> initSseConverters(List<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> converter : converters) {
      if (converter.canWrite(String.class, MediaType.TEXT_PLAIN)) {
        return converters;
      }
    }

    var result = new ArrayList<HttpMessageConverter<?>>(converters.size() + 1);
    result.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    result.addAll(converters);
    return result;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return supportsReturnType(handlerMethod.getReturnType());
    }
    return false;
  }

  @Override
  public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return supportsReturnValue(returnValue)
              || supportsReturnType(handlerMethod.getReturnValueType(returnValue));
    }
    return supportsReturnValue(returnValue);
  }

  public boolean supportsReturnType(MethodParameter returnType) {
    Class<?> bodyType = ResponseEntity.class.isAssignableFrom(returnType.getParameterType())
            ? ResolvableType.forMethodParameter(returnType).getGeneric().resolve()
            : returnType.getParameterType();
    return bodyType != null
            && (
            ResponseBodyEmitter.class.isAssignableFrom(bodyType)
                    || reactiveHandler.isReactiveType(bodyType)
    );
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof ResponseBodyEmitter;
  }

  @Override
  public void handleReturnValue(RequestContext request, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    // maybe nested body
    MethodParameter returnType = null;
    if (handler instanceof HandlerMethod handlerMethod) {
      returnType = handlerMethod.getReturnType();
      // for ResponseEntity unwrap body
      if (returnValue instanceof ResponseEntity<?> entity) {
        request.setStatus(entity.getStatusCode());
        request.mergeToResponse(entity.getHeaders());
        returnValue = entity.getBody();
        returnType = returnType.nested();
        if (returnValue == null) {
          request.flush();
          return;
        }
      }
    }

    ResponseBodyEmitter emitter;
    // for ResponseBodyEmitter
    if (returnValue instanceof ResponseBodyEmitter) {
      emitter = (ResponseBodyEmitter) returnValue;
    }
    else if (returnType != null) {
      // for reactive types
      emitter = reactiveHandler.handleValue(returnValue, returnType, request);
      if (emitter == null) {
        // Not streaming: write headers without committing response..
        return;
      }
    }
    else {
      // should not happen
      throw new UnsupportedOperationException(
              "Unsupported handler: '%s' and its return-value: '%s'".formatted(handler, returnValue));
    }
    emitter.extendResponse(request);

    HttpMessageConvertingHandler responseBodyEmitter;
    // the response will ignore further header changes
    // Headers will be flushed at the first write
    try {
      DeferredResult<?> deferredResult = new DeferredResult<>(emitter.getTimeout());

      request.getAsyncManager()
              .startDeferredResultProcessing(deferredResult, handler);

      responseBodyEmitter = new HttpMessageConvertingHandler(request, deferredResult);
    }
    catch (Throwable ex) {
      emitter.initializeWithError(ex);
      throw ex;
    }

    emitter.initialize(responseBodyEmitter);
  }

  /**
   * ResponseBodyEmitter.Handler that writes with HttpMessageConverter's.
   */
  private class HttpMessageConvertingHandler implements ResponseBodyEmitter.Handler {

    private final RequestContext request;

    private final DeferredResult<?> deferredResult;

    public HttpMessageConvertingHandler(RequestContext request, DeferredResult<?> deferredResult) {
      this.request = request;
      this.deferredResult = deferredResult;
    }

    @Override
    public void send(Object data, @Nullable MediaType mediaType) throws IOException {
      sendInternal(data, mediaType);
      request.flush();
    }

    @Override
    public void send(Collection<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
      for (ResponseBodyEmitter.DataWithMediaType item : items) {
        sendInternal(item.data, item.mediaType);
      }
      request.flush();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void sendInternal(Object data, @Nullable MediaType mediaType) throws IOException {
      Class<?> dataClass = data.getClass();
      for (HttpMessageConverter converter : sseMessageConverters) {
        if (converter.canWrite(dataClass, mediaType)) {
          converter.write(data, mediaType, request.asHttpOutputMessage());
          return;
        }
      }
      throw new IllegalArgumentException("No suitable converter for " + data.getClass());
    }

    @Override
    public void complete() {
      try {
        request.flush();
        deferredResult.setResult(null);
      }
      catch (IOException ex) {
        deferredResult.setErrorResult(ex);
      }
    }

    @Override
    public void completeWithError(Throwable failure) {
      deferredResult.setErrorResult(failure);
    }

    @Override
    public void onTimeout(Runnable callback) {
      deferredResult.onTimeout(callback);
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
      deferredResult.onError(callback);
    }

    @Override
    public void onCompletion(Runnable callback) {
      deferredResult.onCompletion(callback);
    }
  }

}
