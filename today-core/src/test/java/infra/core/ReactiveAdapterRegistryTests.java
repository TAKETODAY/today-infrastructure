/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
class ReactiveAdapterRegistryTests {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  private final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

  @Test
  void getAdapterForReactiveSubType() {
    ReactiveAdapter adapter1 = getAdapter(Flux.class);
    ReactiveAdapter adapter2 = getAdapter(ExtendedFlux.class);
    assertThat(adapter2).isSameAs(adapter1);

    // Register regular reactive type (after existing adapters)
    this.registry.registerReactiveType(
            ReactiveTypeDescriptor.multiValue(ExtendedFlux.class, ExtendedFlux::empty),
            o -> (ExtendedFlux<?>) o,
            ExtendedFlux::from);

    // Matches for ExtendedFlux itself
    ReactiveAdapter adapter3 = getAdapter(ExtendedFlux.class);
    assertThat(adapter3).isNotNull();
    assertThat(adapter3).isNotSameAs(adapter1);

    // Does not match for ExtendedFlux subclass since the default Flux adapter
    // is being assignability-checked first when no specific match was found
    ReactiveAdapter adapter4 = getAdapter(ExtendedExtendedFlux.class);
    assertThat(adapter4).isSameAs(adapter1);

    // Register reactive type override (before existing adapters)
    this.registry.registerReactiveTypeOverride(
            ReactiveTypeDescriptor.multiValue(Flux.class, ExtendedFlux::empty),
            o -> (ExtendedFlux<?>) o,
            ExtendedFlux::from);

    // Override match for Flux
    ReactiveAdapter adapter5 = getAdapter(Flux.class);
    assertThat(adapter5).isNotNull();
    assertThat(adapter5).isNotSameAs(adapter1);

    // Initially registered adapter specifically matches for ExtendedFlux
    ReactiveAdapter adapter6 = getAdapter(ExtendedFlux.class);
    assertThat(adapter6).isSameAs(adapter3);

    // Override match for ExtendedFlux subclass
    ReactiveAdapter adapter7 = getAdapter(ExtendedExtendedFlux.class);
    assertThat(adapter7).isSameAs(adapter5);
  }

  private ReactiveAdapter getAdapter(Class<?> reactiveType) {
    ReactiveAdapter adapter = this.registry.getAdapter(reactiveType);
    assertThat(adapter).isNotNull();
    return adapter;
  }

