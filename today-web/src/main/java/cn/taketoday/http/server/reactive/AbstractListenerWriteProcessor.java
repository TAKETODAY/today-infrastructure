/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for {@code Processor} implementations that bridge between
 * event-listener write APIs and Reactive Streams.
 *
 * <p>Specifically a base class for writing to the HTTP response body with
 * Servlet 3.1 non-blocking I/O and Undertow XNIO as well for writing WebSocket
 * messages through the Java WebSocket API (JSR-356), Jetty, and Undertow.
 *
 * @param <T> the type of element signaled to the {@link Subscriber}
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

  /**
   * Special logger for debugging Reactive Streams signals.
   *
   * @see LogDelegateFactory#getHiddenLog(Class)
   * @see AbstractListenerReadPublisher#rsReadLogger
   * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
   * @see WriteResultPublisher#rsWriteResultLogger
   */
  protected static final Logger rsWriteLogger = LogDelegateFactory.getHiddenLog(AbstractListenerWriteProcessor.class);

  private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

  @Nullable
  private Subscription subscription;

  @Nullable
  private volatile T currentData;

  /* Indicates "onComplete" was received during the (last) write. */
  private volatile boolean sourceCompleted;

  /**
   * Indicates we're waiting for one last isReady-onWritePossible cycle
   * after "onComplete" because some Servlet containers expect this to take
   * place prior to calling AsyncContext.complete().
   * See https://github.com/eclipse-ee4j/servlet-api/issues/273
   */
  private volatile boolean readyToCompleteAfterLastWrite;

  private final WriteResultPublisher resultPublisher;

  private final String logPrefix;

  public AbstractListenerWriteProcessor() {
    this("");
  }

  /**
   * Create an instance with the given log prefix.
   */
  public AbstractListenerWriteProcessor(String logPrefix) {
    // AbstractListenerFlushProcessor calls cancelAndSetCompleted directly, so this cancel task
    // won't be used for HTTP responses, but it can be for a WebSocket session.
    this.resultPublisher = new WriteResultPublisher(logPrefix + "[WP] ", this::cancelAndSetCompleted);
    this.logPrefix = StringUtils.hasText(logPrefix) ? logPrefix : "";
  }

  /**
   * Get the configured log prefix.
   */
  public String getLogPrefix() {
    return this.logPrefix;
  }

  // Subscriber methods and async I/O notification methods...

  @Override
  public final void onSubscribe(Subscription subscription) {
    this.state.get().onSubscribe(this, subscription);
  }

  @Override
  public final void onNext(T data) {
    if (rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace("{}onNext: {}", getLogPrefix(), data.getClass().getSimpleName());
    }
    this.state.get().onNext(this, data);
  }

  /**
   * Error signal from the upstream, write Publisher. This is also used by
   * sub-classes to delegate error notifications from the container.
   */
  @Override
  public final void onError(Throwable ex) {
    State state = this.state.get();
    if (rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace("{}onError: {} [{}]", getLogPrefix(), ex.toString(), state);
    }
    state.onError(this, ex);
  }

  /**
   * Completion signal from the upstream, write Publisher. This is also used
   * by sub-classes to delegate completion notifications from the container.
   */
  @Override
  public final void onComplete() {
    State state = this.state.get();
    if (rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace("{}onComplete [{}]", getLogPrefix(), state);
    }
    state.onComplete(this);
  }

  /**
   * Invoked when writing is possible, either in the same thread after a check
   * via {@link #isWritePossible()}, or as a callback from the underlying
   * container.
   */
  public final void onWritePossible() {
    State state = this.state.get();
    if (rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace("{}onWritePossible [{}]", getLogPrefix(), state);
    }
    state.onWritePossible(this);
  }

  /**
   * Cancel the upstream "write" Publisher only, for example due to
   * Servlet container error/completion notifications. This should usually
   * be followed up with a call to either {@link #onError(Throwable)} or
   * {@link #onComplete()} to notify the downstream chain, that is unless
   * cancellation came from downstream.
   */
  public void cancel() {
    if (rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace("{}cancel [{}]", getLogPrefix(), state);
    }
    if (this.subscription != null) {
      this.subscription.cancel();
    }
  }

  /**
   * Cancel the "write" Publisher and transition to COMPLETED immediately also
   * without notifying the downstream. For use when cancellation came from
   * downstream.
   */
  void cancelAndSetCompleted() {
    cancel();
    while (true) {
      State prev = this.state.get();
      if (prev == State.COMPLETED) {
        break;
      }
      if (this.state.compareAndSet(prev, State.COMPLETED)) {
        if (rsWriteLogger.isTraceEnabled()) {
          rsWriteLogger.trace("{}{} -> {}", getLogPrefix(), prev, this.state);
        }
        if (prev != State.WRITING) {
          discardCurrentData();
        }
        break;
      }
    }
  }

  // Publisher implementation for result notifications...

  @Override
  public final void subscribe(Subscriber<? super Void> subscriber) {
    this.resultPublisher.subscribe(subscriber);
  }

  // Write API methods to be implemented or template methods to override...

  /**
   * Whether the given data item has any content to write.
   * If false the item is not written.
   */
  protected abstract boolean isDataEmpty(T data);

  /**
   * Template method invoked after a data item to write is received via
   * {@link Subscriber#onNext(Object)}. The default implementation saves the
   * data item for writing once that is possible.
   */
  protected void dataReceived(T data) {
    T prev = this.currentData;
    if (prev != null) {
      // This shouldn't happen:
      //   1. dataReceived can only be called from REQUESTED state
      //   2. currentData is cleared before requesting
      discardData(data);
      cancel();
      onError(new IllegalStateException("Received new data while current not processed yet."));
    }
    this.currentData = data;
  }

  /**
   * Whether writing is possible.
   */
  protected abstract boolean isWritePossible();

  /**
   * Write the given item.
   * <p><strong>Note:</strong> Sub-classes are responsible for releasing any
   * data buffer associated with the item, once fully written, if pooled
   * buffers apply to the underlying container.
   *
   * @param data the item to write
   * @return {@code true} if the current data item was written completely and
   * a new item requested, or {@code false} if it was written partially and
   * we'll need more write callbacks before it is fully written
   */
  protected abstract boolean write(T data) throws IOException;

  /**
   * Invoked after the current data has been written and before requesting
   * the next item from the upstream, write Publisher.
   * <p>The default implementation is a no-op.
   */
  protected void writingPaused() { }

  /**
   * Invoked after onComplete or onError notification.
   * <p>The default implementation is a no-op.
   */
  protected void writingComplete() { }

  /**
   * Invoked when an I/O error occurs during a write. Sub-classes may choose
   * to ignore this if they know the underlying API will provide an error
   * notification in a container thread.
   * <p>Defaults to no-op.
   */
  protected void writingFailed(Throwable ex) { }

  /**
   * Invoked after any error (either from the upstream write Publisher, or
   * from I/O operations to the underlying server) and cancellation
   * to discard in-flight data that was in
   * the process of being written when the error took place.
   *
   * @param data the data to be released
   * @since 4.0
   */
  protected abstract void discardData(T data);

  // Private methods for use from State's...

  private boolean changeState(State oldState, State newState) {
    boolean result = this.state.compareAndSet(oldState, newState);
    if (result && rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace(getLogPrefix() + oldState + " -> " + newState);
    }
    return result;
  }

  private void changeStateToReceived(State oldState) {
    if (changeState(oldState, State.RECEIVED)) {
      writeIfPossible();
    }
  }

  private void changeStateToComplete(State oldState) {
    if (changeState(oldState, State.COMPLETED)) {
      discardCurrentData();
      writingComplete();
      this.resultPublisher.publishComplete();
    }
    else {
      this.state.get().onComplete(this);
    }
  }

  private void writeIfPossible() {
    boolean result = isWritePossible();
    if (!result && rsWriteLogger.isTraceEnabled()) {
      rsWriteLogger.trace(getLogPrefix() + "isWritePossible false");
    }
    if (result) {
      onWritePossible();
    }
  }

  private void discardCurrentData() {
    T data = this.currentData;
    this.currentData = null;
    if (data != null) {
      discardData(data);
    }
  }

  /**
   * Represents a state for the {@link Processor} to be in.
   *
   * <p><pre>
   *        UNSUBSCRIBED
   *             |
   *             v
   *   +--- REQUESTED -------------> RECEIVED ---+
   *   |        ^                       ^        |
   *   |        |                       |        |
   *   |        + ------ WRITING <------+        |
   *   |                    |                    |
   *   |                    v                    |
   *   +--------------> COMPLETED <--------------+
   * </pre>
   */
  private enum State {

    UNSUBSCRIBED {
      @Override
      public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
        Assert.notNull(subscription, "Subscription must not be null");
        if (processor.changeState(this, REQUESTED)) {
          processor.subscription = subscription;
          subscription.request(1);
        }
        else {
          super.onSubscribe(processor, subscription);
        }
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
        // This can happen on (very early) completion notification from container..
        processor.changeStateToComplete(this);
      }
    },

    REQUESTED {
      @Override
      public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
        if (processor.isDataEmpty(data)) {
          Assert.state(processor.subscription != null, "No subscription");
          processor.subscription.request(1);
        }
        else {
          processor.dataReceived(data);
          processor.changeStateToReceived(this);
        }
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
        processor.readyToCompleteAfterLastWrite = true;
        processor.changeStateToReceived(this);
      }
    },

    RECEIVED {
      @SuppressWarnings("deprecation")
      @Override
      public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
        if (processor.readyToCompleteAfterLastWrite) {
          processor.changeStateToComplete(RECEIVED);
        }
        else if (processor.changeState(this, WRITING)) {
          T data = processor.currentData;
          Assert.state(data != null, "No data");
          try {
            if (processor.write(data)) {
              if (processor.changeState(WRITING, REQUESTED)) {
                processor.currentData = null;
                if (processor.sourceCompleted) {
                  processor.readyToCompleteAfterLastWrite = true;
                  processor.changeStateToReceived(REQUESTED);
                }
                else {
                  processor.writingPaused();
                  Assert.state(processor.subscription != null, "No subscription");
                  processor.subscription.request(1);
                }
              }
            }
            else {
              processor.changeStateToReceived(WRITING);
            }
          }
          catch (IOException ex) {
            processor.writingFailed(ex);
          }
        }
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
        processor.sourceCompleted = true;
        // A competing write might have completed very quickly
        if (processor.state.get() == State.REQUESTED) {
          processor.changeStateToComplete(State.REQUESTED);
        }
      }
    },

    WRITING {
      @Override
      public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
        processor.sourceCompleted = true;
        // A competing write might have completed very quickly
        if (processor.state.get() == State.REQUESTED) {
          processor.changeStateToComplete(State.REQUESTED);
        }
      }
    },

    COMPLETED {
      @Override
      public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
        // ignore
      }

      @Override
      public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
        // ignore
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
        // ignore
      }
    };

    public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
      subscription.cancel();
    }

    public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
      processor.discardData(data);
      processor.cancel();
      processor.onError(new IllegalStateException("Illegal onNext without demand"));
    }

    public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
      if (processor.changeState(this, COMPLETED)) {
        processor.discardCurrentData();
        processor.writingComplete();
        processor.resultPublisher.publishError(ex);
      }
      else {
        processor.state.get().onError(processor, ex);
      }
    }

    public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
      throw new IllegalStateException(toString());
    }

    public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
      // ignore
    }
  }

}
