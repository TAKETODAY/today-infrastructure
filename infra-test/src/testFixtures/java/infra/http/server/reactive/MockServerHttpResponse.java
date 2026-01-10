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

package infra.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import org.jspecify.annotations.Nullable;
import infra.mock.api.AsyncContext;
import infra.mock.api.AsyncEvent;
import infra.mock.api.AsyncListener;
import infra.mock.api.MockOutputStream;
import infra.mock.api.WriteListener;
import infra.mock.api.http.HttpMockResponse;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpMockResponse}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MockServerHttpResponse extends AbstractListenerServerHttpResponse {

  private final int bufferSize;

  private volatile boolean flushOnNext;

  private final HttpMockResponse response;

  private final MockOutputStream outputStream;

  private final MockServerHttpRequest request;

  private final ResponseAsyncListener asyncListener;

  @Nullable
  private volatile ResponseBodyProcessor bodyProcessor;

  @Nullable
  private volatile ResponseBodyFlushProcessor bodyFlushProcessor;

  public MockServerHttpResponse(HttpMockResponse response, AsyncContext asyncContext,
          DataBufferFactory bufferFactory, int bufferSize, MockServerHttpRequest request) throws IOException {

    this(HttpHeaders.forWritable(), response, asyncContext, bufferFactory, bufferSize, request);
  }

  public MockServerHttpResponse(HttpHeaders headers, HttpMockResponse response, AsyncContext asyncContext,
          DataBufferFactory bufferFactory, int bufferSize, MockServerHttpRequest request) throws IOException {
    super(bufferFactory, headers);

    Assert.notNull(response, "HttpServletResponse is required");
    Assert.notNull(bufferFactory, "DataBufferFactory is required");
    Assert.isTrue(bufferSize > 0, "Buffer size must be greater than 0");

    this.response = response;
    this.outputStream = response.getOutputStream();
    this.bufferSize = bufferSize;
    this.request = request;

    this.asyncListener = new ResponseAsyncListener();

    // Tomcat expects WriteListener registration on initial thread
    response.getOutputStream().setWriteListener(new ResponseBodyWriteListener());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeResponse() {
    return (T) this.response;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    HttpStatusCode status = super.getStatusCode();
    return (status != null ? status : HttpStatus.resolve(this.response.getStatus()));
  }

  @Override
  public Integer getRawStatusCode() {
    Integer status = super.getRawStatusCode();
    return (status != null ? status : this.response.getStatus());
  }

  @Override
  protected void applyStatusCode() {
    Integer status = super.getRawStatusCode();
    if (status != null) {
      this.response.setStatus(status);
    }
  }

  @Override
  protected void applyHeaders() {
    HttpHeaders httpHeaders = getHeaders();
    for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
      String headerName = entry.getKey();
      for (String headerValue : entry.getValue()) {
        response.addHeader(headerName, headerValue);
      }
    }
    MediaType contentType = null;
    try {
      contentType = httpHeaders.getContentType();
    }
    catch (Exception ex) {
      String rawContentType = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
      response.setContentType(rawContentType);
    }
    if (response.getContentType() == null && contentType != null) {
      response.setContentType(contentType.toString());
    }
    Charset charset = (contentType != null ? contentType.getCharset() : null);
    if (response.getCharacterEncoding() == null && charset != null) {
      response.setCharacterEncoding(charset.name());
    }
    long contentLength = httpHeaders.getContentLength();
    if (contentLength != -1) {
      response.setContentLengthLong(contentLength);
    }
  }

  @Override
  protected void applyCookies() {

    // Servlet Cookie doesn't support same site:
    // https://github.com/eclipse-ee4j/servlet-api/issues/175

    // For Tomcat it seems to be a global option only:
    // https://tomcat.apache.org/tomcat-8.5-doc/config/cookie-processor.html

    for (List<ResponseCookie> cookies : getCookies().values()) {
      for (ResponseCookie cookie : cookies) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
      }
    }
  }

  /**
   * Return an {@link ResponseAsyncListener} that notifies the response
   * body Publisher and Subscriber of Servlet container events. The listener
   * is not actually registered but is rather exposed for
   * {@link MockHttpHandlerAdapter} to ensure events are delegated.
   */
  AsyncListener getAsyncListener() {
    return this.asyncListener;
  }

  @Override
  protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
    ResponseBodyFlushProcessor processor = new ResponseBodyFlushProcessor();
    this.bodyFlushProcessor = processor;
    return processor;
  }

  /**
   * Return the {@link MockOutputStream} for the current response.
   */
  protected final MockOutputStream getOutputStream() {
    return this.outputStream;
  }

  /**
   * Write the DataBuffer to the response body OutputStream.
   * Invoked only when {@link MockOutputStream#isReady()} returns "true"
   * and the readable bytes in the DataBuffer is greater than 0.
   *
   * @return the number of bytes written
   */
  protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
    MockOutputStream outputStream = this.outputStream;
    InputStream input = dataBuffer.asInputStream();
    int bytesWritten = 0;
    byte[] buffer = new byte[this.bufferSize];
    int bytesRead;
    while (outputStream.isReady() && (bytesRead = input.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
      bytesWritten += bytesRead;
    }
    return bytesWritten;
  }

  private void flush() throws IOException {
    MockOutputStream outputStream = this.outputStream;
    if (outputStream.isReady()) {
      try {
        outputStream.flush();
        this.flushOnNext = false;
      }
      catch (IOException ex) {
        this.flushOnNext = true;
        throw ex;
      }
    }
    else {
      this.flushOnNext = true;
    }
  }

  private boolean isWritePossible() {
    return this.outputStream.isReady();
  }

  private final class ResponseAsyncListener implements AsyncListener {

    @Override
    public void onStartAsync(AsyncEvent event) { }

    @Override
    public void onTimeout(AsyncEvent event) {
      Throwable ex = event.getThrowable();
      ex = (ex != null ? ex : new IllegalStateException("Async operation timeout."));
      handleError(ex);
    }

    @Override
    public void onError(AsyncEvent event) {
      handleError(event.getThrowable());
    }

    public void handleError(Throwable ex) {
      ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
      ResponseBodyProcessor processor = bodyProcessor;
      if (flushProcessor != null) {
        // Cancel the upstream source of "write" Publishers
        flushProcessor.cancel();
        // Cancel the current "write" Publisher and propagate onComplete downstream
        if (processor != null) {
          processor.cancel();
          processor.onError(ex);
        }
        // This is a no-op if processor was connected and onError propagated all the way
        flushProcessor.onError(ex);
      }
    }

    @Override
    public void onComplete(AsyncEvent event) {
      ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
      ResponseBodyProcessor processor = bodyProcessor;
      if (flushProcessor != null) {
        // Cancel the upstream source of "write" Publishers
        flushProcessor.cancel();
        // Cancel the current "write" Publisher and propagate onComplete downstream
        if (processor != null) {
          processor.cancel();
          processor.onComplete();
        }
        // This is a no-op if processor was connected and onComplete propagated all the way
        flushProcessor.onComplete();
      }
    }
  }

  private class ResponseBodyWriteListener implements WriteListener {

    @Override
    public void onWritePossible() {
      ResponseBodyProcessor processor = bodyProcessor;
      if (processor != null) {
        processor.onWritePossible();
      }
      else {
        ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
        if (flushProcessor != null) {
          flushProcessor.onFlushPossible();
        }
      }
    }

    @Override
    public void onError(Throwable ex) {
      MockServerHttpResponse.this.asyncListener.handleError(ex);
    }
  }

  private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

    public ResponseBodyFlushProcessor() {
      super(request.getLogPrefix());
    }

    @Override
    protected Processor<? super DataBuffer, Void> createWriteProcessor() {
      ResponseBodyProcessor processor = new ResponseBodyProcessor();
      bodyProcessor = processor;
      return processor;
    }

    @Override
    protected void flush() throws IOException {
      if (rsWriteFlushLogger.isTraceEnabled()) {
        rsWriteFlushLogger.trace("{}flushing", getLogPrefix());
      }
      MockServerHttpResponse.this.flush();
    }

    @Override
    protected boolean isWritePossible() {
      return MockServerHttpResponse.this.isWritePossible();
    }

    @Override
    protected boolean isFlushPending() {
      return flushOnNext;
    }
  }

  private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

    public ResponseBodyProcessor() {
      super(request.getLogPrefix());
    }

    @Override
    protected boolean isWritePossible() {
      return MockServerHttpResponse.this.isWritePossible();
    }

    @Override
    protected boolean isDataEmpty(DataBuffer dataBuffer) {
      return dataBuffer.readableBytes() == 0;
    }

    @Override
    protected boolean write(DataBuffer dataBuffer) throws IOException {
      if (MockServerHttpResponse.this.flushOnNext) {
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}flushing", getLogPrefix());
        }
        flush();
      }

      boolean ready = MockServerHttpResponse.this.isWritePossible();
      int remaining = dataBuffer.readableBytes();
      if (ready && remaining > 0) {
        // In case of IOException, onError handling should call discardData(DataBuffer)..
        int written = writeToOutputStream(dataBuffer);
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}Wrote {} of {} bytes", getLogPrefix(), written, remaining);
        }
        if (written == remaining) {
          dataBuffer.release();
          return true;
        }
      }
      else {
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}ready: {}, remaining: {}", getLogPrefix(), ready, remaining);
        }
      }

      return false;
    }

    @Override
    protected void writingComplete() {
      bodyProcessor = null;
    }

    @Override
    protected void discardData(DataBuffer dataBuffer) {
      dataBuffer.release();
    }
  }

}
