/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.converter.HttpMessageConverter;
import infra.http.server.DelegatingServerHttpResponse;
import infra.http.server.ServerHttpResponse;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import infra.util.function.ThrowingConsumer;
import infra.web.RequestContext;
import infra.web.async.DeferredResult;

/**
 * Implementation of {@link ServerResponse} for sending streaming response bodies.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class StreamingServerResponse extends AbstractServerResponse {

  private final ThrowingConsumer<StreamBuilder> streamConsumer;

  @Nullable
  private final Duration timeout;

  private StreamingServerResponse(HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies,
          ThrowingConsumer<StreamBuilder> streamConsumer, @Nullable Duration timeout) {
    super(statusCode, headers, cookies);
    this.streamConsumer = streamConsumer;
    this.timeout = timeout;
  }

  static ServerResponse create(HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies,
          ThrowingConsumer<StreamBuilder> streamConsumer, @Nullable Duration timeout) {
    Assert.notNull(statusCode, "statusCode is required");
    Assert.notNull(headers, "headers is required");
    Assert.notNull(cookies, "cookies is required");
    Assert.notNull(streamConsumer, "streamConsumer is required");
    return new StreamingServerResponse(statusCode, headers, cookies, streamConsumer, timeout);
  }

  @Nullable
  @Override
  protected Object writeToInternal(RequestContext request, Context context) throws Throwable {
    DeferredResult<?> result = new DeferredResult<>(timeout != null ? timeout.toMillis() : null);
    DefaultAsyncServerResponse.writeAsync(request, result);
    this.streamConsumer.accept(new DefaultStreamBuilder(request, context, result, headers()));
    return null;
  }

  private static class DefaultStreamBuilder implements StreamBuilder {

    private final ServerHttpResponse outputMessage;

    private final DeferredResult<?> deferredResult;

    private final List<HttpMessageConverter<?>> messageConverters;

    private final HttpHeaders httpHeaders;

    private boolean sendFailed;

    public DefaultStreamBuilder(RequestContext response, Context context,
            DeferredResult<?> deferredResult, HttpHeaders httpHeaders) {
      this.outputMessage = response.asHttpOutputMessage();
      this.deferredResult = deferredResult;
      this.messageConverters = context.messageConverters();
      this.httpHeaders = httpHeaders;
    }

    @Override
    public StreamBuilder write(Object object) throws IOException {
      write(object, null);
      return this;
    }

    @Override
    public StreamBuilder write(Object object, @Nullable MediaType mediaType) throws IOException {
      Assert.notNull(object, "data is required");
      try {
        if (object instanceof byte[] bytes) {
          this.outputMessage.getBody().write(bytes);
        }
        else if (object instanceof String str) {
          this.outputMessage.getBody().write(str.getBytes(StandardCharsets.UTF_8));
        }
        else {
          writeObject(object, mediaType);
        }
      }
      catch (IOException ex) {
        this.sendFailed = true;
        throw ex;
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    private void writeObject(Object data, @Nullable MediaType mediaType) throws IOException {
      Class<?> elementClass = data.getClass();
      for (HttpMessageConverter<?> converter : this.messageConverters) {
        if (converter.canWrite(elementClass, mediaType)) {
          HttpMessageConverter<Object> objectConverter = (HttpMessageConverter<Object>) converter;
          ServerHttpResponse response = new MutableHeadersServerHttpResponse(this.outputMessage, this.httpHeaders);
          objectConverter.write(data, mediaType, response);
          return;
        }
      }
    }

    @Override
    public void flush() throws IOException {
      if (this.sendFailed) {
        return;
      }
      try {
        this.outputMessage.flush();
      }
      catch (IOException ex) {
        this.sendFailed = true;
        throw ex;
      }
    }

    @Override
    public void error(Throwable t) {
      if (this.sendFailed) {
        return;
      }
      this.deferredResult.setErrorResult(t);
    }

    @Override
    public void complete() {
      if (this.sendFailed) {
        return;
      }
      try {
        this.outputMessage.flush();
        this.deferredResult.setResult(null);
      }
      catch (IOException ex) {
        this.deferredResult.setErrorResult(ex);
      }
    }

    @Override
    public StreamBuilder onTimeout(Runnable onTimeout) {
      this.deferredResult.onTimeout(onTimeout);
      return this;
    }

    @Override
    public StreamBuilder onError(Consumer<Throwable> onError) {
      this.deferredResult.onError(onError);
      return this;
    }

    @Override
    public StreamBuilder onComplete(Runnable onCompletion) {
      this.deferredResult.onCompletion(onCompletion);
      return this;
    }

    /**
     * Wrap to silently ignore header changes HttpMessageConverter's that would
     * otherwise cause HttpHeaders to raise exceptions.
     */
    private static final class MutableHeadersServerHttpResponse extends DelegatingServerHttpResponse {

      private final HttpHeaders mutableHeaders = HttpHeaders.forWritable();

      public MutableHeadersServerHttpResponse(ServerHttpResponse delegate, HttpHeaders headers) {
        super(delegate);
        this.mutableHeaders.putAll(delegate.getHeaders());
        this.mutableHeaders.putAll(headers);
      }

      @Override
      public HttpHeaders getHeaders() {
        return this.mutableHeaders;
      }

    }

  }

}
