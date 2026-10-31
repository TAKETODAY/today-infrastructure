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
import java.util.Objects;

import infra.context.ApplicationContext;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import infra.web.HttpContextHolder;
import infra.web.server.RequestBodySizeExceededException;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;

/**
 * Internal {@link NettyHttpContext} implementation that handles the lifecycle of an HTTP request
 * within the Netty transport layer. This class manages request body streaming, content length validation,
 * {@code 100 Continue} expectation handling, and delegates request processing to the configured
 * {@link DispatcherHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class HttpContext extends NettyHttpContext implements Runnable {

  private final HttpTrafficHandler httpTrafficHandler;

  private final @Nullable BodyInputStream requestBody;

  private final long contentLength;

  private long receivedBytes = 0;

  private volatile boolean continueExpected = false;

  public HttpContext(Channel channel, HttpRequest request, HttpProtocol protocol, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, request, protocol, config, dispatcherHandler);
    this.httpTrafficHandler = httpTrafficHandler;
    long contentLength = getContentLength(request);
    this.requestBody = (contentLength == 0L || (contentLength == -1L && (request.method() == GET || request.method() == HEAD))) ? null : createRequestBody();
    if (request instanceof HttpContent content) {
      onDataReceived(content);
    }
    this.contentLength = contentLength;
  }

  private BodyInputStream createRequestBody() {
    Awaiter awaiter = config.awaiterFactory.apply(this);
    if (config.autoRead) {
      return new BodyInputStream(awaiter, config.dataReceivedQueueCapacity);
    }
    channel.config().setAutoRead(false);
    return new ManualReadingBodyInputStream(awaiter, config.dataReceivedQueueCapacity);
  }

  /**
   * Consume an HTTP/1.x request body chunk. Ownership of the chunk is transferred
   * to this method, which either forwards it to the request body stream or releases it.
   */
  public void onDataReceived(HttpContent httpContent) {
    final int chunkSize = httpContent.content().readableBytes();
    final long received = receivedBytes + chunkSize;

    if (received > config.maxContentLength && !continueExpected) {
      httpContent.release();
      if (requestBody != null) {
        requestBody.onError(new IOException(new RequestBodySizeExceededException(config.maxContentLength)));
      }
      return;
    }

    receivedBytes = received;

    boolean last = httpContent instanceof LastHttpContent;
    if (requestBody != null) {
      if (chunkSize > 0) {
        requestBody.onDataReceived(httpContent.content());
      }
      else {
        httpContent.release();
      }
      if (last) {
        requestBody.onComplete();
      }
    }
    else {
      httpContent.release();
    }
  }

  /**
   * Signal a stream-level failure to the request body reader.
   */
  public void onError(IOException error) {
    if (requestBody != null) {
      requestBody.onError(error);
    }
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  @Override
  protected InputStream createInputStream() {
    return requestBody != null ? requestBody : InputStream.nullInputStream();
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
      if (HttpUtil.is100ContinueExpected(Objects.requireNonNull(request))) {
        if (!acceptContinue()) {
          // the expectation failed so we refuse the request.
          response.writeError(HttpResponseStatus.EXPECTATION_FAILED, false);
          return;
        }

        continueExpected = true;
        response.writeContinue();
        Objects.requireNonNull(request).headers().remove(HttpHeaderNames.EXPECT);
      }
      else if (contentLength != -1 && contentLength > config.maxContentLength) {
        processException(new RequestBodySizeExceededException(config.maxContentLength));
        return;
      }

      Throwable decoderFailure = Objects.requireNonNull(request).decoderResult().cause();
      if (decoderFailure != null) {
        processException(decoderFailure);
      }
      else {
        dispatcherHandler.handleRequest(this); // handling HTTP request
      }
    }
    catch (Throwable e) {
      httpTrafficHandler.handleException(channel, e);
    }
    finally {
      HttpContextHolder.cleanup();
    }
  }

  private void cleanup(@Nullable IOException error) {
    if (requestBody != null) {
      if (error != null) {
        requestBody.onError(error);
      }
      requestBody.close();
    }
  }

  public void channelInactive() {
    cleanup(new ClosedChannelException());
  }

  /**
   * Determine whether the request expecting {@code 100 Continue} should be accepted.
   */
  private boolean acceptContinue() {
    Boolean continueExpected = dispatcherHandler.requestContinueExpected(this);
    if (continueExpected != null) {
      return continueExpected;
    }
    // fallback to default max content-length check
    return contentLength <= config.maxContentLength;
  }

  /**
   * Returns the length of the content or the specified default value if the message does not have the {@code
   * "Content-Length" header}. Please note that this value is not retrieved from {@link HttpContent#content()} but
   * from the {@code "Content-Length"} header, and thus they are independent from each other.
   *
   * @param message the message
   * @return the content length or the specified default value
   * @throws NumberFormatException if the {@code "Content-Length"} header does not parse as a long
   */
  static long getContentLength(HttpMessage message) {
    String value = message.headers().get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value);
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

