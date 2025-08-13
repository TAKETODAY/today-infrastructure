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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/13 21:17
 */
class PublisherFutureTests {

  @Test
  void sinkEmitsValueAndCompletes() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    sink.emitValue("test", Sinks.EmitFailureHandler.FAIL_FAST);

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isEqualTo("test");
  }

  @Test
  void sinkEmitsError() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());
    RuntimeException error = new RuntimeException("test error");

    sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST);

    assertThat(future).failsWithin(Duration.ZERO)
            .withThrowableThat().havingRootCause()
            .isInstanceOf(RuntimeException.class)
            .withMessage("test error");
  }

  @Test
  void sinkEmitsCompleteWithoutValue() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isNull();
  }

  @Test
  void multipleEmitsOnlyFirstIsAccepted() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    PublisherFuture<String> future = PublisherFuture.of(sink.asFlux());

    sink.tryEmitNext("first");
    sink.tryEmitNext("second");
    sink.tryEmitComplete();

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isEqualTo("first");
  }

  @Test
  void cancelledFutureIgnoresSinkSignals() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    future.cancel(true);
    sink.emitValue("test", Sinks.EmitFailureHandler.FAIL_FAST);

    assertThat(future).isCancelled();
  }

  @Test
  void errorAfterValueIsIgnored() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    PublisherFuture<String> future = PublisherFuture.of(sink.asFlux());

    sink.tryEmitNext("value");
    sink.tryEmitError(new RuntimeException("error"));

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isEqualTo("value");
  }

  @Test
  void immediatelyEmptyPublisher() {
    Sinks.Empty<String> sink = Sinks.empty();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    sink.tryEmitEmpty();

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isNull();
  }

  @Test
  void emptyCompleteAfterErrorIsIgnored() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());
    RuntimeException error = new RuntimeException("test");

    sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST);
    sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);

    assertThat(future).failsWithin(Duration.ZERO)
            .withThrowableThat().havingRootCause()
            .isInstanceOf(RuntimeException.class)
            .withMessage("test");
  }

  @Test
  void nullPublisherThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> PublisherFuture.of(null))
            .withMessage("Publisher is required");
  }

  @Test
  void cancelBeforeSubscription() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    future.cancel(true);

    sink.emitValue("test", Sinks.EmitFailureHandler.FAIL_FAST);
    assertThat(future).isCancelled();
  }

  @Test
  void concurrentEmitsOnlyFirstValueAccepted() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    PublisherFuture<String> future = PublisherFuture.of(sink.asFlux());

    Thread t1 = new Thread(() -> sink.tryEmitNext("first"));
    Thread t2 = new Thread(() -> sink.tryEmitNext("second"));
    t1.start();
    t2.start();

    try {
      t1.join();
      t2.join();
      sink.tryEmitComplete();

      assertThat(future).succeedsWithin(Duration.ofSeconds(1))
              .isIn("first", "second");
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Test
  void valueDeliveredAfterDelay() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    Thread t = new Thread(() -> {
      try {
        Thread.sleep(100);
        sink.emitValue("delayed", Sinks.EmitFailureHandler.FAIL_FAST);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    t.start();

    assertThat(future).succeedsWithin(Duration.ofSeconds(1))
            .isEqualTo("delayed");
  }

  @Test
  void multipleThreadsSubscribingAndCancelling() {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    PublisherFuture<String> future = PublisherFuture.of(sink.asFlux());

    Thread emitter = new Thread(() -> {
      sink.tryEmitNext("value");
      sink.tryEmitComplete();
    });

    Thread canceller = new Thread(() -> future.cancel(true));

    emitter.start();
    canceller.start();

    try {
      emitter.join();
      canceller.join();

      assertThat(future).satisfiesAnyOf(
              f -> assertThat(f).isCancelled(),
              f -> assertThat(f).succeedsWithin(Duration.ofSeconds(1)).isEqualTo("value")
      );
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Test
  void onlyFirstThreadCompletesSubscription() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    Thread t1 = new Thread(() -> sink.emitValue("first", Sinks.EmitFailureHandler.FAIL_FAST));
    Thread t2 = new Thread(() -> sink.emitValue("second", Sinks.EmitFailureHandler.FAIL_FAST));

    t1.start();
    t2.start();

    assertThat(future).succeedsWithin(Duration.ofSeconds(1))
            .satisfiesAnyOf(
                    value -> assertThat(value).isEqualTo("first"),
                    value -> assertThat(value).isEqualTo("second")
            );
  }

  @Test
  void multipleSubscriptionsWithDifferentTypes() {
    Sinks.Many<Number> numberSink = Sinks.many().multicast().onBackpressureBuffer();
    Sinks.Many<String> stringSink = Sinks.many().multicast().onBackpressureBuffer();

    PublisherFuture<Number> future = PublisherFuture.of(numberSink.asFlux());
    stringSink.asFlux().subscribe(s -> { });

    numberSink.tryEmitNext(42);

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isEqualTo(42);
  }

  @Test
  void cancelAfterCompletion() {
    Sinks.One<String> sink = Sinks.one();
    PublisherFuture<String> future = PublisherFuture.of(sink.asMono());

    sink.emitValue("done", Sinks.EmitFailureHandler.FAIL_FAST);
    future.cancel(true);

    assertThat(future).succeedsWithin(Duration.ZERO)
            .isEqualTo("done");
  }

}