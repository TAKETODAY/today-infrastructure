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

package cn.taketoday.web.handler.method;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHttpOutputMessage;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.servlet.filter.ShallowEtagHeaderFilter;

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
public class ResponseBodyEmitterReturnValueHandler implements HandlerMethodReturnValueHandler {

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
  public ResponseBodyEmitterReturnValueHandler(
          List<HttpMessageConverter<?>> messageConverters, ContentNegotiationManager manager) {
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
  public boolean supportsHandlerMethod(HandlerMethod handler, @Nullable Object returnValue) {
    MethodParameter returnType = handler.getReturnValueType(returnValue);
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
  public void handleReturnValue(RequestContext request, HandlerMethod handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    MethodParameter returnType = handler.getReturnType();
    HttpHeaders responseHeaders = request.responseHeaders();
    if (returnValue instanceof ResponseEntity<?> responseEntity) {
      request.setStatus(responseEntity.getStatusCode());
      responseHeaders.putAll(responseEntity.getHeaders());
      returnValue = responseEntity.getBody();
      returnType = returnType.nested();
      if (returnValue == null) {
        return;
      }
    }

    ResponseBodyEmitter emitter;
    if (returnValue instanceof ResponseBodyEmitter) {
      emitter = (ResponseBodyEmitter) returnValue;
    }
    else {
      emitter = reactiveHandler.handleValue(returnValue, returnType, request);
      if (emitter == null) {
        // Not streaming: write headers without committing response..
//        responseHeaders.forEach((headerName, headerValues) -> {
//          for (String headerValue : headerValues) {
//            responseHeaders.add(headerName, headerValue);
//          }
//        });
        return;
      }
    }
    emitter.extendResponse(request);

    // At this point we know we're streaming..
    if (ServletDetector.runningInServlet(request)) {
      ShallowEtagHeaderFilter.disableContentCaching(request);
    }

    // Wrap the response to ignore further header changes
    // Headers will be flushed at the first write

    HttpMessageConvertingHandler responseBodyEmitter;
    try {
      DeferredResult<?> deferredResult = new DeferredResult<>(emitter.getTimeout());
      WebAsyncUtils.getAsyncManager(request)
              .startDeferredResultProcessing(deferredResult);
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

    private final RequestContext context;
    private final DeferredResult<?> deferredResult;

    public HttpMessageConvertingHandler(RequestContext context, DeferredResult<?> deferredResult) {
      this.context = context;
      this.deferredResult = deferredResult;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void send(Object data, @Nullable MediaType mediaType) throws IOException {
      Class<?> dataClass = data.getClass();
      for (HttpMessageConverter converter : sseMessageConverters) {
        if (converter.canWrite(dataClass, mediaType)) {
          converter.write(data, mediaType, new RequestContextHttpOutputMessage(context));
          context.flush();
          return;
        }
      }
      throw new IllegalArgumentException("No suitable converter for " + dataClass);
    }

    @Override
    public void complete() {
      try {
        this.context.flush();
        this.deferredResult.setResult(null);
      }
      catch (IOException ex) {
        this.deferredResult.setErrorResult(ex);
      }
    }

    @Override
    public void completeWithError(Throwable failure) {
      this.deferredResult.setErrorResult(failure);
    }

    @Override
    public void onTimeout(Runnable callback) {
      this.deferredResult.onTimeout(callback);
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
      this.deferredResult.onError(callback);
    }

    @Override
    public void onCompletion(Runnable callback) {
      this.deferredResult.onCompletion(callback);
    }
  }

}
