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

package infra.messaging.handler.invocation.reactive;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import reactor.core.Scannable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChannelSendOperator}.
 * <p>See also {@code ChannelSendOperatorTests} in {@code infra-http-reactive}.
 */
class ChannelSendOperatorTests {

  @Test
  void exposesSourceAndPrefetchThroughScannable() {
    Flux<String> source = Flux.just("one");
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(source, publisher -> Mono.empty());

    assertThat(operator.scan(Scannable.Attr.PARENT)).isSameAs(source);
    assertThat(operator.scan(Scannable.Attr.PREFETCH)).isEqualTo(Integer.MAX_VALUE);
    assertThat(operator.scanUnsafe(Scannable.Attr.ACTUAL)).isNull();
  }

  @Test
  void forwardsErrorAfterWritingItems() {
    List<String> written = new ArrayList<>();
    IllegalStateException failure = new IllegalStateException("source failure");
    Flux<String> source = Flux.just("one", "two").concatWith(Mono.error(failure));
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(source,
            publisher -> Flux.from(publisher).limitRate(1).doOnNext(written::add).then());

    StepVerifier.create(operator).expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify(Duration.ofSeconds(5));
    assertThat(written).containsExactly("one", "two");
  }

  @Test
  void reportsWriteFunctionFailureForEmptySource() {
    UnsupportedOperationException failure = new UnsupportedOperationException("cannot write");
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(Flux.empty(), publisher -> {
      throw failure;
    });

    StepVerifier.create(operator).expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void cancelsSourceAndWriterWhenCompletionSubscriberCancels() {
    AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
    AtomicReference<SignalType> writerSignal = new AtomicReference<>();
    Flux<String> source = Flux.just("one").concatWith(Flux.never()).doFinally(sourceSignal::set);
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(source,
            publisher -> Mono.<Void>never().doFinally(writerSignal::set));

    BaseSubscriber<Void> completion = new BaseSubscriber<>() { };
    operator.subscribe(completion);
    completion.cancel();

    assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
    assertThat(writerSignal.get()).isEqualTo(SignalType.CANCEL);
  }

  @Test
  void releasesCachedBufferWhenSourceFailsBeforeWriterRequestsIt() {
    LeakAwareDataBufferFactory factory = new LeakAwareDataBufferFactory();
    DataBuffer buffer = factory.allocateBuffer(16);
    IllegalStateException failure = new IllegalStateException("source failure");
    AtomicReference<Throwable> receivedError = new AtomicReference<>();
    List<DataBuffer> written = new ArrayList<>();
    BaseSubscriber<DataBuffer> writer = new BaseSubscriber<>() {
      @Override
      protected void hookOnSubscribe(Subscription subscription) { }

      @Override
      protected void hookOnNext(DataBuffer value) {
        written.add(value);
        value.release();
      }

      @Override
      protected void hookOnError(Throwable throwable) {
        receivedError.set(throwable);
      }
    };
    Flux<DataBuffer> source = Flux.create(sink -> {
      sink.next(buffer);
      sink.error(failure);
    });
    ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(source, publisher -> {
      publisher.subscribe(writer);
      return Mono.never();
    });

    BaseSubscriber<Void> completion = new BaseSubscriber<>() { };
    operator.subscribe(completion);
    assertThat(receivedError.get()).isNull();
    writer.request(1);
    assertThat(receivedError.get()).isSameAs(failure);
    assertThat(written).isEmpty();
    completion.cancel();
    factory.checkForLeaks();
  }

  @Test
  void sourceCancelledWhenWriteFunctionResultErrorsWhileItemCached() {
    AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
    Flux<String> source = Flux.just("one").concatWith(Flux.never()).doFinally(sourceSignal::set);
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(source,
            publisher -> Mono.error(new IllegalStateException("boom")));

    StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
    assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
  }

  @Test
  void sourceCancelledWhenWriteFunctionThrowsWhileItemCached() {
    AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
    Flux<String> source = Flux.just("one").concatWith(Flux.never()).doFinally(sourceSignal::set);
    ChannelSendOperator<String> operator = new ChannelSendOperator<>(source, publisher -> {
      throw new IllegalStateException("boom");
    });

    StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
    assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
  }

  @Test
  void sourceCancelledAndCachedItemReleasedWhenWriteFunctionResultErrors() {
    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
    AtomicReference<SignalType> sourceSignal = new AtomicReference<>();
    ChannelSendOperator<DataBuffer> operator = new ChannelSendOperator<>(
            Flux.<DataBuffer>create(sink -> {
              DataBuffer dataBuffer = bufferFactory.allocateBuffer(256);
              dataBuffer.write("foo", StandardCharsets.UTF_8);
              sink.next(dataBuffer);
            }).doFinally(sourceSignal::set),
            publisher -> Mono.error(new IllegalStateException("boom")));

    StepVerifier.create(operator).expectErrorMessage("boom").verify(Duration.ofSeconds(5));
    assertThat(sourceSignal.get()).isEqualTo(SignalType.CANCEL);
    bufferFactory.checkForLeaks();
  }

}
