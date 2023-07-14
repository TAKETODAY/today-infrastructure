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

import java.util.List;
import java.util.Optional;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.service.annotation.GetExchange;
import cn.taketoday.web.service.annotation.HttpExchange;
import cn.taketoday.web.service.annotation.PostExchange;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.http.MediaType.APPLICATION_CBOR_VALUE;
import static cn.taketoday.http.MediaType.APPLICATION_JSON_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceMethod} with
 * {@link TestExchangeAdapter} and {@link TestReactorExchangeAdapter}.
 *
 * <p>The tests do not create or invoke {@code HttpServiceMethod} directly but
 * rather use {@link HttpServiceProxyFactory} to create a service proxy in order to
 * use a strongly typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
class HttpServiceMethodTests {

  private static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() { };

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final TestReactorExchangeAdapter reactorClient = new TestReactorExchangeAdapter();

  private final HttpServiceProxyFactory proxyFactory =
          HttpServiceProxyFactory.builder().exchangeAdapter(this.client).build();

  private final HttpServiceProxyFactory reactorProxyFactory =
          HttpServiceProxyFactory.builder().exchangeAdapter(this.reactorClient).build();

  @Test
  void service() {
    Service service = this.proxyFactory.createClient(Service.class);

    service.execute();

    HttpHeaders headers = service.getHeaders();
    assertThat(headers).isNotNull();

    String body = service.getBody();
    assertThat(body).isEqualTo(this.client.getInvokedMethodName());

    Optional<String> optional = service.getBodyOptional();
    assertThat(optional.get()).isEqualTo("exchangeForBody");

    ResponseEntity<String> entity = service.getEntity();
    assertThat(entity.getBody()).isEqualTo("exchangeForEntity");

    ResponseEntity<Void> voidEntity = service.getVoidEntity();
    assertThat(voidEntity.getBody()).isNull();

    List<String> list = service.getList();
    assertThat(list.get(0)).isEqualTo("exchangeForBody");
  }

  @Test
  void reactorService() {
    ReactorService service = this.reactorProxyFactory.createClient(ReactorService.class);

    Mono<Void> voidMono = service.execute();
    StepVerifier.create(voidMono).verifyComplete();
    verifyReactorClientInvocation("exchangeForMono", null);

    Mono<HttpHeaders> headersMono = service.getHeaders();
    StepVerifier.create(headersMono).expectNextCount(1).verifyComplete();
    verifyReactorClientInvocation("exchangeForHeadersMono", null);

    Mono<String> body = service.getBody();
    StepVerifier.create(body).expectNext("exchangeForBodyMono").verifyComplete();
    verifyReactorClientInvocation("exchangeForBodyMono", BODY_TYPE);

    Flux<String> fluxBody = service.getFluxBody();
    StepVerifier.create(fluxBody).expectNext("exchange", "For", "Body", "Flux").verifyComplete();
    verifyReactorClientInvocation("exchangeForBodyFlux", BODY_TYPE);

    Mono<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
    StepVerifier.create(voidEntity).expectNext(ResponseEntity.ok().build()).verifyComplete();
    verifyReactorClientInvocation("exchangeForBodilessEntityMono", null);

    Mono<ResponseEntity<String>> entity = service.getEntity();
    StepVerifier.create(entity).expectNext(ResponseEntity.ok("exchangeForEntityMono"));
    verifyReactorClientInvocation("exchangeForEntityMono", BODY_TYPE);

    Mono<ResponseEntity<Flux<String>>> fluxEntity = service.getFluxEntity();
    StepVerifier.create(fluxEntity.flatMapMany(HttpEntity::getBody))
            .expectNext("exchange", "For", "Entity", "Flux")
            .verifyComplete();
    verifyReactorClientInvocation("exchangeForEntityFlux", BODY_TYPE);

    assertThat(service.getDefaultMethodValue()).isEqualTo("default value");
  }

