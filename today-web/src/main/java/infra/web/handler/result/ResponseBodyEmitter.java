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

package infra.web.handler.result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Nullable;
import infra.util.ObjectUtils;
import infra.web.RequestContext;
import infra.web.async.DeferredResult;

/**
 * A controller method return value type for asynchronous request processing
 * where one or more objects are written to the response.
 *
 * <p>While {@link DeferredResult}
 * is used to produce a single result, a {@code ResponseBodyEmitter} can be used
 * to send multiple objects where each object is written with a compatible
 * {@link HttpMessageConverter}.
 *
 * <p>Supported as a return type on its own as well as within a
 * {@link ResponseEntity}.
 *
 * <pre>{@code
 * @GET("/stream")
 * public ResponseBodyEmitter handle() {
 *   ResponseBodyEmitter emitter = forChunkedTransferEncoding();
 *   // Pass the emitter to another component...
 *   return emitter;
 * }
 *
 * // in another thread
 * emitter.send(foo1);
 *
 * // and again
 * emitter.send(foo2);
 *
 * // and done
 * emitter.complete();
 * }</pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #forChunkedTransferEncoding()
 * @since 4.0 2022/4/8 23:54
 */
public class ResponseBodyEmitter {

  @Nullable
  private final Long timeout;

  @Nullable
  private final MediaType contentType;

  private final DefaultCallback timeoutCallback = new DefaultCallback();

  private final ErrorCallback errorCallback = new ErrorCallback();

  private final DefaultCallback completionCallback = new DefaultCallback();

  /**
   * Store send data before handler is initialized.
   */
  private final ArrayList<DataWithMediaType> earlySendAttempts = new ArrayList<>();

  /**
   * Guards access to write operations on the response.
   *
   * @since 5.0
   */
  protected final ReentrantLock writeLock = new ReentrantLock();

  /**
   * Store an error before the handler is initialized.
   */
  @Nullable
  private Throwable failure;

  @Nullable
  private Handler handler;

  /**
   * Store successful completion before the handler is initialized.
   */
  private boolean complete;

  /**
   * Create a ResponseBodyEmitter with a custom timeout value.
   * <p>By default, not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   * @param contentType response content-type header
   * @since 5.0
   */
  protected ResponseBodyEmitter(@Nullable Long timeout, @Nullable MediaType contentType) {
    this.timeout = timeout;
    this.contentType = contentType;
  }

  /**
   * Return the configured timeout value, if any.
   */
  @Nullable
  public Long getTimeout() {
    return timeout;
  }

  /**
   * Write the given object to the response.
   * <p>If any exception occurs a dispatch is made back to the app server where
   * Web MVC will pass the exception through its exception handling mechanism.
   * <p><strong>Note:</strong> if the send fails with an IOException, you do
   * not need to call {@link #completeWithError(Throwable)} in order to clean
   * up. Instead, the Web container creates a notification that results in a
   * dispatch where Web MVC invokes exception resolvers and completes
   * processing.
   *
   * @param object the object to write
   * @throws IOException raised when an I/O error occurs
   * @throws java.lang.IllegalStateException wraps any other errors
   */
  public void send(Object object) throws IOException {
    send(object, null);
  }

