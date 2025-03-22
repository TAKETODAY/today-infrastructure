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

package infra.core.io.buffer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:26
 */
class SubscriberInputStreamTests {

  @Test
  void readSingleByteFromBuffer() throws IOException {
    var stream = new SubscriberInputStream(1);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 42 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer);

    assertThat(stream.read()).isEqualTo(42);
  }

  @Test
  void readMultipleBytesFromBuffer() throws IOException {
    var stream = new SubscriberInputStream(1);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1, 2, 3 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer);

    byte[] bytes = new byte[3];
    assertThat(stream.read(bytes)).isEqualTo(3);
    assertThat(bytes).containsExactly(1, 2, 3);
  }

  @Test
  void readReturnsMinusOneAfterComplete() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());
    stream.onComplete();

    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void readThrowsAfterError() {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());
    stream.onError(new RuntimeException("boom"));

    assertThatThrownBy(() -> stream.read())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");
  }

  @Test
  void closeReleasesBuffers() throws IOException {
    var stream = new SubscriberInputStream(2);
    var buffer1 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    var buffer2 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 2 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer1);
    stream.onNext(buffer2);

    stream.close();

    assertThat(buffer1.isAllocated()).isFalse();
    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  void readAfterCloseReturnsMinusOne() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());
    stream.close();

    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void readEmptyArrayReturnsZero() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());

    assertThat(stream.read(new byte[0])).isZero();
  }

  @Test
  void invalidOffsetOrLengthThrowsException() {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());
    byte[] bytes = new byte[10];

    assertThatThrownBy(() -> stream.read(bytes, -1, 5))
            .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> stream.read(bytes, 0, 11))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void requestMoreDataWhenBufferLimitReached() throws IOException {
    var stream = new SubscriberInputStream(4);
    var counter = new AtomicInteger();
    var subscription = new TestSubscription() {
      @Override
      public void request(long n) {
        counter.incrementAndGet();
      }
    };

    stream.onSubscribe(subscription);
    stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 }));
    stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 2 }));
    stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 3 }));

    byte[] bytes = new byte[3];
    stream.read(bytes);

    assertThat(counter.get()).isEqualTo(2); // Initial request + limit reached
  }

  @Test
  void multipleSubscriptionsNotAllowed() {
    var stream = new SubscriberInputStream(1);
    var subscription1 = new TestSubscription();
    var subscription2 = new TestSubscription();

    stream.onSubscribe(subscription1);
    stream.onSubscribe(subscription2);

    assertThat(subscription1.isCancelled()).isFalse();
    assertThat(subscription2.isCancelled()).isTrue();
  }

  @Test
  void onNextAfterDoneDiscardsBuffer() {
    var stream = new SubscriberInputStream(1);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    stream.onSubscribe(new TestSubscription());
    stream.onComplete();
    stream.onNext(buffer);

    assertThat(buffer.isAllocated()).isFalse();
  }

  @Test
  void bufferOverflowErrorDiscardsBuffer() {
    var stream = new SubscriberInputStream(1);
    var buffer1 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    var buffer2 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 2 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer1);
    stream.onNext(buffer2);

    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  void readPartialBufferContent() throws IOException {
    var stream = new SubscriberInputStream(1);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1, 2, 3 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer);

    byte[] bytes = new byte[2];
    assertThat(stream.read(bytes)).isEqualTo(2);
    assertThat(bytes).containsExactly(1, 2);
    assertThat(stream.read()).isEqualTo(3);
  }

  @Test
  void readingAcrossMultipleBuffersWithPartialReads() throws IOException {
    var stream = new SubscriberInputStream(3);
    var buffer1 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1, 2 });
    var buffer2 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 3, 4 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer1);
    stream.onNext(buffer2);

    byte[] bytes = new byte[3];
    assertThat(stream.read(bytes)).isEqualTo(3);
    assertThat(bytes).containsExactly(1, 2, 3);
    assertThat(stream.read()).isEqualTo(4);
  }

  @Test
  void readingFromEmptyStreamBeforeOnNextWaits() throws IOException {
    var stream = new SubscriberInputStream(1);
    var subscription = new TestSubscription();
    stream.onSubscribe(subscription);

    var thread = new Thread(() -> {
      try {
        Thread.sleep(100);
        stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 42 }));
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    thread.start();

    assertThat(stream.read()).isEqualTo(42);
  }

  @Test
  void bufferOverflowErrorPropagatesAfterReadingExistingData() throws IOException {
    var stream = new SubscriberInputStream(2);
    var buffer1 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    var buffer2 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 2 });
    var buffer3 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 3 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer1);
    stream.onNext(buffer2);
    stream.onNext(buffer3);

    byte[] bytes = new byte[2];
    assertThat(stream.read(bytes)).isEqualTo(2);
    assertThat(bytes).containsExactly(1, 2);
    assertThatThrownBy(stream::read)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Buffer overflow");
  }

  @Test
  void readingWithZeroLengthBufferAfterComplete() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());
    stream.onComplete();

    byte[] bytes = new byte[0];
    assertThat(stream.read(bytes)).isZero();
  }

  @Test
  void readThrowsAfterErrorEvenWithRemainingData() throws IOException {
    var stream = new SubscriberInputStream(2);
    stream.onSubscribe(new TestSubscription());
    stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 }));
    var error = new RuntimeException("test error");
    stream.onError(error);

    byte[] bytes = new byte[1];
    assertThat(stream.read(bytes)).isEqualTo(1);
    assertThatThrownBy(stream::read)
            .isSameAs(error);
  }

  @Test
  void onNextAfterErrorDiscardsBuffer() {
    var stream = new SubscriberInputStream(1);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    stream.onSubscribe(new TestSubscription());
    stream.onError(new RuntimeException());
    stream.onNext(buffer);

    assertThat(buffer.isAllocated()).isFalse();
  }

  @Test
  void readWhileStreamBeingClosedOnAnotherThread() throws IOException {
    var stream = new SubscriberInputStream(2);
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1, 2, 3 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer);

    var thread = new Thread(() -> {
      try {
        Thread.sleep(100);
        stream.close();
      }
      catch (Exception e) {
        Thread.currentThread().interrupt();
      }
    });
    thread.start();

    byte[] bytes = new byte[1];
    assertThat(stream.read(bytes)).isEqualTo(1);
    assertThat(stream.read()).isEqualTo(2);
    assertThat(stream.read()).isEqualTo(3);
    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void interruptedThreadDuringReadingBlocks() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());

    var thread = new Thread(() -> {
      try {
        Thread.sleep(100);
        Thread.currentThread().interrupt();
        stream.onNext(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 }));
      }
      catch (InterruptedException e) {
        // expected
      }
    });
    thread.start();

    assertThat(stream.read()).isEqualTo(1);
  }

  @Test
  void readingFromClosedStreamAfterBufferOverflow() throws IOException {
    var stream = new SubscriberInputStream(1);
    var buffer1 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1 });
    var buffer2 = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 2 });
    stream.onSubscribe(new TestSubscription());
    stream.onNext(buffer1);
    stream.onNext(buffer2);
    stream.close();

    assertThat(stream.read()).isEqualTo(-1);
    assertThat(buffer1.isAllocated()).isFalse();
    assertThat(buffer2.isAllocated()).isFalse();
  }

  @Test
  @Disabled
  void concurrentReadingFromDifferentThreadsNotAllowed() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());

    var thread = new Thread(() -> {
      try {
        stream.read();
      }
      catch (IOException e) {
        // expected
      }
    });
    thread.start();

    assertThatThrownBy(stream::read)
            .isInstanceOf(ConcurrentModificationException.class);
  }

  @Test
  void readingFromStreamWithZeroCapacityThrowsException() {
    var stream = new SubscriberInputStream(0);
    stream.onSubscribe(new TestSubscription());

    assertThatThrownBy(stream::read)
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void readingFromMultipleThreadsWithSharedBuffer() throws IOException, InterruptedException {
    var stream = new SubscriberInputStream(2);
    stream.onSubscribe(new TestSubscription());
    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 1, 2, 3, 4 });
    stream.onNext(buffer);

    var latch = new CountDownLatch(2);
    var results = new AtomicInteger[2];

    for (int i = 0; i < 2; i++) {
      final int index = i;
      new Thread(() -> {
        try {
          results[index] = new AtomicInteger(stream.read());
          latch.countDown();
        }
        catch (IOException e) {
          // expected
        }
      }).start();
    }

    latch.await(1, TimeUnit.SECONDS);
    assertThat(results[0].get() + results[1].get()).isEqualTo(-1);
  }

  @Test
  void onCompleteWhileReadingFromEmptyStream() throws IOException {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());

    var thread = new Thread(() -> {
      try {
        Thread.sleep(100);
        stream.onComplete();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    thread.start();

    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void readingWithNullByteArrayThrowsException() {
    var stream = new SubscriberInputStream(1);
    stream.onSubscribe(new TestSubscription());

    assertThatThrownBy(() -> stream.read(null, 0, 1))
            .isInstanceOf(NullPointerException.class);
  }

  private static class TestSubscription implements Subscription {
    private boolean cancelled;

    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
      cancelled = true;
    }

    boolean isCancelled() {
      return cancelled;
    }
  }

}