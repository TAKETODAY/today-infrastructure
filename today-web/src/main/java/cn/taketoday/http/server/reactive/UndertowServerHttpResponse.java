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

package cn.taketoday.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Adapt {@link ServerHttpResponse} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
class UndertowServerHttpResponse extends AbstractListenerServerHttpResponse implements ZeroCopyHttpOutputMessage {

  @Nullable
  private StreamSinkChannel responseChannel;

  private final HttpServerExchange exchange;
  private final UndertowServerHttpRequest request;

  UndertowServerHttpResponse(
          HttpServerExchange exchange, DataBufferFactory bufferFactory, UndertowServerHttpRequest request) {

    super(bufferFactory, createHeaders(exchange));
    Assert.notNull(exchange, "HttpServerExchange must not be null");
    this.exchange = exchange;
    this.request = request;
  }

  private static HttpHeaders createHeaders(HttpServerExchange exchange) {
    UndertowHeadersAdapter headersMap = new UndertowHeadersAdapter(exchange.getResponseHeaders());
    return new DefaultHttpHeaders(headersMap);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeResponse() {
    return (T) this.exchange;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    HttpStatusCode status = super.getStatusCode();
    return (status != null ? status : HttpStatus.resolve(this.exchange.getStatusCode()));
  }

  @Override
  public Integer getRawStatusCode() {
    Integer status = super.getRawStatusCode();
    return (status != null ? status : this.exchange.getStatusCode());
  }

  @Override
  protected void applyStatusCode() {
    Integer status = super.getRawStatusCode();
    if (status != null) {
      this.exchange.setStatusCode(status);
    }
  }

  @Override
  protected void applyHeaders() { }

  @SuppressWarnings("deprecation")
  @Override
  protected void applyCookies() {

    for (Map.Entry<String, List<ResponseCookie>> entry : getCookies().entrySet()) {
      String name = entry.getKey();
      for (ResponseCookie httpCookie : entry.getValue()) {
        CookieImpl cookie = new CookieImpl(name, httpCookie.getValue());
        if (!httpCookie.getMaxAge().isNegative()) {
          cookie.setMaxAge((int) httpCookie.getMaxAge().getSeconds());
        }
        if (httpCookie.getDomain() != null) {
          cookie.setDomain(httpCookie.getDomain());
        }
        if (httpCookie.getPath() != null) {
          cookie.setPath(httpCookie.getPath());
        }
        cookie.setSecure(httpCookie.isSecure());
        cookie.setHttpOnly(httpCookie.isHttpOnly());
        cookie.setSameSiteMode(httpCookie.getSameSite());
        // getResponseCookies() is deprecated in Undertow 2.2
        this.exchange.getResponseCookies().putIfAbsent(name, cookie);
      }
    }
  }

  @Override
  public Mono<Void> writeWith(Path file, long position, long count) {
    return doCommit(() -> Mono.create(sink -> {
      try {
        FileChannel source = FileChannel.open(file, StandardOpenOption.READ);
        TransferBodyListener listener = new TransferBodyListener(source, position, count, sink);
        sink.onDispose(listener::closeSource);
        StreamSinkChannel destination = this.exchange.getResponseChannel();
        destination.getWriteSetter().set(listener::transfer);
        listener.transfer(destination);
      }
      catch (IOException ex) {
        sink.error(ex);
      }
    }));
  }

  @Override
  protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
    return new ResponseBodyFlushProcessor();
  }

  private ResponseBodyProcessor createBodyProcessor() {
    if (this.responseChannel == null) {
      this.responseChannel = this.exchange.getResponseChannel();
    }
    return new ResponseBodyProcessor(this.responseChannel);
  }

  private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

    private final StreamSinkChannel channel;
    /** Keep track of write listener calls, for {@link #writePossible}. */
    private volatile boolean writePossible;

    @Nullable
    private volatile ByteBuffer byteBuffer;

