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

package infra.http.server.reactive;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class ChannelSendOperatorTests {

  private final OneByOneAsyncWriter writer = new OneByOneAsyncWriter();

  @Test
  public void errorBeforeFirstItem() throws Exception {
    IllegalStateException error = new IllegalStateException("boo");
    Mono<Void> completion = Mono.<String>error(error).as(this::sendOperator);
    Signal<Void> signal = completion.materialize().block();

    assertThat(signal).isNotNull();
    assertThat(signal.getThrowable()).as("Unexpected signal: " + signal).isSameAs(error);
  }

  @Test
  public void completionBeforeFirstItem() throws Exception {
    Mono<Void> completion = Flux.<String>empty().as(this::sendOperator);
    Signal<Void> signal = completion.materialize().block();

    assertThat(signal).isNotNull();
    assertThat(signal.isOnComplete()).as("Unexpected signal: " + signal).isTrue();

    assertThat(this.writer.items.size()).isEqualTo(0);
    assertThat(this.writer.completed).isTrue();
  }

  @Test
  public void writeOneItem() throws Exception {
    Mono<Void> completion = Flux.just("one").as(this::sendOperator);
    Signal<Void> signal = completion.materialize().block();

    assertThat(signal).isNotNull();
    assertThat(signal.isOnComplete()).as("Unexpected signal: " + signal).isTrue();

    assertThat(this.writer.items.size()).isEqualTo(1);
    assertThat(this.writer.items.get(0)).isEqualTo("one");
    assertThat(this.writer.completed).isTrue();
  }

  @Test
  public void writeMultipleItems() {
    List<String> items = Arrays.asList("one", "two", "three");
    Mono<Void> completion = Flux.fromIterable(items).as(this::sendOperator);
    Signal<Void> signal = completion.materialize().block();

    assertThat(signal).isNotNull();
    assertThat(signal.isOnComplete()).as("Unexpected signal: " + signal).isTrue();

    assertThat(this.writer.items.size()).isEqualTo(3);
    assertThat(this.writer.items.get(0)).isEqualTo("one");
    assertThat(this.writer.items.get(1)).isEqualTo("two");
    assertThat(this.writer.items.get(2)).isEqualTo("three");
    assertThat(this.writer.completed).isTrue();
  }

  @Test
  public void errorAfterMultipleItems() {
    IllegalStateException error = new IllegalStateException("boo");
    Flux<String> publisher = Flux.generate(() -> 0, (idx, subscriber) -> {
      int i = ++idx;
      subscriber.next(String.valueOf(i));
      if (i == 3) {
        subscriber.error(error);
      }
      return i;
    });
    Mono<Void> completion = publisher.as(this::sendOperator);
    Signal<Void> signal = completion.materialize().block();

    assertThat(signal).isNotNull();
    assertThat(signal.getThrowable()).as("Unexpected signal: " + signal).isSameAs(error);

    assertThat(this.writer.items.size()).isEqualTo(3);
    assertThat(this.writer.items.get(0)).isEqualTo("1");
    assertThat(this.writer.items.get(1)).isEqualTo("2");
    assertThat(this.writer.items.get(2)).isEqualTo("3");
    assertThat(this.writer.error).isSameAs(error);
  }

  @Test // gh-22720
  public void cancelWhileItemCached() {
    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

    ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(
            Mono.fromCallable(() -> {
              DataBuffer dataBuffer = bufferFactory.allocateBuffer();
              dataBuffer.write("foo", StandardCharsets.UTF_8);
              return dataBuffer;
            }),
            publisher -> {
              ZeroDemandSubscriber subscriber = new ZeroDemandSubscriber();
              publisher.subscribe(subscriber);
              return Mono.never();
            });

    BaseSubscriber<Void> subscriber = new BaseSubscriber<Void>() { };
    operator.subscribe(subscriber);
    subscriber.cancel();

    bufferFactory.checkForLeaks();
  }

  @Test // gh-22720
  public void errorFromWriteSourceWhileItemCached() {

    // 1. First item received
    // 2. writeFunction applied and writeCompletionBarrier subscribed to it
    // 3. Write Publisher fails right after that and before request(n) from server

    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
    ZeroDemandSubscriber writeSubscriber = new ZeroDemandSubscriber();

    ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(
            Flux.create(sink -> {
              DataBuffer dataBuffer = bufferFactory.allocateBuffer();
              dataBuffer.write("foo", StandardCharsets.UTF_8);
              sink.next(dataBuffer);
              sink.error(new IllegalStateException("err"));
            }),
            publisher -> {
              publisher.subscribe(writeSubscriber);
              return Mono.never();
            });

    operator.subscribe(new BaseSubscriber<Void>() { });
    try {
      writeSubscriber.signalDemand(1);  // Let cached signals ("foo" and error) be published..
    }
    catch (Throwable ex) {
      assertThat(ex.getCause()).isNotNull();
      assertThat(ex.getCause().getMessage()).isEqualTo("err");
    }

    bufferFactory.checkForLeaks();
  }

  @Test // gh-22720
  public void errorFromWriteFunctionWhileItemCached() {

    // 1. First item received
    // 2. writeFunction applied and writeCompletionBarrier subscribed to it
    // 3. writeFunction fails, e.g. to flush status and headers, before request(n) from server

    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();

    ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(
            Flux.create(sink -> {
              DataBuffer dataBuffer = bufferFactory.allocateBuffer();
              dataBuffer.write("foo", StandardCharsets.UTF_8);
              sink.next(dataBuffer);
            }),
            publisher -> {
              publisher.subscribe(new ZeroDemandSubscriber());
              return Mono.error(new IllegalStateException("err"));
            });

    StepVerifier.create(operator).expectErrorMessage("err").verify(Duration.ofSeconds(5));
    bufferFactory.checkForLeaks();
  }

  @Test // gh-23175
  public void errorInWriteFunction() {

    StepVerifier
            .create(new ChannelSendOperator<>(Mono.just("one"), p -> {
              throw new IllegalStateException("boo");
            }))
            .expectErrorMessage("boo")
            .verify(Duration.ofMillis(5000));

    StepVerifier
            .create(new ChannelSendOperator<>(Mono.empty(), p -> {
              throw new IllegalStateException("boo");
            }))
            .expectErrorMessage("boo")
            .verify(Duration.ofMillis(5000));
  }

  private <T> Mono<Void> sendOperator(Publisher<String> source) {
    return new ChannelSendOperator<>(source, writer::send);
  }

  private static class OneByOneAsyncWriter {

    private List<String> items = new ArrayList<>();

    private boolean completed = false;

    private Throwable error;

    public Publisher<Void> send(Publisher<String> publisher) {
      return subscriber -> Executors.newSingleThreadScheduledExecutor()
              .schedule(() -> publisher.subscribe(new WriteSubscriber(subscriber)), 50, TimeUnit.MILLISECONDS);
    }

    private class WriteSubscriber implements Subscriber<String> {

      private Subscription subscription;

      private final Subscriber<? super Void> subscriber;

      public WriteSubscriber(Subscriber<? super Void> subscriber) {
        this.subscriber = subscriber;
      }

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
      }

      @Override
      public void onNext(String item) {
        items.add(item);
        this.subscription.request(1);
      }

      @Override
      public void onError(Throwable ex) {
        error = ex;
        this.subscriber.onError(ex);
      }

      @Override
      public void onComplete() {
        completed = true;
        this.subscriber.onComplete();
      }
    }
  }

  private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      // Just subscribe without requesting
    }

    public void signalDemand(long demand) {
      upstream().request(demand);
    }
  }

}
