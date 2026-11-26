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

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.SseUtils;
import infra.util.Assert;
import infra.web.ServerHttpExchange;
import infra.web.handler.result.SseEmitter;

/**
 * Implementation of {@link ServerResponse} for sending
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>.
 *
 * <p>This response writes the status and headers and then returns an
 * {@link SseEmitter}. Handling is completed by the return-value handler chain,
 * so SSE responses share the emitter lifecycle, timeout and completion handling
 * with MVC's {@code SseEmitter} support.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SseServerResponse extends AbstractServerResponse {

  private final Consumer<SseBuilder> sseConsumer;

  private final @Nullable Duration timeout;

  public SseServerResponse(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
    super(HttpStatus.OK, HttpHeaders.empty(), null);
    Assert.notNull(sseConsumer, "SseConsumer is required");
    this.sseConsumer = sseConsumer;
    this.timeout = timeout;
  }

  @Override
  protected Object writeToInternal(ServerHttpExchange request, Context context) throws Exception {
    request.setHeader(HttpHeaders.CACHE_CONTROL, HttpHeaders.NO_CACHE);
    SseEmitter emitter = new SseEmitter(timeout, null);
    sseConsumer.accept(new DefaultSseBuilder(emitter));
    return emitter;
  }

  /**
   * Adapts {@link SseBuilder} to {@link SseEmitter.SseEventBuilder}.
   * <p>Fields are accumulated in an event builder; each {@code data} or
   * {@link #send()} call completes the current event and sends it through
   * the emitter.
   */
  private static final class DefaultSseBuilder implements SseBuilder {

    private final SseEmitter emitter;

    private SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event();

    public DefaultSseBuilder(SseEmitter emitter) {
      this.emitter = emitter;
    }

    @Override
    public void send(Object object) throws IOException {
      data(object);
    }

    @Override
    public void send() throws IOException {
      sendEvent();
    }

    private void sendEvent() throws IOException {
      SseEmitter.SseEventBuilder builder = this.eventBuilder;
      this.eventBuilder = SseEmitter.event();
      this.emitter.send(builder);
    }

    @Override
    public SseBuilder id(String id) {
      Assert.hasLength(id, "Id must not be empty");
      SseUtils.assertNoLineSeparator(id);
      this.eventBuilder.id(id);
      return this;
    }

    @Override
    public SseBuilder event(String eventName) {
      Assert.hasLength(eventName, "Name must not be empty");
      SseUtils.assertNoLineSeparator(eventName);
      this.eventBuilder.name(eventName);
      return this;
    }

    @Override
    public SseBuilder retry(Duration duration) {
      Assert.notNull(duration, "Duration is required");
      this.eventBuilder.reconnectTime(duration.toMillis());
      return this;
    }

    @Override
    public SseBuilder comment(String comment) {
      this.eventBuilder.comment(comment);
      return this;
    }

    @Override
    public void data(Object object) throws IOException {
      data(object, MediaType.APPLICATION_JSON);
    }

    @Override
    public void data(Object object, @Nullable MediaType mediaType) throws IOException {
      Assert.notNull(object, "Object is required");
      // Strings are written as-is, without JSON quoting; other objects are
      // serialized with the given media type hint.
      this.eventBuilder.data(object, object instanceof String ? null : mediaType);
      sendEvent();
    }

    @Override
    public void error(Throwable t) {
      this.emitter.completeWithError(t);
    }

    @Override
    public void complete() {
      this.emitter.complete();
    }

    @Override
    public SseBuilder onTimeout(Runnable onTimeout) {
      this.emitter.onTimeout(onTimeout);
      return this;
    }

    @Override
    public SseBuilder onError(Consumer<Throwable> onError) {
      this.emitter.onError(onError);
      return this;
    }

    @Override
    public SseBuilder onComplete(Runnable onCompletion) {
      this.emitter.onCompletion(onCompletion);
      return this;
    }

  }
}
