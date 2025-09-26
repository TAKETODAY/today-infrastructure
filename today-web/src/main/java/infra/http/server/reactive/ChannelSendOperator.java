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

package infra.http.server.reactive;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

import infra.core.io.buffer.DataBuffer;
import infra.lang.Assert;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * Given a write function that accepts a source {@code Publisher<T>} to write
 * with and returns {@code Publisher<Void>} for the result, this operator helps
 * to defer the invocation of the write function, until we know if the source
 * publisher will begin publishing without an error. If the first emission is
 * an error, the write function is bypassed, and the error is sent directly
 * through the result publisher. Otherwise the write function is invoked.
 *
 * @param <T> the type of element signaled
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ChannelSendOperator<T> extends Mono<Void> implements Scannable {

  private final Flux<T> source;

  private final Function<Publisher<T>, Publisher<Void>> writeFunction;

  public ChannelSendOperator(Publisher<? extends T> source, Function<Publisher<T>, Publisher<Void>> writeFunction) {
    this.source = Flux.from(source);
    this.writeFunction = writeFunction;
  }

  @Override
  @Nullable
  public Object scanUnsafe(Attr key) {
    if (key == Attr.PREFETCH) {
      return Integer.MAX_VALUE;
    }
    if (key == Attr.PARENT) {
      return this.source;
    }
    return null;
  }

  @Override
  public void subscribe(CoreSubscriber<? super Void> actual) {
    this.source.subscribe(new WriteBarrier(actual));
  }

  private enum State {

    /** No emissions from the upstream source yet. */
    NEW,

    /**
     * At least one signal of any kind has been received; we're ready to
     * call the write function and proceed with actual writing.
     */
    FIRST_SIGNAL_RECEIVED,

    /**
     * The write subscriber has subscribed and requested; we're going to
     * emit the cached signals.
     */
    EMITTING_CACHED_SIGNALS,

    /**
     * The write subscriber has subscribed, and cached signals have been
     * emitted to it; we're ready to switch to a simple pass-through mode
     * for all remaining signals.
     **/
    READY_TO_WRITE

  }

  /**
   * A barrier inserted between the write source and the write subscriber
   * (i.e. the HTTP server adapter) that pre-fetches and waits for the first
   * signal before deciding whether to hook in to the write subscriber.
   *
   * <p>Acts as:
   * <ul>
   * <li>Subscriber to the write source.
   * <li>Subscription to the write subscriber.
   * <li>Publisher to the write subscriber.
   * </ul>
   *
   * <p>Also uses {@link WriteCompletionBarrier} to communicate completion
   * and detect cancel signals from the completion subscriber.
   */
  private class WriteBarrier implements CoreSubscriber<T>, Subscription, Publisher<T> {

    /* Bridges signals to and from the completionSubscriber */
    private final WriteCompletionBarrier writeCompletionBarrier;

    /* Upstream write source subscription */
    @Nullable
    private Subscription subscription;

    /** Cached data item before readyToWrite. */
    @Nullable
    private T item;

    /** Cached error signal before readyToWrite. */
    @Nullable
    private Throwable error;

    /** Cached onComplete signal before readyToWrite. */
    private boolean completed = false;

    /** Recursive demand while emitting cached signals. */
    private long demandBeforeReadyToWrite;

    /** Current state. */
    private State state = State.NEW;

    /** The actual writeSubscriber from the HTTP server adapter. */
    @Nullable
    private Subscriber<? super T> writeSubscriber;

    WriteBarrier(CoreSubscriber<? super Void> completionSubscriber) {
      this.writeCompletionBarrier = new WriteCompletionBarrier(completionSubscriber, this);
    }

    // Subscriber<T> methods (we're the subscriber to the write source)..

    @Override
    public final void onSubscribe(Subscription s) {
      if (Operators.validate(this.subscription, s)) {
        this.subscription = s;
        this.writeCompletionBarrier.connect();
        s.request(1);
      }
    }

    @Override
    public final void onNext(T item) {
      if (this.state == State.READY_TO_WRITE) {
        requiredWriteSubscriber().onNext(item);
        return;
      }
      //FIXME revisit in case of reentrant sync deadlock
      synchronized(this) {
        if (this.state == State.READY_TO_WRITE) {
          requiredWriteSubscriber().onNext(item);
        }
        else if (this.state == State.NEW) {
          this.item = item;
          this.state = State.FIRST_SIGNAL_RECEIVED;
          Publisher<Void> result;
          try {
            result = writeFunction.apply(this);
          }
          catch (Throwable ex) {
            this.writeCompletionBarrier.onError(ex);
            return;
          }
          result.subscribe(this.writeCompletionBarrier);
        }
        else {
          if (this.subscription != null) {
            this.subscription.cancel();
          }
          this.writeCompletionBarrier.onError(new IllegalStateException("Unexpected item."));
        }
      }
    }

    private Subscriber<? super T> requiredWriteSubscriber() {
      Assert.state(this.writeSubscriber != null, "No write subscriber");
      return this.writeSubscriber;
    }

    @Override
    public final void onError(Throwable ex) {
      if (this.state == State.READY_TO_WRITE) {
        requiredWriteSubscriber().onError(ex);
        return;
      }
      synchronized(this) {
        if (this.state == State.READY_TO_WRITE) {
          requiredWriteSubscriber().onError(ex);
        }
        else if (this.state == State.NEW) {
          this.state = State.FIRST_SIGNAL_RECEIVED;
          this.writeCompletionBarrier.onError(ex);
        }
        else {
          this.error = ex;
        }
      }
    }

    @Override
    public final void onComplete() {
      if (this.state == State.READY_TO_WRITE) {
        requiredWriteSubscriber().onComplete();
        return;
      }
      synchronized(this) {
        if (this.state == State.READY_TO_WRITE) {
          requiredWriteSubscriber().onComplete();
        }
        else if (this.state == State.NEW) {
          this.completed = true;
          this.state = State.FIRST_SIGNAL_RECEIVED;
          Publisher<Void> result;
          try {
            result = writeFunction.apply(this);
          }
          catch (Throwable ex) {
            this.writeCompletionBarrier.onError(ex);
            return;
          }
          result.subscribe(this.writeCompletionBarrier);
        }
        else {
          this.completed = true;
        }
      }
    }

    @Override
    public Context currentContext() {
      return this.writeCompletionBarrier.currentContext();
    }

    // Subscription methods (we're the Subscription to the writeSubscriber)..

    @Override
    public void request(long n) {
      Subscription s = this.subscription;
      if (s == null) {
        return;
      }
      if (this.state == State.READY_TO_WRITE) {
        s.request(n);
        return;
      }
      synchronized(this) {
        if (this.writeSubscriber != null) {
          if (this.state == State.EMITTING_CACHED_SIGNALS) {
            this.demandBeforeReadyToWrite = n;
            return;
          }
          try {
            this.state = State.EMITTING_CACHED_SIGNALS;
            if (emitCachedSignals()) {
              return;
            }
            n = n + this.demandBeforeReadyToWrite - 1;
            if (n == 0) {
              return;
            }
          }
          finally {
            this.state = State.READY_TO_WRITE;
          }
        }
      }
      s.request(n);
    }

    private boolean emitCachedSignals() {
      if (this.error != null) {
        try {
          requiredWriteSubscriber().onError(this.error);
        }
        finally {
          releaseCachedItem();
        }
        return true;
      }
      T item = this.item;
      this.item = null;
      if (item != null) {
        requiredWriteSubscriber().onNext(item);
      }
      if (this.completed) {
        requiredWriteSubscriber().onComplete();
        return true;
      }
      return false;
    }

    @Override
    public void cancel() {
      Subscription s = this.subscription;
      if (s != null) {
        this.subscription = null;
        try {
          s.cancel();
        }
        finally {
          releaseCachedItem();
        }
      }
    }

    private void releaseCachedItem() {
      synchronized(this) {
        Object item = this.item;
        if (item instanceof DataBuffer dataBuffer) {
          dataBuffer.release();
        }
        this.item = null;
      }
    }

    // Publisher<T> methods (we're the Publisher to the writeSubscriber)..

    @Override
    public void subscribe(Subscriber<? super T> writeSubscriber) {
      synchronized(this) {
        Assert.state(this.writeSubscriber == null, "Only one write subscriber supported");
        this.writeSubscriber = writeSubscriber;
        if (this.error != null || this.completed) {
          this.writeSubscriber.onSubscribe(Operators.emptySubscription());
          emitCachedSignals();
        }
        else {
          this.writeSubscriber.onSubscribe(this);
        }
      }
    }
  }

  /**
   * We need an extra barrier between the WriteBarrier itself and the actual
   * completion subscriber.
   *
   * <p>The completionSubscriber is subscribed initially to the WriteBarrier.
   * Later after the first signal is received, we need one more subscriber
   * instance (per spec can only subscribe once) to subscribe to the write
   * function and switch to delegating completion signals from it.
   */
  private class WriteCompletionBarrier implements CoreSubscriber<Void>, Subscription {

    /* Downstream write completion subscriber */
    private final CoreSubscriber<? super Void> completionSubscriber;

    private final WriteBarrier writeBarrier;

    @Nullable
    private Subscription subscription;

    public WriteCompletionBarrier(CoreSubscriber<? super Void> subscriber, WriteBarrier writeBarrier) {
      this.completionSubscriber = subscriber;
      this.writeBarrier = writeBarrier;
    }

    /**
     * Connect the underlying completion subscriber to this barrier in order
     * to track cancel signals and pass them on to the write barrier.
     */
    public void connect() {
      this.completionSubscriber.onSubscribe(this);
    }

    // Subscriber methods (we're the subscriber to the write function)..

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Void aVoid) { }

    @Override
    public void onError(Throwable ex) {
      try {
        this.completionSubscriber.onError(ex);
      }
      finally {
        this.writeBarrier.releaseCachedItem();
      }
    }

    @Override
    public void onComplete() {
      this.completionSubscriber.onComplete();
    }

    @Override
    public Context currentContext() {
      return this.completionSubscriber.currentContext();
    }

    @Override
    public void request(long n) {
      // Ignore: we don't produce data
    }

    @Override
    public void cancel() {
      this.writeBarrier.cancel();
      Subscription subscription = this.subscription;
      if (subscription != null) {
        subscription.cancel();
      }
    }
  }

}