  /**
   * Overloaded variant of {@link #send(Object)} that also accepts a MediaType
   * hint for how to serialize the given Object.
   *
   * @param object the object to write
   * @param mediaType a MediaType hint for selecting an HttpMessageConverter
   * @throws IOException raised when an I/O error occurs
   * @throws java.lang.IllegalStateException wraps any other errors
   */
  public void send(Object object, @Nullable MediaType mediaType) throws IOException {
    writeLock.lock();
    try {
      if (complete) {
        throw new IllegalStateException("ResponseBodyEmitter has already completed%s"
                .formatted(failure != null ? " with error: " + failure : ""));
      }
      if (handler != null) {
        try {
          handler.send(object, mediaType);
        }
        catch (IOException ex) {
          throw ex;
        }
        catch (Throwable ex) {
          throw new IllegalStateException("Failed to send " + object, ex);
        }
      }
      else {
        earlySendAttempts.add(new DataWithMediaType(object, mediaType));
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Write a set of data and MediaType pairs in a batch.
   * <p>Compared to {@link #send(Object, MediaType)}, this batches the write operations
   * and flushes to the network at the end.
   *
   * @param items the object and media type pairs to write
   * @throws IOException raised when an I/O error occurs
   * @throws java.lang.IllegalStateException wraps any other errors
   */
  public void send(Collection<DataWithMediaType> items) throws IOException {
    writeLock.lock();
    try {
      if (complete) {
        throw new IllegalStateException("ResponseBodyEmitter has already completed%s"
                .formatted(failure != null ? " with error: " + failure : ""));
      }
      sendInternal(items);
    }
    finally {
      writeLock.unlock();
    }
  }

  private void sendInternal(Collection<DataWithMediaType> items) throws IOException {
    if (items.isEmpty()) {
      return;
    }
    if (handler != null) {
      try {
        handler.send(items);
      }
      catch (IOException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to send " + items, ex);
      }
    }
    else {
      earlySendAttempts.addAll(items);
    }
  }

  /**
   * Complete request processing by performing a dispatch into the web
   * container, where Web MVC is invoked once more, and completes the
   * request processing lifecycle.
   * <p><strong>Note:</strong> this method should be called by the application
   * to complete request processing. It should not be used after container
   * related events such as an error while {@link #send(Object) sending}.
   */
  public void complete() {
    writeLock.lock();
    try {
      complete = true;
      if (handler != null) {
        handler.complete();
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Complete request processing with an error.
   * <p>A dispatch is made into the app server where Web MVC will pass the
   * exception through its exception handling mechanism. Note however that
   * at this stage of request processing, the response is committed and the
   * response status can no longer be changed.
   * <p><strong>Note:</strong> this method should be called by the application
   * to complete request processing with an error. It should not be used after
   * container related events such as an error while
   * {@link #send(Object) sending}.
   */
  public void completeWithError(Throwable ex) {
    writeLock.lock();
    try {
      complete = true;
      failure = ex;
      if (handler != null) {
        handler.completeWithError(ex);
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Register code to invoke when the async request times out. This method is
   * called from a container thread when an async request times out.
   * <p>As of 5.0, one can register multiple callbacks for this event.
   */
  public void onTimeout(Runnable callback) {
    writeLock.lock();
    try {
      timeoutCallback.addDelegate(callback);
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Register code to invoke for an error during async request processing.
   * This method is called from a container thread when an error occurred
   * while processing an async request.
   * <p>As of 5.0, one can register multiple callbacks for this event.
   */
  public void onError(Consumer<Throwable> callback) {
    writeLock.lock();
    try {
      errorCallback.addDelegate(callback);
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Register code to invoke when the async request completes. This method is
   * called from a container thread when an async request completed for any
   * reason including timeout and network error. This method is useful for
   * detecting that a {@code ResponseBodyEmitter} instance is no longer usable.
   * <p>As of 5.0, one can register multiple callbacks for this event.
   */
  public void onCompletion(Runnable callback) {
    writeLock.lock();
    try {
      completionCallback.addDelegate(callback);
    }
    finally {
      writeLock.unlock();
    }
  }

  void initialize(Handler handler) throws IOException {
    writeLock.lock();
    try {
      this.handler = handler;

      try {
        sendInternal(earlySendAttempts);
      }
      finally {
        earlySendAttempts.clear();
      }

      if (complete) {
        if (failure != null) {
          handler.completeWithError(failure);
        }
        else {
          handler.complete();
        }
      }
      else {
        handler.onTimeout(timeoutCallback);
        handler.onError(errorCallback);
        handler.onCompletion(completionCallback);
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  void initializeWithError(Throwable ex) {
    writeLock.lock();
    try {
      complete = true;
      failure = ex;
      earlySendAttempts.clear();
      errorCallback.accept(ex);
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Invoked after the response is updated with the status code and headers,
   * if the ResponseBodyEmitter is wrapped in a ResponseEntity, but before the
   * response is committed, i.e. before the response body has been written to.
   * <p>The default implementation is empty.
   *
   * @see HttpHeaders#TRANSFER_ENCODING
   * @see HttpHeaders#CHUNKED
   */
  protected void extendResponse(RequestContext context) {
    context.setHeader(HttpHeaders.TRANSFER_ENCODING, HttpHeaders.CHUNKED);
    if (contentType != null && context.getResponseContentType() == null) {
      context.setContentType(contentType);
    }
  }

  @Override
  public String toString() {
    return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
  }

  /**
   * Create ResponseBodyEmitter in chunked Transfer-Encoding without timeout
   *
   * @since 5.0
   */
  public static ResponseBodyEmitter forChunkedTransferEncoding() {
    return new ResponseBodyEmitter(null, null);
  }

  /**
   * Create a ResponseBodyEmitter with a custom timeout value.
   * <p>By default, not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   */
  public static ResponseBodyEmitter forChunkedTransferEncoding(@Nullable Long timeout) {
    return new ResponseBodyEmitter(timeout, null);
  }

  /**
   * Create a ResponseBodyEmitter with a custom content-type.
   * <p>By default, not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param contentType response content-type header
   * @since 5.0
   */
  public static ResponseBodyEmitter forChunkedTransferEncoding(@Nullable MediaType contentType) {
    return new ResponseBodyEmitter(null, contentType);
  }

  /**
   * Create a ResponseBodyEmitter with a custom timeout value.
   * <p>By default, not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   * @param contentType response content-type header
   * @since 5.0
   */
  public static ResponseBodyEmitter forChunkedTransferEncoding(@Nullable Long timeout, @Nullable MediaType contentType) {
    return new ResponseBodyEmitter(timeout, contentType);
  }

  /**
   * Create a new SseEmitter instance.
   *
   * @since 5.0
   */
  public static SseEmitter forServerSentEvents() {
    return new SseEmitter(null);
  }

  /**
   * Create a SseEmitter with a custom timeout value.
   * <p>By default, not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   * @since 5.0
   */
  public static SseEmitter forServerSentEvents(@Nullable Long timeout) {
    return new SseEmitter(timeout);
  }

  /**
   * Contract to handle the sending of event data, the completion of event
   * sending, and the registration of callbacks to be invoked in case of
   * timeout, error, and completion for any reason (including from the
   * container side).
   */
  interface Handler {

    /**
     * Immediately write and flush the given data to the network.
     */
    void send(Object data, @Nullable MediaType mediaType) throws IOException;

    /**
     * Immediately write all data items then flush to the network.
     */
    void send(Collection<DataWithMediaType> items) throws IOException;

    void complete();

    void completeWithError(Throwable failure);

    void onTimeout(Runnable callback);

    void onError(Consumer<Throwable> callback);

    void onCompletion(Runnable callback);
  }

  /**
   * A simple holder of data to be written along with a MediaType hint for
   * selecting a message converter to write with.
   */
  public static class DataWithMediaType {

    public final Object data;

    @Nullable
    public final MediaType mediaType;

    public DataWithMediaType(Object data, @Nullable MediaType mediaType) {
      this.data = data;
      this.mediaType = mediaType;
    }
  }

  private final class DefaultCallback implements Runnable {

    private final ArrayList<Runnable> delegates = new ArrayList<>(1);

    public void addDelegate(Runnable delegate) {
      delegates.add(delegate);
    }

    @Override
    public void run() {
      ResponseBodyEmitter.this.complete = true;
      for (Runnable delegate : delegates) {
        delegate.run();
      }
    }
  }

  private final class ErrorCallback implements Consumer<Throwable> {

    private final ArrayList<Consumer<Throwable>> delegates = new ArrayList<>(1);

    public void addDelegate(Consumer<Throwable> callback) {
      delegates.add(callback);
    }

    @Override
    public void accept(Throwable t) {
      ResponseBodyEmitter.this.complete = true;
      for (Consumer<Throwable> delegate : delegates) {
        delegate.accept(t);
      }
    }
  }

}
