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

package infra.util.concurrent;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import infra.lang.Assert;

/**
 * Publisher future adapter
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/13 21:13
 */
public final class PublisherFuture<T> extends AbstractFuture<T> implements Subscriber<T> {

  private final AtomicReference<Subscription> ref = new AtomicReference<>();

  PublisherFuture(Publisher<T> publisher) {
    super(null);
    publisher.subscribe(this);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean cancelled = super.cancel(mayInterruptIfRunning);
    if (cancelled) {
      Subscription s = ref.getAndSet(null);
      if (s != null) {
        s.cancel();
      }
    }
    return cancelled;
  }

  @Override
  public void onSubscribe(Subscription s) {
    Objects.requireNonNull(s, "Subscription is required");
    if (ref.getAndSet(s) == null) {
      s.request(Long.MAX_VALUE);
    }
    else {
      s.cancel();
    }
  }

  @Override
  public void onNext(T t) {
    Subscription s = ref.getAndSet(null);
    if (s != null) {
      trySuccess(t);
    }
  }

  @Override
  public void onError(Throwable t) {
    if (ref.getAndSet(null) != null) {
      tryFailure(t);
    }
  }

  @Override
  public void onComplete() {
    if (ref.getAndSet(null) != null) {
      trySuccess(null);
    }
  }

  public static <T> PublisherFuture<T> of(Publisher<T> publisher) {
    Assert.notNull(publisher, "Publisher is required");
    return new PublisherFuture<>(publisher);
  }

}
