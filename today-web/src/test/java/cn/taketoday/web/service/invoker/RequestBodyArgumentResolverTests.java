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

package cn.taketoday.web.service.invoker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import cn.taketoday.core.ParameterizedTypeReference;
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
public class RequestBodyArgumentResolverTests {

  private final TestHttpClientAdapter client = new TestHttpClientAdapter();

  private Service service;

  @BeforeEach
  void setUp() throws Exception {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
    proxyFactory.afterPropertiesSet();
    this.service = proxyFactory.createClient(Service.class);
  }

  @Test
  void stringBody() {
    String body = "bodyValue";
    this.service.execute(body);

    assertThat(getRequestValues().getBodyValue()).isEqualTo(body);
    assertThat(getRequestValues().getBody()).isNull();
  }

  @Test
  void monoBody() {
    Mono<String> bodyMono = Mono.just("bodyValue");
    this.service.executeMono(bodyMono);

    assertThat(getRequestValues().getBodyValue()).isNull();
    assertThat(getRequestValues().getBody()).isSameAs(bodyMono);
    assertThat(getRequestValues().getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() { });
  }

  @Test
  @SuppressWarnings("unchecked")
  void singleBody() {
    String bodyValue = "bodyValue";
    this.service.executeSingle(Single.just(bodyValue));

    assertThat(getRequestValues().getBodyValue()).isNull();
    assertThat(getRequestValues().getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() { });

    Publisher<?> body = getRequestValues().getBody();
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

    assertThat(getRequestValues().getBodyValue()).isNull();
    assertThat(getRequestValues().getBody()).isNull();

    this.service.executeMono(null);

    assertThat(getRequestValues().getBodyValue()).isNull();
    assertThat(getRequestValues().getBody()).isNull();
  }

  private HttpRequestValues getRequestValues() {
    return this.client.getRequestValues();
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
