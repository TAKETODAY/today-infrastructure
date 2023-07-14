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

package cn.taketoday.web.service.invoker;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.service.annotation.GetExchange;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link RequestBodyArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class RequestBodyArgumentResolverTests {

  private final TestReactorExchangeAdapter client = new TestReactorExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void stringBody() {
    String body = "bodyValue";
    this.service.execute(body);

    assertThat(getBodyValue()).isEqualTo(body);
    assertThat(getPublisherBody()).isNull();
  }

  @Test
  void monoBody() {
    Mono<String> bodyMono = Mono.just("bodyValue");
    this.service.executeMono(bodyMono);

    assertThat(getBodyValue()).isNull();
    assertThat(getPublisherBody()).isSameAs(bodyMono);
    assertThat(getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() { });
  }

  @Test
  @SuppressWarnings("unchecked")
  void singleBody() {
    String bodyValue = "bodyValue";
    this.service.executeSingle(Single.just(bodyValue));

    assertThat(getBodyValue()).isNull();
    assertThat(getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() { });

    Publisher<?> body = getPublisherBody();
    assertThat(body).isNotNull();
    assertThat(((Mono<String>) body).block()).isEqualTo(bodyValue);
  }

  @Test
  void monoVoid() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.service.executeMonoVoid(Mono.empty()))
            .withMessage("Async type for @RequestBody should produce value(s)");
  }

  @Test
  void completable() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.service.executeCompletable(Completable.complete()))
            .withMessage("Async type for @RequestBody should produce value(s)");
  }

  @Test
  void notRequestBody() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.service.executeNotRequestBody("value"))
            .withMessage("Could not resolve parameter [0] in " +
                    "public abstract void cn.taketoday.web.service.invoker." +
                    "RequestBodyArgumentResolverTests$Service.executeNotRequestBody(java.lang.String): " +
                    "No suitable resolver");
  }

  @Test
  void ignoreNull() {
    this.service.execute(null);

    assertThat(getBodyValue()).isNull();
    assertThat(getPublisherBody()).isNull();

    this.service.executeMono(null);

    assertThat(getBodyValue()).isNull();
    assertThat(getPublisherBody()).isNull();
  }

  @Nullable
  private Object getBodyValue() {
    return getReactiveRequestValues().getBodyValue();
  }

  @Nullable
  private Publisher<?> getPublisherBody() {
    return getReactiveRequestValues().getBodyPublisher();
  }

  @Nullable
  private ParameterizedTypeReference<?> getBodyElementType() {
    return getReactiveRequestValues().getBodyPublisherElementType();
  }

  private ReactiveHttpRequestValues getReactiveRequestValues() {
    return (ReactiveHttpRequestValues) this.client.getRequestValues();
  }

  private interface Service {

    @GetExchange
    void execute(@RequestBody String body);

    @GetExchange
    void executeMono(@RequestBody Mono<String> body);

    @GetExchange
    void executeSingle(@RequestBody Single<String> body);

    @GetExchange
    void executeMonoVoid(@RequestBody Mono<Void> body);

    @GetExchange
    void executeCompletable(@RequestBody Completable body);

    @GetExchange
    void executeNotRequestBody(String body);
  }

}
