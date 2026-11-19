/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/23 10:27
 */
class SubscriberInputStreamTests {

  @Test
  void rejectsNonPositiveDemand() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 0))
            .withMessage("demand must be greater than 0");
  }

  @Test
  void drainsPendingBytesBeforePropagatingAnUpstreamError() throws IOException {
    var failure = new IllegalStateException("upstream failed after data");
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 2)) {
      stream.onSubscribe(mock());
      stream.onNext(FOO);
      stream.onError(failure);

      byte[] result = new byte[FOO.length + 1];
      assertThat(stream.read(result)).isEqualTo(FOO.length);
      assertThat(result).startsWith(FOO);
      assertThatThrownBy(stream::read).isSameAs(failure);
    }
  }

  @Test
  void cancelsADuplicateSubscriptionAndDiscardsDataAfterClose() throws IOException {
    Flow.Subscription first = mock();
    Flow.Subscription duplicate = mock();
    List<byte[]> discarded = new ArrayList<>();
    var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, discarded::add, 1);
    stream.onSubscribe(first);
    stream.onSubscribe(duplicate);
    stream.onNext(FOO);
    stream.close();
    stream.onNext(BAR);

    assertThat(stream.read()).isEqualTo(-1);
    assertThat(stream.read(new byte[1])).isEqualTo(-1);
    assertThat(discarded).containsExactly(FOO, BAR);
    verify(first).request(1);
    verify(duplicate).cancel();
    verify(duplicate, never()).request(1);
  }

  @Test
  void readsBufferedDataBeforeReportingOverflowAndDiscardsSubsequentBuffers() throws IOException {
    Flow.Subscription subscription = mock();
    List<byte[]> discarded = new ArrayList<>();
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, discarded::add, 1)) {
      stream.onSubscribe(subscription);
      stream.onNext(FOO);
      stream.onNext(BAR);
      stream.onNext(BAZ);
      stream.onComplete();
      stream.onError(new IllegalStateException("late error"));

      byte[] result = new byte[8];
      assertThat(stream.read(result, 1, 0)).isZero();
      assertThat(stream.read(result, 1, 7)).isEqualTo(3);
      assertThat(result).containsExactly(0, 'f', 'o', 'o', 0, 0, 0, 0);
      assertThatThrownBy(() -> stream.read(result)).hasMessage("Buffer overflow");
      assertThat(discarded).containsExactly(BAR, BAZ);
      verify(subscription).cancel();
    }
  }

  @Test
  void propagatesUpstreamErrorFromSingleByteRead() throws IOException {
    Flow.Subscription subscription = mock();
    var failure = new IllegalStateException("upstream failed");
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 1)) {
      stream.onSubscribe(subscription);
      stream.onError(failure);
      assertThatThrownBy(stream::read).isSameAs(failure);
      verify(subscription).cancel();
    }
  }

  @Test
  void returnsEndOfStreamAfterCompletionWithoutData() throws IOException {
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 1)) {
      stream.onSubscribe(mock());
      stream.onComplete();
      assertThat(stream.read()).isEqualTo(-1);
    }
  }

  @Test
  void propagatesInterruptionBeforeAnyBytesAreRead() throws IOException {
    Flow.Subscription subscription = mock();
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 1)) {
      stream.onSubscribe(subscription);
      Thread.currentThread().interrupt();
      assertThatIOException().isThrownBy(() -> stream.read(new byte[1]))
              .isInstanceOf(InterruptedIOException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verify(subscription).cancel();
    }
    finally {
      Thread.interrupted();
    }
  }

  @Test
  void rejectsConcurrentReadsButAllowsCloseWhileAnotherReaderOwnsTheStream() throws Exception {
    Flow.Subscription subscription = mock();
    var mapping = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var stream = new SubscriberInputStream<byte[]>(bytes -> {
      mapping.countDown();
      try {
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new AssertionError("Reader was not released");
        }
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError(ex);
      }
      return bytes;
    }, bytes -> { }, 1)) {
      stream.onSubscribe(subscription);
      stream.onNext(FOO);
      var read = new FutureTask<>(() -> stream.read(new byte[FOO.length + 1]));
      Thread reader = new Thread(read, "subscriber-input-stream-reader");
      reader.setDaemon(true);
      reader.start();
      try {
        assertThat(mapping.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(stream::read).isInstanceOf(ConcurrentModificationException.class);
        assertThatThrownBy(() -> stream.read(new byte[1])).isInstanceOf(ConcurrentModificationException.class);
        stream.close();
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.read(new byte[1])).isEqualTo(-1);
      }
      finally {
        release.countDown();
      }
      assertThat(read.get(5, TimeUnit.SECONDS)).isEqualTo(-1);
      verify(subscription).cancel();
    }
  }

  @Test
  void requiresASubscriptionBeforeClosingAndContinuesCleanupWhenDiscardFails() throws IOException {
    var unsubscribed = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> { }, 1);
    assertThatIllegalStateException().isThrownBy(unsubscribed::close).withMessageContaining("must be subscribed");

    List<byte[]> discarded = new ArrayList<>();
    var stream = new SubscriberInputStream<byte[]>(bytes -> bytes, bytes -> {
      discarded.add(bytes);
      if (bytes == FOO) {
        throw new IllegalStateException("discard failed");
      }
    }, 2);
    stream.onSubscribe(mock());
    stream.onNext(FOO);
    stream.onNext(BAR);
    stream.close();
    assertThat(discarded).containsExactly(FOO, BAR);
  }

  @Test
  void interruptWhileAwaitingData() throws InterruptedException {
    CountDownLatch reading = new CountDownLatch(1);
    AtomicReference<Throwable> savedEx = new AtomicReference<>();
    AtomicBoolean interruptStatus = new AtomicBoolean();
    Flow.Publisher<byte[]> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
      @Override
      public void request(long n) {
      }

      @Override
      public void cancel() {
      }
    });

    Thread reader = new Thread(() -> {
      try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
        publisher.subscribe(is);
        reading.countDown();
        is.read();
      }
      catch (Throwable ex) {
        savedEx.set(ex);
        interruptStatus.set(Thread.currentThread().isInterrupted());
      }
    });
    reader.start();

    reading.await();
    for (int i = 0; i < 100 && reader.getState() != Thread.State.WAITING; i++) {
      Thread.sleep(20);
    }
    assertThat(reader.getState()).isEqualTo(Thread.State.WAITING);
    reader.interrupt();
    reader.join(5000);

    assertThat(reader.isAlive()).as("read() did not return after interrupt").isFalse();
    assertThat(savedEx.get()).isInstanceOf(InterruptedIOException.class)
            .hasMessage("Interrupted while awaiting data");
    assertThat(interruptStatus).as("interrupt status restored").isTrue();
  }

  @Test
  void interruptAfterPartialRead() throws IOException {
    Flow.Publisher<byte[]> publisher = subscriber -> {
      subscriber.onSubscribe(new Flow.Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
      });
      subscriber.onNext(FOO);
    };

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);
      Thread.currentThread().interrupt();
      byte[] chunk = new byte[6];
      assertThat(is.read(chunk)).as("bytes read before interruption").isEqualTo(3);
      assertThat(chunk).startsWith(FOO);
      assertThatExceptionOfType(InterruptedIOException.class)
              .isThrownBy(is::read).withMessage("Interrupted while awaiting data");
      assertThat(Thread.currentThread().isInterrupted()).as("interrupt status restored").isTrue();
    }
    finally {
      Thread.interrupted();
    }
  }

  private static final byte[] FOO = "foo".getBytes(UTF_8);

  private static final byte[] BAR = "bar".getBytes(UTF_8);

  private static final byte[] BAZ = "baz".getBytes(UTF_8);

  private final Executor executor = Executors.newSingleThreadExecutor();

  private final OutputStreamPublisher.ByteMapper<byte[]> byteMapper =
          new OutputStreamPublisher.ByteMapper<>() {

            @Override
            public byte[] map(int b) {
              return new byte[] { (byte) b };
            }

            @Override
            public byte[] map(byte[] b, int off, int len) {
              byte[] result = new byte[len];
              System.arraycopy(b, off, result, 0, len);
              return result;
            }
          };

  @Test
  void basic() throws IOException {
    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.flush();
              out.write(BAR);
              out.flush();
              out.write(BAZ);
              out.flush();
            },
            this.byteMapper, this.executor, null);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);

      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(BAR);

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(BAZ);

      assertThat(is.read(chunk)).isEqualTo(-1);
    }
  }

  @Test
  void chunkSize() throws Exception {
    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.write(BAR);
              out.write(BAZ);
            },
            this.byteMapper, this.executor, 2);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);

      StringBuilder sb = new StringBuilder();
      byte[] chunk = new byte[3];

      sb.append((char) is.read());
      assertThat(sb).matches("f");

      assertThat(is.read(chunk)).isEqualTo(3);
      sb.append(new String(chunk, UTF_8));
      assertThat(sb).matches("foob");

      assertThat(is.read(chunk)).isEqualTo(3);
      sb.append(new String(chunk, UTF_8));
      assertThat(sb).matches("foobarb");

      assertThat(is.read(chunk)).isEqualTo(2);
      sb.append(new String(chunk, 0, 2, UTF_8));
      assertThat(sb).matches("foobarbaz");

      assertThat(is.read()).isEqualTo(-1);
    }
  }

  @Test
  void cancel() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              assertThatIOException()
                      .isThrownBy(() -> {
                        out.write(FOO);
                        out.flush();
                        out.write(BAR);
                        out.flush();
                        latch1.countDown();
                        out.write(BAZ);
                        out.flush();
                      })
                      .withMessage("Subscription has been terminated");
              latch2.countDown();
            }, this.byteMapper, this.executor, null);

    List<byte[]> discarded = new ArrayList<>();

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, discarded::add, 1)) {
      publisher.subscribe(is);
      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      latch1.await();
    }

    latch2.await();
    assertThat(discarded).containsExactly("bar".getBytes(UTF_8));
  }

  @Test
  void closed() throws InterruptedException, IOException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
              writer.write("foo");
              writer.close();
              assertThatIOException().isThrownBy(() -> writer.write("bar")).withMessage("Stream closed");
              latch.countDown();
            },
            this.byteMapper, this.executor, null);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);
      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      assertThat(is.read(chunk)).isEqualTo(-1);
    }

    latch.await();
  }

  @Test
  void mapperThrowsException() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.flush();
              assertThatIOException()
                      .isThrownBy(() -> {
                        out.write(BAR);
                        out.flush();
                      })
                      .withMessage("Subscription has been terminated");
              latch.countDown();
            },
            this.byteMapper, this.executor, null);

    Throwable savedEx = null;

    StringBuilder sb = new StringBuilder();
    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(
            s -> { throw new NullPointerException("boom"); }, s -> { }, 1)) {

      publisher.subscribe(is);
      sb.append(new String(new byte[] { (byte) is.read() }, UTF_8));
    }
    catch (Throwable ex) {
      savedEx = ex;
    }

    latch.await();

    assertThat(sb.toString()).isEqualTo("");
    assertThat(savedEx).hasMessage("boom");
  }

}
