/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
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

  private final Executor executor = Executors.newSingleThreadExecutor();

  @Test
  void basic() {
    Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        writer.write("foo");
        writer.write("bar");
        writer.write("baz");
      }
    }, this.executor);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foobarbaz"))
            .verifyComplete();
  }

  @Test
  void flush() {
    Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        writer.write("foo");
        writer.flush();
        writer.write("bar");
        writer.flush();
        writer.write("baz");
        writer.flush();
      }
    }, this.executor);
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

    Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        assertThatIOException()
                .isThrownBy(() -> {
                  writer.write("foo");
                  writer.flush();
                  writer.write("bar");
                  writer.flush();
                })
                .withMessage("Subscription has been terminated");
        latch.countDown();
      }
    }, this.executor);
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

    Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      writer.write("foo");
      writer.close();
      assertThatIOException().isThrownBy(() -> writer.write("bar"))
              .withMessage("Stream closed");
      latch.countDown();
    }, this.executor);
    Flux<String> flux = toString(flowPublisher);

    StepVerifier.create(flux)
            .assertNext(s -> assertThat(s).isEqualTo("foo"))
            .verifyComplete();

    latch.await();
  }

  @Test
  void negativeRequestN() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
      try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        writer.write("foo");
        writer.flush();
        writer.write("foo");
        writer.flush();
      }
      finally {
        latch.countDown();
      }
    }, this.executor);
    Flow.Subscription[] subscriptions = new Flow.Subscription[1];
    Flux<String> flux = toString(a -> flowPublisher.subscribe(new Flow.Subscriber<>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        subscriptions[0] = subscription;
        a.onSubscribe(subscription);
      }

      @Override
      public void onNext(ByteBuffer item) {
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

  private static Flux<String> toString(Flow.Publisher<ByteBuffer> flowPublisher) {
    return Flux.from(FlowAdapters.toPublisher(flowPublisher))
            .map(bb -> StandardCharsets.UTF_8.decode(bb).toString());
  }

}