    public ResponseBodyProcessor(StreamSinkChannel channel) {
      super(request.getLogPrefix());
      Assert.notNull(channel, "StreamSinkChannel must not be null");
      this.channel = channel;
      this.channel.getWriteSetter().set(c -> {
        this.writePossible = true;
        onWritePossible();
      });
      this.channel.suspendWrites();
    }

    @Override
    protected boolean isWritePossible() {
      this.channel.resumeWrites();
      return this.writePossible;
    }

    @Override
    protected boolean write(DataBuffer dataBuffer) throws IOException {
      ByteBuffer buffer = this.byteBuffer;
      if (buffer == null) {
        return false;
      }

      // Track write listener calls from here on..
      this.writePossible = false;

      // In case of IOException, onError handling should call discardData(DataBuffer)..
      int total = buffer.remaining();
      int written = writeByteBuffer(buffer);

      if (rsWriteLogger.isTraceEnabled()) {
        rsWriteLogger.trace("{}Wrote {} of {} bytes", getLogPrefix(), written, total);
      }
      if (written != total) {
        return false;
      }

      // We wrote all, so can still write more..
      this.writePossible = true;

      DataBufferUtils.release(dataBuffer);
      this.byteBuffer = null;
      return true;
    }

    private int writeByteBuffer(ByteBuffer byteBuffer) throws IOException {
      int written;
      int totalWritten = 0;
      do {
        written = this.channel.write(byteBuffer);
        totalWritten += written;
      }
      while (byteBuffer.hasRemaining() && written > 0);
      return totalWritten;
    }

    @Override
    protected void dataReceived(DataBuffer dataBuffer) {
      super.dataReceived(dataBuffer);
      this.byteBuffer = dataBuffer.toByteBuffer();
    }

    @Override
    protected boolean isDataEmpty(DataBuffer dataBuffer) {
      return (dataBuffer.readableByteCount() == 0);
    }

    @Override
    protected void writingComplete() {
      this.channel.getWriteSetter().set(null);
      this.channel.resumeWrites();
    }

    @Override
    protected void writingFailed(Throwable ex) {
      cancel();
      onError(ex);
    }

    @Override
    protected void discardData(DataBuffer dataBuffer) {
      DataBufferUtils.release(dataBuffer);
    }
  }

  private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

    public ResponseBodyFlushProcessor() {
      super(request.getLogPrefix());
    }

    @Override
    protected Processor<? super DataBuffer, Void> createWriteProcessor() {
      return UndertowServerHttpResponse.this.createBodyProcessor();
    }

    @Override
    protected void flush() throws IOException {
      StreamSinkChannel channel = UndertowServerHttpResponse.this.responseChannel;
      if (channel != null) {
        if (rsWriteFlushLogger.isTraceEnabled()) {
          rsWriteFlushLogger.trace("{}flush", getLogPrefix());
        }
        channel.flush();
      }
    }

    @Override
    protected boolean isWritePossible() {
      StreamSinkChannel channel = UndertowServerHttpResponse.this.responseChannel;
      if (channel != null) {
        // We can always call flush, just ensure writes are on..
        channel.resumeWrites();
        return true;
      }
      return false;
    }

    @Override
    protected boolean isFlushPending() {
      return false;
    }
  }

  private static class TransferBodyListener {

    private long count;
    private long position;
    private final FileChannel source;
    private final MonoSink<Void> sink;

    public TransferBodyListener(FileChannel source, long position, long count, MonoSink<Void> sink) {
      this.source = source;
      this.sink = sink;
      this.position = position;
      this.count = count;
    }

    public void transfer(StreamSinkChannel destination) {
      try {
        while (this.count > 0) {
          long len = destination.transferFrom(this.source, this.position, this.count);
          if (len != 0) {
            this.position += len;
            this.count -= len;
          }
          else {
            destination.resumeWrites();
            return;
          }
        }
        this.sink.success();
      }
      catch (IOException ex) {
        this.sink.error(ex);
      }

    }

    public void closeSource() {
      try {
        this.source.close();
      }
      catch (IOException ignore) { }
    }

  }

}
