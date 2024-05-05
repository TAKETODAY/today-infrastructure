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

package cn.taketoday.http.server.reactive;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.AsyncContext;
import cn.taketoday.mock.api.AsyncEvent;
import cn.taketoday.mock.api.AsyncListener;
import cn.taketoday.mock.api.ServletOutputStream;
import cn.taketoday.mock.api.WriteListener;
import cn.taketoday.mock.api.http.HttpServletResponse;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ServletServerHttpResponse extends AbstractListenerServerHttpResponse {

  private final int bufferSize;

  private volatile boolean flushOnNext;

  private final HttpServletResponse response;

  private final ServletOutputStream outputStream;

  private final ServletServerHttpRequest request;

  private final ResponseAsyncListener asyncListener;

  @Nullable
  private volatile ResponseBodyProcessor bodyProcessor;

  @Nullable
  private volatile ResponseBodyFlushProcessor bodyFlushProcessor;

  public ServletServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
          DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

    this(HttpHeaders.forWritable(), response, asyncContext, bufferFactory, bufferSize, request);
  }

  public ServletServerHttpResponse(HttpHeaders headers, HttpServletResponse response, AsyncContext asyncContext,
          DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {
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
   * {@link ServletHttpHandlerAdapter} to ensure events are delegated.
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
   * Return the {@link ServletOutputStream} for the current response.
   */
  protected final ServletOutputStream getOutputStream() {
    return this.outputStream;
  }

  /**
   * Write the DataBuffer to the response body OutputStream.
   * Invoked only when {@link ServletOutputStream#isReady()} returns "true"
   * and the readable bytes in the DataBuffer is greater than 0.
   *
   * @return the number of bytes written
   */
  protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
    ServletOutputStream outputStream = this.outputStream;
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
    ServletOutputStream outputStream = this.outputStream;
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
      ServletServerHttpResponse.this.asyncListener.handleError(ex);
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
      ServletServerHttpResponse.this.flush();
    }

    @Override
    protected boolean isWritePossible() {
      return ServletServerHttpResponse.this.isWritePossible();
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
      return ServletServerHttpResponse.this.isWritePossible();
    }

    @Override
    protected boolean isDataEmpty(DataBuffer dataBuffer) {
      return dataBuffer.readableByteCount() == 0;
    }

    @Override
    protected boolean write(DataBuffer dataBuffer) throws IOException {
      if (ServletServerHttpResponse.this.flushOnNext) {
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}flushing", getLogPrefix());
        }
        flush();
      }

      boolean ready = ServletServerHttpResponse.this.isWritePossible();
      int remaining = dataBuffer.readableByteCount();
      if (ready && remaining > 0) {
        // In case of IOException, onError handling should call discardData(DataBuffer)..
        int written = writeToOutputStream(dataBuffer);
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}Wrote {} of {} bytes", getLogPrefix(), written, remaining);
        }
        if (written == remaining) {
          DataBufferUtils.release(dataBuffer);
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
      DataBufferUtils.release(dataBuffer);
    }
  }

}
