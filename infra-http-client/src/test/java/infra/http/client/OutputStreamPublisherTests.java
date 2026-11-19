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
import org.mockito.ArgumentCaptor;
import org.reactivestreams.FlowAdapters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 19:59
 */
class OutputStreamPublisherTests {

  private static final byte[] FOO = "foo".getBytes(StandardCharsets.UTF_8);

  private static final byte[] BAR = "bar".getBytes(StandardCharsets.UTF_8);

  private static final byte[] BAZ = "baz".getBytes(StandardCharsets.UTF_8);

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
  void rejectsANonPositiveChunkSize() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new OutputStreamPublisher<>(output -> { }, this.byteMapper, Runnable::run, 0))
            .withMessage("ChunkSize must be larger than 0");
  }

  @Test
  void propagatesAWriterFailureAfterFlushedData() {
    var failure = new IOException("write failed");
    var publisher = new OutputStreamPublisher<>(output -> {
      output.write(FOO);
      output.flush();
      throw failure;
    }, this.byteMapper, Runnable::run, null);

    StepVerifier.create(toString(publisher)).expectNext("foo")
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure)).verify();
  }

  @Test
  void emitsInvalidDemandErrorEvenWhenTheWriterProducesNoData() {
    var task = new AtomicReference<Runnable>();
    var publisher = new OutputStreamPublisher<>(output -> { }, this.byteMapper, task::set, null);
    Flow.Subscriber<byte[]> subscriber = mock();
    var subscription = ArgumentCaptor.forClass(Flow.Subscription.class);
    publisher.subscribe(subscriber);
    verify(subscriber).onSubscribe(subscription.capture());
    subscription.getValue().request(1);
    subscription.getValue().request(0);

    task.get().run();

    var error = ArgumentCaptor.forClass(Throwable.class);
    verify(subscriber).onError(error.capture());
    assertThat(error.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("request should be a positive number");
    verifyNoMoreInteractions(subscriber);
  }

  @Test
  void completesSilentlyWhenCancelledBeforeAnEmptyWrite() {
    var publisher = new OutputStreamPublisher<>(output -> { }, this.byteMapper, Runnable::run, null);
    Flow.Subscriber<byte[]> subscriber = mock();
    willAnswer(invocation -> {
      Flow.Subscription subscription = invocation.getArgument(0);
      subscription.cancel();
      return null;
    }).given(subscriber).onSubscribe(any());

    publisher.subscribe(subscriber);

    verify(subscriber).onSubscribe(any());
    verifyNoMoreInteractions(subscriber);
  }

  @Test
  void ignoresRepeatedCancellationAndDoesNotNotifyTheCancelledSubscriber() {
    var task = new AtomicReference<Runnable>();
    var publisher = new OutputStreamPublisher<>(output -> output.write(FOO), this.byteMapper, task::set, null);
    Flow.Subscriber<byte[]> subscriber = mock();
    var subscription = ArgumentCaptor.forClass(Flow.Subscription.class);
    publisher.subscribe(subscriber);
    verify(subscriber).onSubscribe(subscription.capture());
    subscription.getValue().request(1);
    subscription.getValue().cancel();
    subscription.getValue().cancel();
    subscription.getValue().request(1);
    subscription.getValue().request(0);

    task.get().run();

    verifyNoMoreInteractions(subscriber);
  }

  @Test
  void saturatesDemandRatherThanOverflowing() {
    var task = new AtomicReference<Runnable>();
    var publisher = new OutputStreamPublisher<>(output -> {
      output.write(FOO);
      output.flush();
      output.write(BAR);
    }, this.byteMapper, task::set, null);
    Flow.Subscriber<byte[]> subscriber = mock();
    var subscription = ArgumentCaptor.forClass(Flow.Subscription.class);
    publisher.subscribe(subscriber);
    verify(subscriber).onSubscribe(subscription.capture());
    subscription.getValue().request(Long.MAX_VALUE - 1);
    subscription.getValue().request(3);
    subscription.getValue().request(1);

    task.get().run();

    var order = inOrder(subscriber);
    order.verify(subscriber).onNext(FOO);
    order.verify(subscriber).onNext(BAR);
    order.verify(subscriber).onComplete();
  }

  @Test
  void basic() {
    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      outputStream.write(FOO);
      outputStream.write(BAR);
      outputStream.write(BAZ);
    }, this.byteMapper, this.executor, null);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foobarbaz"))
            .verifyComplete();
  }

  @Test
  void flush() {
    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      outputStream.write(FOO);
      outputStream.flush();
      outputStream.write(BAR);
      outputStream.flush();
      outputStream.write(BAZ);
      outputStream.flush();
    }, this.byteMapper, this.executor, null);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .assertNext(s -> assertThat(s).isEqualTo("bar"))
            .assertNext(s -> assertThat(s).isEqualTo("baz"))
            .verifyComplete();
  }

  @Test
  void chunkSize() {
    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      outputStream.write(FOO);
      outputStream.write(BAR);
      outputStream.write(BAZ);
    }, this.byteMapper, this.executor, 3);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .assertNext(s -> assertThat(s).isEqualTo("bar"))
            .assertNext(s -> assertThat(s).isEqualTo("baz"))
            .verifyComplete();
  }

  @Test
  void cancel() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      assertThatIOException()
              .isThrownBy(() -> {
                outputStream.write(FOO);
                outputStream.flush();
                outputStream.write(BAR);
                outputStream.flush();
              })
              .withMessage("Subscription has been terminated");
      latch.countDown();

    }, this.byteMapper, this.executor, null);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux, 1)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .thenCancel()
            .verify();

    latch.await();
  }

  @Test
  void closed() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      writer.write("foo");
      writer.close();
      assertThatIOException().isThrownBy(() -> writer.write("bar"))
              .withMessage("Stream closed");
      latch.countDown();
    }, this.byteMapper, this.executor, null);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .verifyComplete();

    latch.await();
  }

  @Test
  void negativeRequestN() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> flowPublisher = new OutputStreamPublisher<>(outputStream -> {
      try (outputStream) {
        outputStream.write(FOO);
        outputStream.flush();
        outputStream.write(BAR);
        outputStream.flush();
      }
      finally {
        latch.countDown();
      }
    }, this.byteMapper, this.executor, null);
    Flow.Subscription[] subscriptions = new Flow.Subscription[1];
    Flux<String> flux = toString(a -> flowPublisher.subscribe(new Flow.Subscriber<>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        subscriptions[0] = subscription;
        a.onSubscribe(subscription);
      }

      @Override
      public void onNext(byte[] item) {
        a.onNext(item);
      }

      @Override
      public void onError(Throwable throwable) {
        a.onError(throwable);
      }

      @Override
      public void onComplete() {
        a.onComplete();
      }
    }));

    StepVerifier.create(flux, 1)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .then(() -> subscriptions[0].request(-1))
            .expectErrorMessage("request should be a positive number")
            .verify();

    latch.await();
  }

  private static Flux<String> toString(Flow.Publisher<byte[]> flowPublisher) {
    return Flux.from(FlowAdapters.toPublisher(flowPublisher))
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

}
