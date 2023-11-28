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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogDelegateFactory;
import cn.taketoday.logging.Logger;
import reactor.core.publisher.Operators;

/**
 * Publisher returned from {@link ServerHttpResponse#writeWith(Publisher)}.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class WriteResultPublisher implements Publisher<Void> {

  /**
   * Special logger for debugging Reactive Streams signals.
   *
   * @see LogDelegateFactory#getHiddenLog(Class)
   * @see AbstractListenerReadPublisher#rsReadLogger
   * @see AbstractListenerWriteProcessor#rsWriteLogger
   * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
   */
  private static final Logger rsWriteResultLogger = LogDelegateFactory.getHiddenLog(WriteResultPublisher.class);

  private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

  private final Runnable cancelTask;

  @Nullable
  private volatile Subscriber<? super Void> subscriber;

  private volatile boolean completedBeforeSubscribed;

  @Nullable
  private volatile Throwable errorBeforeSubscribed;

  private final String logPrefix;

  public WriteResultPublisher(String logPrefix, Runnable cancelTask) {
    this.cancelTask = cancelTask;
    this.logPrefix = logPrefix;
  }

  @Override
  public final void subscribe(Subscriber<? super Void> subscriber) {
    if (rsWriteResultLogger.isTraceEnabled()) {
      rsWriteResultLogger.trace("{}got subscriber {}", this.logPrefix, subscriber);
    }
    this.state.get().subscribe(this, subscriber);
  }

  /**
   * Invoke this to delegate a completion signal to the subscriber.
   */
  public void publishComplete() {
    State state = this.state.get();
    if (rsWriteResultLogger.isTraceEnabled()) {
      rsWriteResultLogger.trace("{}completed [{}]", this.logPrefix, state);
    }
    state.publishComplete(this);
  }

  /**
   * Invoke this to delegate an error signal to the subscriber.
   */
  public void publishError(Throwable t) {
    State state = this.state.get();
    if (rsWriteResultLogger.isTraceEnabled()) {
      rsWriteResultLogger.trace("failed: {} [{}]", this.logPrefix, t, state);
    }
    state.publishError(this, t);
  }

  private boolean changeState(State oldState, State newState) {
    return this.state.compareAndSet(oldState, newState);
  }

  /**
   * Subscription to receive and delegate request and cancel signals from the
   * subscriber to this publisher.
   */
  private record WriteResultSubscription(WriteResultPublisher publisher) implements Subscription {

    @Override
    public void request(long n) {
      if (rsWriteResultLogger.isTraceEnabled()) {
        rsWriteResultLogger.trace(
                "{}request {}", this.publisher.logPrefix, (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
      }
      getState().request(this.publisher, n);
    }

    @Override
    public void cancel() {
      State state = getState();
      if (rsWriteResultLogger.isTraceEnabled()) {
        rsWriteResultLogger.trace("{}cancel [{}]", this.publisher.logPrefix, state);
      }
      state.cancel(this.publisher);
    }

    private State getState() {
      return this.publisher.state.get();
    }
  }

  /**
   * Represents a state for the {@link Publisher} to be in.
   * <p><pre>
   *     UNSUBSCRIBED
   *          |
   *          v
   *     SUBSCRIBING
   *          |
   *          v
   *      SUBSCRIBED
   *          |
   *          v
   *      COMPLETED
   * </pre>
   */
  private enum State {

    UNSUBSCRIBED {
      @Override
      void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
        Assert.notNull(subscriber, "Subscriber is required");
        if (publisher.changeState(this, SUBSCRIBING)) {
          WriteResultSubscription subscription = new WriteResultSubscription(publisher);
          publisher.subscriber = subscriber;
          subscriber.onSubscribe(subscription);
          publisher.changeState(SUBSCRIBING, SUBSCRIBED);
          // Now safe to check "beforeSubscribed" flags, they won't change once in NO_DEMAND
          if (publisher.completedBeforeSubscribed) {
            publisher.state.get().publishComplete(publisher);
          }
          Throwable ex = publisher.errorBeforeSubscribed;
          if (ex != null) {
            publisher.state.get().publishError(publisher, ex);
          }
        }
        else {
          throw new IllegalStateException(toString());
        }
      }

      @Override
      void publishComplete(WriteResultPublisher publisher) {
        publisher.completedBeforeSubscribed = true;
        if (State.SUBSCRIBED == publisher.state.get()) {
          publisher.state.get().publishComplete(publisher);
        }
      }

      @Override
      void publishError(WriteResultPublisher publisher, Throwable ex) {
        publisher.errorBeforeSubscribed = ex;
        if (State.SUBSCRIBED == publisher.state.get()) {
          publisher.state.get().publishError(publisher, ex);
        }
      }
    },

    SUBSCRIBING {
      @Override
      void request(WriteResultPublisher publisher, long n) {
        Operators.validate(n);
      }

      @Override
      void publishComplete(WriteResultPublisher publisher) {
        publisher.completedBeforeSubscribed = true;
        if (State.SUBSCRIBED == publisher.state.get()) {
          publisher.state.get().publishComplete(publisher);
        }
      }

      @Override
      void publishError(WriteResultPublisher publisher, Throwable ex) {
        publisher.errorBeforeSubscribed = ex;
        if (State.SUBSCRIBED == publisher.state.get()) {
          publisher.state.get().publishError(publisher, ex);
        }
      }
    },

    SUBSCRIBED {
      @Override
      void request(WriteResultPublisher publisher, long n) {
        Operators.validate(n);
      }
    },

    COMPLETED {
      @Override
      void request(WriteResultPublisher publisher, long n) {
        // ignore
      }

      @Override
      void cancel(WriteResultPublisher publisher) {
        // ignore
      }

      @Override
      void publishComplete(WriteResultPublisher publisher) {
        // ignore
      }

      @Override
      void publishError(WriteResultPublisher publisher, Throwable t) {
        // ignore
      }
    };

    void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
      throw new IllegalStateException(toString());
    }

    void request(WriteResultPublisher publisher, long n) {
      throw new IllegalStateException(toString());
    }

    void cancel(WriteResultPublisher publisher) {
      if (publisher.changeState(this, COMPLETED)) {
        publisher.cancelTask.run();
      }
      else {
        publisher.state.get().cancel(publisher);
      }
    }

    void publishComplete(WriteResultPublisher publisher) {
      if (publisher.changeState(this, COMPLETED)) {
        Subscriber<? super Void> s = publisher.subscriber;
        Assert.state(s != null, "No subscriber");
        s.onComplete();
      }
      else {
        publisher.state.get().publishComplete(publisher);
      }
    }

    void publishError(WriteResultPublisher publisher, Throwable t) {
      if (publisher.changeState(this, COMPLETED)) {
        Subscriber<? super Void> s = publisher.subscriber;
        Assert.state(s != null, "No subscriber");
        s.onError(t);
      }
      else {
        publisher.state.get().publishError(publisher, t);
      }
    }
  }

}