  private static class ExtendedFlux<T> extends Flux<T> {

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
      throw new UnsupportedOperationException();
    }
  }

  private static class ExtendedExtendedFlux<T> extends ExtendedFlux<T> {
  }

  @Nested
  class Reactor {

    @Test
    void defaultAdapterRegistrations() {
      // Reactor
      assertThat(getAdapter(Mono.class)).isNotNull();
      assertThat(getAdapter(Flux.class)).isNotNull();

      // Publisher
      assertThat(getAdapter(Publisher.class)).isNotNull();

      // Completable
      assertThat(getAdapter(CompletableFuture.class)).isNotNull();

      // @since 5.0
      assertThat(getAdapter(Future.class)).isNotNull();

      assertThat(getAdapter(Future.class).supportsEmpty()).isTrue();
      assertThat(getAdapter(Mono.class).supportsEmpty()).isTrue();
      assertThat(getAdapter(Flux.class).supportsEmpty()).isTrue();
      assertThat(getAdapter(Publisher.class).supportsEmpty()).isTrue();
      assertThat(getAdapter(CompletableFuture.class).supportsEmpty()).isTrue();

    }

    @Test
    void toFlux() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Publisher<Integer> source = io.reactivex.rxjava3.core.Flowable.fromIterable(sequence);
      Object target = getAdapter(Flux.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Flux.class);
      assertThat(((Flux<Integer>) target).collectList().block(ONE_SECOND)).isEqualTo(sequence);
    }

    @Test
    void toMono() {
      Publisher<Integer> source = io.reactivex.rxjava3.core.Flowable.fromArray(1, 2, 3);
      Object target = getAdapter(Mono.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Mono.class);
      assertThat(((Mono<Integer>) target).block(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void toFlowPublisher() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Publisher<Integer> source = io.reactivex.rxjava3.core.Flowable.fromIterable(sequence);
      Object target = getAdapter(Flow.Publisher.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Flow.Publisher.class);
      assertThat(JdkFlowAdapter.flowPublisherToFlux((Flow.Publisher<Integer>) target)
              .collectList().block(ONE_SECOND)).isEqualTo(sequence);
    }

    @Test
    void toCompletableFuture() throws Exception {
      Publisher<Integer> source = Flux.fromArray(new Integer[] { 1, 2, 3 });
      Object target = getAdapter(CompletableFuture.class).fromPublisher(source);
      assertThat(target).isInstanceOf(CompletableFuture.class);
      assertThat(((CompletableFuture<Integer>) target).get()).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void fromCompletableFuture() {
      CompletableFuture<Integer> future = new CompletableFuture<>();
      future.complete(1);
      Object target = getAdapter(CompletableFuture.class).toPublisher(future);
      assertThat(target).as("Expected Mono Publisher: " + target.getClass().getName()).isInstanceOf(Mono.class);
      assertThat(((Mono<Integer>) target).block(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void fromFuture() {
      Promise<Integer> future = Future.forPromise();
      future.trySuccess(1);
      Object target = getAdapter(Future.class).toPublisher(future);
      assertThat(target).as("Expected Mono Publisher: " + target.getClass().getName()).isInstanceOf(Mono.class);
      assertThat(((Mono<Integer>) target).block(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void toFuture() {
      Publisher<Integer> source = Flux.fromArray(new Integer[] { 1, 2, 3 });
      Object target = getAdapter(Future.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Future.class);
      assertThat(((Future<Integer>) target).join()).isEqualTo(Integer.valueOf(1));
    }

  }

  @Nested
  class RxJava3 {

    @Test
    void defaultAdapterRegistrations() {
      // RxJava 3
      assertThat(getAdapter(io.reactivex.rxjava3.core.Flowable.class)).isNotNull();
      assertThat(getAdapter(io.reactivex.rxjava3.core.Observable.class)).isNotNull();
      assertThat(getAdapter(io.reactivex.rxjava3.core.Single.class)).isNotNull();
      assertThat(getAdapter(io.reactivex.rxjava3.core.Maybe.class)).isNotNull();
      assertThat(getAdapter(io.reactivex.rxjava3.core.Completable.class)).isNotNull();
    }

    @Test
    void toFlowable() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Publisher<Integer> source = Flux.fromIterable(sequence);
      Object target = getAdapter(io.reactivex.rxjava3.core.Flowable.class).fromPublisher(source);
      assertThat(target).isInstanceOf(io.reactivex.rxjava3.core.Flowable.class);
      assertThat(((io.reactivex.rxjava3.core.Flowable<?>) target).toList().blockingGet()).isEqualTo(sequence);
    }

    @Test
    void toObservable() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Publisher<Integer> source = Flux.fromIterable(sequence);
      Object target = getAdapter(io.reactivex.rxjava3.core.Observable.class).fromPublisher(source);
      assertThat(target).isInstanceOf(io.reactivex.rxjava3.core.Observable.class);
      assertThat(((io.reactivex.rxjava3.core.Observable<?>) target).toList().blockingGet()).isEqualTo(sequence);
    }

    @Test
    void toSingle() {
      Publisher<Integer> source = Flux.fromArray(new Integer[] { 1 });
      Object target = getAdapter(io.reactivex.rxjava3.core.Single.class).fromPublisher(source);
      assertThat(target).isInstanceOf(io.reactivex.rxjava3.core.Single.class);
      assertThat(((io.reactivex.rxjava3.core.Single<Integer>) target).blockingGet()).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void toCompletable() {
      Publisher<Integer> source = Flux.fromArray(new Integer[] { 1, 2, 3 });
      Object target = getAdapter(io.reactivex.rxjava3.core.Completable.class).fromPublisher(source);
      assertThat(target).isInstanceOf(io.reactivex.rxjava3.core.Completable.class);
      ((io.reactivex.rxjava3.core.Completable) target).blockingAwait();
    }

    @Test
    void fromFlowable() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Object source = io.reactivex.rxjava3.core.Flowable.fromIterable(sequence);
      Object target = getAdapter(io.reactivex.rxjava3.core.Flowable.class).toPublisher(source);
      assertThat(target).as("Expected Flux Publisher: " + target.getClass().getName()).isInstanceOf(Flux.class);
      assertThat(((Flux<Integer>) target).collectList().block(ONE_SECOND)).isEqualTo(sequence);
    }

    @Test
    void fromObservable() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Object source = io.reactivex.rxjava3.core.Observable.fromIterable(sequence);
      Object target = getAdapter(io.reactivex.rxjava3.core.Observable.class).toPublisher(source);
      assertThat(target).as("Expected Flux Publisher: " + target.getClass().getName()).isInstanceOf(Flux.class);
      assertThat(((Flux<Integer>) target).collectList().block(ONE_SECOND)).isEqualTo(sequence);
    }

    @Test
    void fromSingle() {
      Object source = io.reactivex.rxjava3.core.Single.just(1);
      Object target = getAdapter(io.reactivex.rxjava3.core.Single.class).toPublisher(source);
      assertThat(target).as("Expected Mono Publisher: " + target.getClass().getName()).isInstanceOf(Mono.class);
      assertThat(((Mono<Integer>) target).block(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void fromCompletable() {
      Object source = io.reactivex.rxjava3.core.Completable.complete();
      Object target = getAdapter(io.reactivex.rxjava3.core.Completable.class).toPublisher(source);
      assertThat(target).as("Expected Mono Publisher: " + target.getClass().getName()).isInstanceOf(Mono.class);
      ((Mono<Void>) target).block(ONE_SECOND);
    }
  }

  @Nested
  class Mutiny {

    @Test
    void defaultAdapterRegistrations() {
      assertThat(getAdapter(io.smallrye.mutiny.Uni.class)).isNotNull();
      assertThat(getAdapter(io.smallrye.mutiny.Multi.class)).isNotNull();
    }

    @Test
    void toUni() {
      Publisher<Integer> source = Mono.just(1);
      Object target = getAdapter(Uni.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Uni.class);
      assertThat(((Uni<Integer>) target).await().atMost(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void fromUni() {
      Uni<Integer> source = Uni.createFrom().item(1);
      Object target = getAdapter(Uni.class).toPublisher(source);
      assertThat(target).isInstanceOf(Mono.class);
      assertThat(((Mono<Integer>) target).block(ONE_SECOND)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    void toMulti() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Publisher<Integer> source = Flux.fromIterable(sequence);
      Object target = getAdapter(Multi.class).fromPublisher(source);
      assertThat(target).isInstanceOf(Multi.class);
      assertThat(((Multi<Integer>) target).collect().asList().await().atMost(ONE_SECOND)).isEqualTo(sequence);
    }

    @Test
    void fromMulti() {
      List<Integer> sequence = Arrays.asList(1, 2, 3);
      Multi<Integer> source = Multi.createFrom().iterable(sequence);
      Object target = getAdapter(Multi.class).toPublisher(source);
      assertThat(target).isInstanceOf(Flux.class);
      assertThat(((Flux<Integer>) target).blockLast(ONE_SECOND)).isEqualTo(Integer.valueOf(3));
    }
  }

}
