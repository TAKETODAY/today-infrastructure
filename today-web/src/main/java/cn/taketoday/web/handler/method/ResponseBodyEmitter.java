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

package cn.taketoday.web.handler.method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.async.DeferredResult;

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
 * <pre>
 * &#064;GET("/stream")
 * public ResponseBodyEmitter handle() {
 * 	   ResponseBodyEmitter emitter = new ResponseBodyEmitter();
 * 	   // Pass the emitter to another component...
 * 	   return emitter;
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
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:54
 */
public class ResponseBodyEmitter {

  @Nullable
  private final Long timeout;

  @Nullable
  private Handler handler;

  /** Store send data before handler is initialized. */
  private final ArrayList<DataWithMediaType> earlySendAttempts = new ArrayList<>();

  /** Store successful completion before the handler is initialized. */
  private boolean complete;

  /** Store an error before the handler is initialized. */
  @Nullable
  private Throwable failure;

  private final DefaultCallback timeoutCallback = new DefaultCallback();

  private final ErrorCallback errorCallback = new ErrorCallback();

  private final DefaultCallback completionCallback = new DefaultCallback();

  /**
   * Create a new ResponseBodyEmitter instance.
   */
  public ResponseBodyEmitter() {
    this.timeout = null;
  }

  /**
   * Create a ResponseBodyEmitter with a custom timeout value.
   * <p>By default not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeout the timeout value in milliseconds
   */
  public ResponseBodyEmitter(Long timeout) {
    this.timeout = timeout;
  }

  /**
   * Return the configured timeout value, if any.
   */
  @Nullable
  public Long getTimeout() {
    return this.timeout;
  }

  synchronized void initialize(Handler handler) throws IOException {
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

  synchronized void initializeWithError(Throwable ex) {
    this.complete = true;
    this.failure = ex;
    this.earlySendAttempts.clear();
    this.errorCallback.accept(ex);
  }

  /**
   * Invoked after the response is updated with the status code and headers,
   * if the ResponseBodyEmitter is wrapped in a ResponseEntity, but before the
   * response is committed, i.e. before the response body has been written to.
   * <p>The default implementation is empty.
   */
  protected void extendResponse(RequestContext outputMessage) {

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
  public synchronized void send(Object object, @Nullable MediaType mediaType) throws IOException {
    if (complete) {
      throw new IllegalStateException("ResponseBodyEmitter has already completed%s"
              .formatted(this.failure != null ? " with error: " + this.failure : ""));
    }
    if (this.handler != null) {
      try {
        this.handler.send(object, mediaType);
      }
      catch (IOException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to send " + object, ex);
      }
    }
    else {
      this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
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
  public synchronized void send(Collection<DataWithMediaType> items) throws IOException {
    if (complete) {
      throw new IllegalStateException("ResponseBodyEmitter has already completed%s"
              .formatted(this.failure != null ? " with error: " + this.failure : ""));
    }
    sendInternal(items);
  }

  private void sendInternal(Collection<DataWithMediaType> items) throws IOException {
    if (items.isEmpty()) {
      return;
    }
    if (this.handler != null) {
      try {
        this.handler.send(items);
      }
      catch (IOException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to send " + items, ex);
      }
    }
    else {
      this.earlySendAttempts.addAll(items);
    }
  }

  /**
   * Complete request processing by performing a dispatch into the servlet
   * container, where Web MVC is invoked once more, and completes the
   * request processing lifecycle.
   * <p><strong>Note:</strong> this method should be called by the application
   * to complete request processing. It should not be used after container
   * related events such as an error while {@link #send(Object) sending}.
   */
  public synchronized void complete() {
    this.complete = true;
    if (this.handler != null) {
      this.handler.complete();
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
  public synchronized void completeWithError(Throwable ex) {
    this.complete = true;
    this.failure = ex;
    if (this.handler != null) {
      this.handler.completeWithError(ex);
    }
  }

  /**
   * Register code to invoke when the async request times out. This method is
   * called from a container thread when an async request times out.
   */
  public synchronized void onTimeout(Runnable callback) {
    this.timeoutCallback.setDelegate(callback);
  }

  /**
   * Register code to invoke for an error during async request processing.
   * This method is called from a container thread when an error occurred
   * while processing an async request.
   */
  public synchronized void onError(Consumer<Throwable> callback) {
    this.errorCallback.setDelegate(callback);
  }

  /**
   * Register code to invoke when the async request completes. This method is
   * called from a container thread when an async request completed for any
   * reason including timeout and network error. This method is useful for
   * detecting that a {@code ResponseBodyEmitter} instance is no longer usable.
   */
  public synchronized void onCompletion(Runnable callback) {
    this.completionCallback.setDelegate(callback);
  }

  @Override
  public String toString() {
    return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
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

  private class DefaultCallback implements Runnable {

    @Nullable
    private Runnable delegate;

    public void setDelegate(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      complete = true;
      if (delegate != null) {
        delegate.run();
      }
    }
  }

  private class ErrorCallback implements Consumer<Throwable> {

    @Nullable
    private Consumer<Throwable> delegate;

    public void setDelegate(Consumer<Throwable> callback) {
      this.delegate = callback;
    }

    @Override
    public void accept(Throwable t) {
      complete = true;
      if (this.delegate != null) {
        this.delegate.accept(t);
      }
    }
  }

}
