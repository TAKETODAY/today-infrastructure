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

package infra.web.handler.method;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import infra.http.MediaType;
import infra.lang.Nullable;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;

/**
 * A specialization of {@link ResponseBodyEmitter} for sending
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:55
 */
public class SseEmitter extends ResponseBodyEmitter {

  private static final MediaType TEXT_PLAIN = MediaType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

  /**
   * Guards access to write operations on the response.
   */
  private final ReentrantLock writeLock = new ReentrantLock();

  /**
   * Create a new SseEmitter instance.
   */
  public SseEmitter() {
    super();
  }

  /**
   * Create a SseEmitter with a custom timeout value.
   * <p>By default not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   */
  public SseEmitter(Long timeout) {
    super(timeout);
  }

  @Override
  protected void extendResponse(RequestContext outputMessage) {
    super.extendResponse(outputMessage);

    if (outputMessage.getResponseContentType() == null) {
      outputMessage.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
  }

  /**
   * Send the object formatted as a single SSE "data" line. It's equivalent to:
   * <pre>{@code
   * // static import of SseEmitter.*
   *
   * SseEmitter emitter = new SseEmitter();
   * emitter.send(event().data(myObject));
   * }</pre>
   * <p>Please, see {@link ResponseBodyEmitter#send(Object) parent Javadoc}
   * for important notes on exception handling.
   *
   * @param object the object to write
   * @throws IOException raised when an I/O error occurs
   * @throws java.lang.IllegalStateException wraps any other errors
   */
  @Override
  public void send(Object object) throws IOException {
    send(object, null);
  }

  /**
   * Send the object formatted as a single SSE "data" line. It's equivalent to:
   * <pre>{@code
   * // static import of SseEmitter.*
   *
   * SseEmitter emitter = new SseEmitter();
   * emitter.send(event().data(myObject, MediaType.APPLICATION_JSON));
   * }</pre>
   * <p>Please, see {@link ResponseBodyEmitter#send(Object) parent Javadoc}
   * for important notes on exception handling.
   *
   * @param object the object to write
   * @param mediaType a MediaType hint for selecting an HttpMessageConverter
   * @throws IOException raised when an I/O error occurs
   */
  @Override
  public void send(Object object, @Nullable MediaType mediaType) throws IOException {
    send(event().data(object, mediaType));
  }

  /**
   * Send an SSE event prepared with the given builder. For example:
   * <pre>{@code
   * // static import of SseEmitter
   * SseEmitter emitter = new SseEmitter();
   * emitter.send(event().name("update").id("1").data(myObject));
   * }</pre>
   *
   * @param builder a builder for an SSE formatted event.
   * @throws IOException raised when an I/O error occurs
   */
  public void send(SseEventBuilder builder) throws IOException {
    var dataToSend = builder.build();
    this.writeLock.lock();
    try {
      super.send(dataToSend);
    }
    finally {
      this.writeLock.unlock();
    }
  }

  @Override
  public String toString() {
    return "SseEmitter@" + ObjectUtils.getIdentityHexString(this);
  }

  public static SseEventBuilder event() {
    return new SseEventBuilderImpl();
  }

  /**
   * A builder for an SSE event.
   */
  public interface SseEventBuilder {

    /**
     * Add an SSE "id" line.
     */
    SseEventBuilder id(String id);

    /**
     * Add an SSE "event" line.
     */
    SseEventBuilder name(String eventName);

    /**
     * Add an SSE "retry" line.
     */
    SseEventBuilder reconnectTime(long reconnectTimeMillis);

    /**
     * Add an SSE "comment" line.
     */
    SseEventBuilder comment(String comment);

    /**
     * Add an SSE "data" line.
     */
    SseEventBuilder data(Object object);

    /**
     * Add an SSE "data" line.
     */
    SseEventBuilder data(Object object, @Nullable MediaType mediaType);

    /**
     * Return one or more Object-MediaType pairs to write via
     * {@link #send(Object, MediaType)}.
     */
    Collection<DataWithMediaType> build();
  }

  /**
   * Default implementation of SseEventBuilder.
   */
  private static final class SseEventBuilderImpl implements SseEventBuilder {

    private final ArrayList<DataWithMediaType> dataToSend = new ArrayList<>();

    @Nullable
    private StringBuilder sb;

    @Override
    public SseEventBuilder id(String id) {
      append("id:").append(id).append('\n');
      return this;
    }

    @Override
    public SseEventBuilder name(String name) {
      append("event:").append(name).append('\n');
      return this;
    }

    @Override
    public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
      append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
      return this;
    }

    @Override
    public SseEventBuilder comment(String comment) {
      append(':').append(comment).append('\n');
      return this;
    }

    @Override
    public SseEventBuilder data(Object object) {
      return data(object, null);
    }

    @Override
    public SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
      append("data:");
      saveAppendedText();
      if (object instanceof String text) {
        object = StringUtils.replace(text, "\n", "\ndata:");
      }
      this.dataToSend.add(new DataWithMediaType(object, mediaType));
      append('\n');
      return this;
    }

    SseEventBuilderImpl append(String text) {
      if (this.sb == null) {
        this.sb = new StringBuilder();
      }
      this.sb.append(text);
      return this;
    }

    SseEventBuilderImpl append(char ch) {
      if (this.sb == null) {
        this.sb = new StringBuilder();
      }
      this.sb.append(ch);
      return this;
    }

    @Override
    public Collection<DataWithMediaType> build() {
      if (StringUtils.isEmpty(this.sb) && this.dataToSend.isEmpty()) {
        return Collections.emptySet();
      }
      append('\n');
      saveAppendedText();
      return this.dataToSend;
    }

    private void saveAppendedText() {
      if (this.sb != null) {
        this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
        this.sb = null;
      }
    }
  }

}
