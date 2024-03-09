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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.DeferredResult;

/**
 * Implementation of {@link ServerResponse} for sending
 * <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SseServerResponse extends AbstractServerResponse {

  private final Consumer<SseBuilder> sseConsumer;

  @Nullable
  private final Duration timeout;

  public SseServerResponse(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
    super(HttpStatus.OK, createHeaders(), null);
    Assert.notNull(sseConsumer, "SseConsumer is required");
    this.sseConsumer = sseConsumer;
    this.timeout = timeout;
  }

  private static HttpHeaders createHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_EVENT_STREAM);
    headers.setCacheControl(CacheControl.noCache());
    return headers;
  }

  @Override
  protected Object writeToInternal(RequestContext request, Context context) throws Exception {
    DeferredResult<?> result;
    if (timeout != null) {
      result = new DeferredResult<>(timeout.toMillis());
    }
    else {
      result = new DeferredResult<>();
    }

    DefaultAsyncServerResponse.writeAsync(request, result);
    sseConsumer.accept(new DefaultSseBuilder(request, context, result));
    return NONE_RETURN_VALUE;
  }

  private static final class DefaultSseBuilder implements SseBuilder {

    private static final byte[] NL_NL = new byte[] { '\n', '\n' };

    private final DeferredResult<?> deferredResult;

    private final List<HttpMessageConverter<?>> messageConverters;

    private final StringBuilder builder = new StringBuilder();

    private final RequestContext request;

    private boolean sendFailed;

    public DefaultSseBuilder(RequestContext request, Context context, DeferredResult<?> deferredResult) {
      this.request = request;
      this.deferredResult = deferredResult;
      this.messageConverters = context.messageConverters();
    }

    @Override
    public void send(Object object) throws IOException {
      data(object);
    }

    @Override
    public void send() throws IOException {
      this.builder.append('\n');
      try {
        OutputStream body = request.getOutputStream();
        body.write(builderBytes());
        request.flush();
      }
      catch (IOException ex) {
        this.sendFailed = true;
        throw ex;
      }
      finally {
        this.builder.setLength(0);
      }
    }

    @Override
    public SseBuilder id(String id) {
      Assert.hasLength(id, "Id must not be empty");
      return field("id", id);
    }

    @Override
    public SseBuilder event(String eventName) {
      Assert.hasLength(eventName, "Name must not be empty");
      return field("event", eventName);
    }

    @Override
    public SseBuilder retry(Duration duration) {
      Assert.notNull(duration, "Duration is required");
      String millis = Long.toString(duration.toMillis());
      return field("retry", millis);
    }

    @Override
    public SseBuilder comment(String comment) {
      Assert.hasLength(comment, "Comment must not be empty");
      String[] lines = comment.split("\n");
      for (String line : lines) {
        field("", line);
      }
      return this;
    }

    private SseBuilder field(String name, String value) {
      builder.append(name).append(':').append(value).append('\n');
      return this;
    }

    @Override
    public void data(Object object) throws IOException {
      data(object, MediaType.APPLICATION_JSON);
    }

    @Override
    public void data(Object object, @Nullable MediaType mediaType) throws IOException {
      Assert.notNull(object, "Object is required");
      if (object instanceof String) {
        writeString((String) object);
      }
      else {
        writeObject(object, mediaType);
      }
    }

    private void writeString(String string) throws IOException {
      String[] lines = string.split("\n");
      for (String line : lines) {
        field("data", line);
      }
      send();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void writeObject(Object data, @Nullable MediaType mediaType) throws IOException {
      builder.append("data:");
      try {
        OutputStream body = request.getOutputStream();
        body.write(builderBytes());

        Class<?> dataClass = data.getClass();
        for (HttpMessageConverter converter : messageConverters) {
          if (converter.canWrite(dataClass, mediaType)) {
            converter.write(data, mediaType, request.asHttpOutputMessage());
            body.write(NL_NL);
            request.flush();
            return;
          }
        }
      }
      catch (IOException ex) {
        this.sendFailed = true;
        throw ex;
      }
      finally {
        builder.setLength(0);
      }
    }

    private byte[] builderBytes() {
      return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void error(Throwable t) {
      if (sendFailed) {
        return;
      }
      deferredResult.setErrorResult(t);
    }

    @Override
    public void complete() {
      if (sendFailed) {
        return;
      }
      try {
        request.flush();
        deferredResult.setResult(null);
      }
      catch (IOException ex) {
        deferredResult.setErrorResult(ex);
      }
    }

    @Override
    public SseBuilder onTimeout(Runnable onTimeout) {
      deferredResult.onTimeout(onTimeout);
      return this;
    }

    @Override
    public SseBuilder onError(Consumer<Throwable> onError) {
      deferredResult.onError(onError);
      return this;
    }

    @Override
    public SseBuilder onComplete(Runnable onCompletion) {
      deferredResult.onCompletion(onCompletion);
      return this;
    }

  }
}
