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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;

import infra.context.ApplicationContext;
import infra.util.ExceptionUtils;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import infra.web.HttpContextHolder;
import infra.web.server.RequestBodySizeExceededException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.HttpConversionUtil;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;

/**
 * {@link NettyHttpContext} backed by native HTTP/2 frames.
 * <p>
 * Request metadata is taken from {@link Http2Headers}; pseudo headers are mapped
 * once into a regular header view so the rest of the context can stay protocol
 * agnostic.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http2HttpContext extends NettyHttpContext implements Runnable {

  private static final CharSequence CONTINUE_TOKEN = "100-continue";

  private final HttpTrafficHandler httpTrafficHandler;

  private final HttpHeaders headers;

  private final String uri;

  private final HttpMethod method;

  private final long contentLength;

  private final @Nullable BodyInputStream requestBody;

  private long receivedBytes = 0;

  private volatile boolean continueExpected = false;

  Http2HttpContext(Channel channel, Http2Headers http2Headers, HttpProtocol protocol,
          NettyRequestConfig config, ApplicationContext context, DispatcherHandler dispatcherHandler,
          HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, null, protocol, config, dispatcherHandler);
    this.httpTrafficHandler = httpTrafficHandler;

    this.headers = new DefaultHttpHeaders(false);
    try {
      int streamId = channel instanceof Http2StreamChannel streamChannel ? streamChannel.stream().id() : -1;
      HttpConversionUtil.addHttp2ToHttpHeaders(streamId, http2Headers, headers, HttpVersion.HTTP_1_1, false, false);
    }
    catch (Http2Exception ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }

    CharSequence path = http2Headers.path();
    this.uri = path != null ? path.toString() : "/";
    CharSequence method = http2Headers.method();
    this.method = method != null ? HttpMethod.valueOf(method.toString()) : HttpMethod.GET;
    this.contentLength = contentLength(headers);
    this.requestBody = (contentLength == 0L || (contentLength == -1L && (this.method == GET || this.method == HEAD)))
            ? null : createRequestBody();
  }

  private BodyInputStream createRequestBody() {
    Awaiter awaiter = config.awaiterFactory.apply(this);
    if (config.autoRead) {
      return new BodyInputStream(awaiter, config.dataReceivedQueueCapacity);
    }
    channel.config().setAutoRead(false);
    return new ManualReadingBodyInputStream(awaiter, config.dataReceivedQueueCapacity);
  }

  @Override
  protected String requestUri() {
    return uri;
  }

  @Override
  protected String requestMethod() {
    return method.name();
  }

  @Override
  protected HttpHeaders nettyRequestHeaders() {
    return headers;
  }

  @Override
  protected boolean isKeepAlive() {
    return true;
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  @Override
  protected InputStream createInputStream() {
    return requestBody != null ? requestBody : InputStream.nullInputStream();
  }

  /**
   * Consume a request body chunk. Ownership of {@code data} is transferred to the
   * request body stream, or released when the chunk is rejected or empty.
   */
  @Override
  public void onDataReceived(ByteBuf data, boolean last) {
    final int chunkSize = data.readableBytes();
    final long received = receivedBytes + chunkSize;

    if (received > config.maxContentLength && !continueExpected) {
      data.release();
      if (requestBody != null) {
        requestBody.onError(new IOException(new RequestBodySizeExceededException(config.maxContentLength)));
      }
      return;
    }

    receivedBytes = received;

    if (requestBody != null) {
      if (chunkSize > 0) {
        requestBody.onDataReceived(data);
      }
      else {
        data.release();
      }
      if (last) {
        requestBody.onComplete();
      }
    }
    else {
      data.release();
    }
  }

  @Override
  public void onError(IOException error) {
    if (requestBody != null) {
      requestBody.onError(error);
    }
  }

  @Override
  protected void requestCompletedInternal(@Nullable Throwable notHandled) {
    if (!config.autoRead) {
      channel.config().setAutoRead(true);
    }
    cleanup(null);
    super.requestCompletedInternal(notHandled);
  }

  @Override
  public void run() {
    HttpContextHolder.set(this);

    try {
      if (is100ContinueExpected()) {
        if (!acceptContinue()) {
          response.writeError(HttpResponseStatus.EXPECTATION_FAILED, false);
          return;
        }

        continueExpected = true;
        response.writeContinue();
        headers.remove(HttpHeaderNames.EXPECT);
      }
      else if (contentLength != -1 && contentLength > config.maxContentLength) {
        processException(new RequestBodySizeExceededException(config.maxContentLength));
        return;
      }

      dispatcherHandler.handleRequest(this);
    }
    catch (Throwable e) {
      httpTrafficHandler.handleException(channel, e);
    }
    finally {
      HttpContextHolder.cleanup();
    }
  }

  private boolean is100ContinueExpected() {
    return headers.contains(HttpHeaderNames.EXPECT, CONTINUE_TOKEN, true);
  }

  private boolean acceptContinue() {
    Boolean continueExpected = dispatcherHandler.requestContinueExpected(this);
    if (continueExpected != null) {
      return continueExpected;
    }
    return contentLength <= config.maxContentLength;
  }

  @Override
  public void channelInactive() {
    cleanup(new ClosedChannelException());
  }

  private void cleanup(@Nullable IOException error) {
    if (requestBody != null) {
      if (error != null) {
        requestBody.onError(error);
      }
      requestBody.close();
    }
  }

  private static long contentLength(HttpHeaders headers) {
    CharSequence value = headers.get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value.toString());
    }
    return -1L;
  }

  private final class ManualReadingBodyInputStream extends BodyInputStream {

    private ManualReadingBodyInputStream(Awaiter awaiter, int capacity) {
      super(awaiter, capacity);
    }

    @Override
    protected void requestNext() {
      channel.read();
    }

  }

}
