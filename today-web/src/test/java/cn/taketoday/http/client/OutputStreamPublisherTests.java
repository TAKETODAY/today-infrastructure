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

package cn.taketoday.http.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

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
  void basic() {
    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      outputStream.write(FOO);
      outputStream.write(BAR);
      outputStream.write(BAZ);
    }, this.byteMapper, this.executor);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foobarbaz"))
            .verifyComplete();
  }

  @Test
  void flush() {
    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      outputStream.write(FOO);
      outputStream.flush();
      outputStream.write(BAR);
      outputStream.flush();
      outputStream.write(BAZ);
      outputStream.flush();
    }, this.byteMapper, this.executor);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .assertNext(s -> assertThat(s).isEqualTo("bar"))
            .assertNext(s -> assertThat(s).isEqualTo("baz"))
            .verifyComplete();
  }

  @Test
  void chunkSize() {
    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
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

    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      assertThatIOException()
              .isThrownBy(() -> {
                outputStream.write(FOO);
                outputStream.flush();
                outputStream.write(BAR);
                outputStream.flush();
              })
              .withMessage("Subscription has been terminated");
      latch.countDown();

    }, this.byteMapper, this.executor);
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

    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      writer.write("foo");
      writer.close();
      assertThatIOException().isThrownBy(() -> writer.write("bar"))
              .withMessage("Stream closed");
      latch.countDown();
    }, this.byteMapper, this.executor);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .verifyComplete();

    latch.await();
  }

  @Test
  void negativeRequestN() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      try (outputStream) {
        outputStream.write(FOO);
        outputStream.flush();
        outputStream.write(BAR);
        outputStream.flush();
      }
      finally {
        latch.countDown();
      }
    }, this.byteMapper, this.executor);
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