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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;
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
public final class PublisherFuture<T extends @Nullable Object> extends AbstractFuture<T> implements Subscriber<T> {

  private final AtomicReference<@Nullable Subscription> ref = new AtomicReference<>();

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
