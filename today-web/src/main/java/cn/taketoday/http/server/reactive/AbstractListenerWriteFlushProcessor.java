/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;

/**
 * An alternative to {@link AbstractListenerWriteProcessor} but instead writing
 * a {@code Publisher<Publisher<T>>} with flush boundaries enforces after
 * the completion of each nested Publisher.
 *
 * @param <T> the type of element signaled to the {@link Subscriber}
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractListenerWriteFlushProcessor<T> implements Processor<Publisher<? extends T>, Void> {

  /**
   * Special logger for debugging Reactive Streams signals.
   *
   * @see LogDelegateFactory#getHiddenLog(Class)
   * @see AbstractListenerReadPublisher#rsReadLogger
   * @see AbstractListenerWriteProcessor#rsWriteLogger
   * @see WriteResultPublisher#rsWriteResultLogger
   */
  protected static final Logger rsWriteFlushLogger =
          LogDelegateFactory.getHiddenLog(AbstractListenerWriteFlushProcessor.class);

  private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

  @Nullable
  private Subscription subscription;

  private volatile boolean sourceCompleted;

  @Nullable
  private volatile AbstractListenerWriteProcessor<?> currentWriteProcessor;

  private final WriteResultPublisher resultPublisher;

  private final String logPrefix;

  public AbstractListenerWriteFlushProcessor() {
    this("");
  }

  /**
   * Create an instance with the given log prefix.
   */
  public AbstractListenerWriteFlushProcessor(String logPrefix) {
    this.logPrefix = logPrefix;
    this.resultPublisher = new WriteResultPublisher(
            logPrefix + "[WFP] ",
            () -> {
              cancel();
              // Complete immediately
              State oldState = this.state.getAndSet(State.COMPLETED);
              if (rsWriteFlushLogger.isTraceEnabled()) {
                rsWriteFlushLogger.trace("{}{} -> {}", getLogPrefix(), oldState, this.state);
              }
              // Propagate to current "write" Processor
              AbstractListenerWriteProcessor<?> writeProcessor = this.currentWriteProcessor;
              if (writeProcessor != null) {
                writeProcessor.cancelAndSetCompleted();
              }
              this.currentWriteProcessor = null;
            });
  }

  /**
   * Create an instance with the given log prefix.
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
  public final void onNext(Publisher<? extends T> publisher) {
    if (rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}onNext: \"write\" Publisher", getLogPrefix());
    }
    this.state.get().onNext(this, publisher);
  }

  /**
   * Error signal from the upstream, write Publisher. This is also used by
   * sub-classes to delegate error notifications from the container.
   */
  @Override
  public final void onError(Throwable ex) {
    State state = this.state.get();
    if (rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}onError: {} [{}]", getLogPrefix(), ex.toString(), state);
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
    if (rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}onComplete [{}]", getLogPrefix(), state);
    }
    state.onComplete(this);
  }

  /**
   * Invoked when flushing is possible, either in the same thread after a check
   * via {@link #isWritePossible()}, or as a callback from the underlying
   * container.
   */
  protected final void onFlushPossible() {
    this.state.get().onFlushPossible(this);
  }

  /**
   * Cancel the upstream chain of "write" Publishers only, for example due to
   * Servlet container error/completion notifications. This should usually
   * be followed up with a call to either {@link #onError(Throwable)} or
   * {@link #onComplete()} to notify the downstream chain, that is unless
   * cancellation came from downstream.
   */
  protected void cancel() {
    if (rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}cancel [{}]", getLogPrefix(), state);
    }
    if (this.subscription != null) {
      this.subscription.cancel();
    }
  }

  // Publisher implementation for result notifications...

  @Override
  public final void subscribe(Subscriber<? super Void> subscriber) {
    this.resultPublisher.subscribe(subscriber);
  }

  // Write API methods to be implemented or template methods to override...

  /**
   * Create a new processor for the current flush boundary.
   */
  protected abstract Processor<? super T, Void> createWriteProcessor();

  /**
   * Whether writing/flushing is possible.
   */
  protected abstract boolean isWritePossible();

  /**
   * Flush the output if ready, or otherwise {@link #isFlushPending()} should
   * return true after.
   * <p>This is primarily for the Servlet non-blocking I/O API where flush
   * cannot be called without a readyToWrite check.
   */
  protected abstract void flush() throws IOException;

  /**
   * Whether flushing is pending.
   * <p>This is primarily for the Servlet non-blocking I/O API where flush
   * cannot be called without a readyToWrite check.
   */
  protected abstract boolean isFlushPending();

  /**
   * Invoked when an error happens while flushing.
   * <p>The default implementation cancels the upstream write publisher and
   * sends an onError downstream as the result of request handling.
   */
  protected void flushingFailed(Throwable t) {
    cancel();
    onError(t);
  }

  // Private methods for use in State...

  private boolean changeState(State oldState, State newState) {
    boolean result = this.state.compareAndSet(oldState, newState);
    if (result && rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}{} -> {}", getLogPrefix(), oldState, newState);
    }
    return result;
  }

  private void flushIfPossible() {
    boolean result = isWritePossible();
    if (rsWriteFlushLogger.isTraceEnabled()) {
      rsWriteFlushLogger.trace("{}isWritePossible[{}]", getLogPrefix(), result);
    }
    if (result) {
      onFlushPossible();
    }
  }

  /**
   * Represents a state for the {@link Processor} to be in.
   *
   * <p><pre>{@code
   *       UNSUBSCRIBED
   *            |
   *            v
   *        REQUESTED <---> RECEIVED ------+
   *            |              |           |
   *            |              v           |
   *            |           FLUSHING       |
   *            |              |           |
   *            |              v           |
   *            +--------> COMPLETED <-----+
   * }</pre>
   */
  private enum State {

    UNSUBSCRIBED {
      @Override
      public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> processor, Subscription subscription) {
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
      public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        // This can happen on (very early) completion notification from container..
        if (processor.changeState(this, COMPLETED)) {
          processor.resultPublisher.publishComplete();
        }
        else {
          processor.state.get().onComplete(processor);
        }
      }
    },

    REQUESTED {
      @Override
      public <T> void onNext(
              AbstractListenerWriteFlushProcessor<T> processor,
              Publisher<? extends T> currentPublisher) {

        if (processor.changeState(this, RECEIVED)) {
          Processor<? super T, Void> writeProcessor = processor.createWriteProcessor();
          processor.currentWriteProcessor = (AbstractListenerWriteProcessor<?>) writeProcessor;
          currentPublisher.subscribe(writeProcessor);
          writeProcessor.subscribe(new WriteResultSubscriber(processor));
        }
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        if (processor.changeState(this, COMPLETED)) {
          processor.resultPublisher.publishComplete();
        }
        else {
          processor.state.get().onComplete(processor);
        }
      }
    },

    RECEIVED {
      @Override
      public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        try {
          processor.flush();
        }
        catch (Throwable ex) {
          processor.flushingFailed(ex);
          return;
        }
        if (processor.changeState(this, REQUESTED)) {
          if (processor.sourceCompleted) {
            handleSourceCompleted(processor);
          }
          else {
            Assert.state(processor.subscription != null, "No subscription");
            processor.subscription.request(1);
          }
        }
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        processor.sourceCompleted = true;
        // A competing write might have completed very quickly
        if (processor.state.get() == State.REQUESTED) {
          handleSourceCompleted(processor);
        }
      }

      private <T> void handleSourceCompleted(AbstractListenerWriteFlushProcessor<T> processor) {
        if (processor.isFlushPending()) {
          // Ensure the final flush
          processor.changeState(State.REQUESTED, State.FLUSHING);
          processor.flushIfPossible();
        }
        else if (processor.changeState(State.REQUESTED, State.COMPLETED)) {
          processor.resultPublisher.publishComplete();
        }
        else {
          processor.state.get().onComplete(processor);
        }
      }
    },

    FLUSHING {
      @Override
      public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
        try {
          processor.flush();
        }
        catch (Throwable ex) {
          processor.flushingFailed(ex);
          return;
        }
        if (processor.changeState(this, COMPLETED)) {
          processor.resultPublisher.publishComplete();
        }
        else {
          processor.state.get().onComplete(processor);
        }
      }

      @Override
      public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
        // ignore
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        // ignore
      }
    },

    COMPLETED {
      @Override
      public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
        // ignore
      }

      @Override
      public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable t) {
        // ignore
      }

      @Override
      public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
        // ignore
      }
    };

    public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> proc, Subscription subscription) {
      subscription.cancel();
    }

    public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
      throw new IllegalStateException(toString());
    }

    public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable ex) {
      if (processor.changeState(this, COMPLETED)) {
        processor.resultPublisher.publishError(ex);
      }
      else {
        processor.state.get().onError(processor, ex);
      }
    }

    public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
      throw new IllegalStateException(toString());
    }

    public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
      throw new IllegalStateException(toString());
    }

    public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
      // ignore
    }

    /**
     * Subscriber to receive and delegate completion notifications for from
     * the current Publisher, i.e. for the current flush boundary.
     */
    private record WriteResultSubscriber(AbstractListenerWriteFlushProcessor<?> processor)
            implements Subscriber<Void> {

      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(Void aVoid) { }

      @Override
      public void onError(Throwable ex) {
        if (rsWriteFlushLogger.isTraceEnabled()) {
          rsWriteFlushLogger.trace(
                  "{}current \"write\" Publisher failed: {}", this.processor.getLogPrefix(), ex);
        }
        this.processor.currentWriteProcessor = null;
        this.processor.cancel();
        this.processor.onError(ex);
      }

      @Override
      public void onComplete() {
        if (rsWriteFlushLogger.isTraceEnabled()) {
          rsWriteFlushLogger.trace(
                  "{}current \"write\" Publisher completed", this.processor.getLogPrefix());
        }
        this.processor.currentWriteProcessor = null;
        this.processor.state.get().writeComplete(this.processor);
      }

      @Override
      public String toString() {
        return this.processor.getClass().getSimpleName() + "-WriteResultSubscriber";
      }
    }
  }

}