  @Test
  void rxJavaService() {
    RxJavaService service = this.reactorProxyFactory.createClient(RxJavaService.class);
    Completable completable = service.execute();
    assertThat(completable).isNotNull();

    Single<HttpHeaders> headersSingle = service.getHeaders();
    assertThat(headersSingle.blockingGet()).isNotNull();

    Single<String> bodySingle = service.getBody();
    assertThat(bodySingle.blockingGet()).isEqualTo("exchangeForBodyMono");

    Flowable<String> bodyFlow = service.getFlowableBody();
    assertThat(bodyFlow.toList().blockingGet()).asList().containsExactly("exchange", "For", "Body", "Flux");

    Single<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
    assertThat(voidEntity.blockingGet().getBody()).isNull();

    Single<ResponseEntity<String>> entitySingle = service.getEntity();
    assertThat(entitySingle.blockingGet().getBody()).isEqualTo("exchangeForEntityMono");

    Single<ResponseEntity<Flowable<String>>> entityFlow = service.getFlowableEntity();
    Flowable<String> body = (entityFlow.blockingGet()).getBody();
    assertThat(body.toList().blockingGet()).containsExactly("exchange", "For", "Entity", "Flux");
  }

  @Test
  void methodAnnotatedService() {
    MethodLevelAnnotatedService service = this.proxyFactory.createClient(MethodLevelAnnotatedService.class);

    service.performGet();

    HttpRequestValues requestValues = this.client.getRequestValues();
    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
    assertThat(requestValues.getUriTemplate()).isEmpty();
    assertThat(requestValues.getHeaders().getContentType()).isNull();
    assertThat(requestValues.getHeaders().getAccept()).isEmpty();

    service.performPost();

    requestValues = this.client.getRequestValues();
    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(requestValues.getUriTemplate()).isEqualTo("/url");
    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
  }

  @Test
  void typeAndMethodAnnotatedService() {
    HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(this.client)
            .embeddedValueResolver(value -> (value.equals("${baseUrl}") ? "/base" : value))
            .build();

    MethodLevelAnnotatedService service = proxyFactory.createClient(TypeAndMethodLevelAnnotatedService.class);

    service.performGet();

    HttpRequestValues requestValues = this.client.getRequestValues();
    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
    assertThat(requestValues.getUriTemplate()).isEqualTo("/base");
    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_CBOR);
    assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_CBOR);

    service.performPost();

    requestValues = this.client.getRequestValues();
    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(requestValues.getUriTemplate()).isEqualTo("/base/url");
    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
  }

  protected void verifyReactorClientInvocation(String methodName, @Nullable ParameterizedTypeReference<?> expectedBodyType) {
    assertThat(this.reactorClient.getInvokedMethodName()).isEqualTo(methodName);
    assertThat(this.reactorClient.getBodyType()).isEqualTo(expectedBodyType);
  }

  @SuppressWarnings("unused")
  private interface Service {

    @GetExchange
    void execute();

    @GetExchange
    HttpHeaders getHeaders();

    @GetExchange
    String getBody();

    @GetExchange
    Optional<String> getBodyOptional();

    @GetExchange
    ResponseEntity<Void> getVoidEntity();

    @GetExchange
    ResponseEntity<String> getEntity();

    @GetExchange
    List<String> getList();

  }

  private interface ReactorService {

    @GetExchange
    Mono<Void> execute();

    @GetExchange
    Mono<HttpHeaders> getHeaders();

    @GetExchange
    Mono<String> getBody();

    @GetExchange
    Flux<String> getFluxBody();

    @GetExchange
    Mono<ResponseEntity<Void>> getVoidEntity();

    @GetExchange
    Mono<ResponseEntity<String>> getEntity();

    @GetExchange
    Mono<ResponseEntity<Flux<String>>> getFluxEntity();

    default String getDefaultMethodValue() {
      return "default value";
    }

  }

  @SuppressWarnings("unused")
  private interface RxJavaService {

    @GetExchange
    Completable execute();

    @GetExchange
    Single<HttpHeaders> getHeaders();

    @GetExchange
    Single<String> getBody();

    @GetExchange
    Flowable<String> getFlowableBody();

    @GetExchange
    Single<ResponseEntity<Void>> getVoidEntity();

    @GetExchange
    Single<ResponseEntity<String>> getEntity();

    @GetExchange
    Single<ResponseEntity<Flowable<String>>> getFlowableEntity();

  }

  @SuppressWarnings("unused")
  private interface MethodLevelAnnotatedService {

    @GetExchange
    void performGet();

    @PostExchange(url = "/url", contentType = APPLICATION_JSON_VALUE, accept = APPLICATION_JSON_VALUE)
    void performPost();

  }

  @SuppressWarnings("unused")
  @HttpExchange(url = "${baseUrl}", contentType = APPLICATION_CBOR_VALUE, accept = APPLICATION_CBOR_VALUE)
  private interface TypeAndMethodLevelAnnotatedService extends MethodLevelAnnotatedService {
  }

}
