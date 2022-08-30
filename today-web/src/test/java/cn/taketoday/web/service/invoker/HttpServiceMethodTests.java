/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.Optional;

import cn.taketoday.core.TypeReference;
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
 * Tests for {@link HttpServiceMethod} with a test {@link TestHttpClientAdapter}
 * that stubs the client invocations.
 *
 * <p>The tests do not create or invoke {@code HttpServiceMethod} directly but
 * rather use {@link HttpServiceProxyFactory} to create a service proxy in order to
 * use a strongly typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 */
public class HttpServiceMethodTests {

  private static final TypeReference<String> BODY_TYPE = new TypeReference<>() { };

  private final TestHttpClientAdapter client = new TestHttpClientAdapter();

  private HttpServiceProxyFactory proxyFactory;

  @BeforeEach
  void setUp() throws Exception {
    this.proxyFactory = new HttpServiceProxyFactory(this.client);
    this.proxyFactory.afterPropertiesSet();
  }

  @Test
  void reactorService() {
    ReactorService service = this.proxyFactory.createClient(ReactorService.class);

    Mono<Void> voidMono = service.execute();
    StepVerifier.create(voidMono).verifyComplete();
    verifyClientInvocation("requestToVoid", null);

    Mono<HttpHeaders> headersMono = service.getHeaders();
    StepVerifier.create(headersMono).expectNextCount(1).verifyComplete();
    verifyClientInvocation("requestToHeaders", null);

    Mono<String> body = service.getBody();
    StepVerifier.create(body).expectNext("requestToBody").verifyComplete();
    verifyClientInvocation("requestToBody", BODY_TYPE);

    Flux<String> fluxBody = service.getFluxBody();
    StepVerifier.create(fluxBody).expectNext("request", "To", "Body", "Flux").verifyComplete();
    verifyClientInvocation("requestToBodyFlux", BODY_TYPE);

    Mono<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
    StepVerifier.create(voidEntity).expectNext(ResponseEntity.ok().build()).verifyComplete();
    verifyClientInvocation("requestToBodilessEntity", null);

    Mono<ResponseEntity<String>> entity = service.getEntity();
    StepVerifier.create(entity).expectNext(ResponseEntity.ok("requestToEntity"));
    verifyClientInvocation("requestToEntity", BODY_TYPE);

    Mono<ResponseEntity<Flux<String>>> fluxEntity = service.getFluxEntity();
    StepVerifier.create(fluxEntity.flatMapMany(HttpEntity::getBody)).expectNext("request", "To", "Entity", "Flux").verifyComplete();
    verifyClientInvocation("requestToEntityFlux", BODY_TYPE);

    assertThat(service.getDefaultMethodValue()).isEqualTo("default value");
  }

  @Test
  void rxJavaService() {
    RxJavaService service = this.proxyFactory.createClient(RxJavaService.class);
    Completable completable = service.execute();
    assertThat(completable).isNotNull();

    Single<HttpHeaders> headersSingle = service.getHeaders();
    assertThat(headersSingle.blockingGet()).isNotNull();

    Single<String> bodySingle = service.getBody();
    assertThat(bodySingle.blockingGet()).isEqualTo("requestToBody");

    Flowable<String> bodyFlow = service.getFlowableBody();
    assertThat(bodyFlow.toList().blockingGet()).asList().containsExactly("request", "To", "Body", "Flux");

    Single<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
    assertThat(voidEntity.blockingGet().getBody()).isNull();

    Single<ResponseEntity<String>> entitySingle = service.getEntity();
    assertThat(entitySingle.blockingGet().getBody()).isEqualTo("requestToEntity");

    Single<ResponseEntity<Flowable<String>>> entityFlow = service.getFlowableEntity();
    Flowable<String> body = (entityFlow.blockingGet()).getBody();
    assertThat(body.toList().blockingGet()).containsExactly("request", "To", "Entity", "Flux");
  }

  @Test
  void blockingService() {
    BlockingService service = this.proxyFactory.createClient(BlockingService.class);

    service.execute();

    HttpHeaders headers = service.getHeaders();
    assertThat(headers).isNotNull();

    String body = service.getBody();
    assertThat(body).isEqualTo("requestToBody");

    Optional<String> optional = service.getBodyOptional();
    assertThat(optional).contains("requestToBody");

    ResponseEntity<String> entity = service.getEntity();
    assertThat(entity.getBody()).isEqualTo("requestToEntity");

    ResponseEntity<Void> voidEntity = service.getVoidEntity();
    assertThat(voidEntity.getBody()).isNull();
  }

  @Test
  void methodAnnotatedService() {
    MethodLevelAnnotatedService service = this.proxyFactory.createClient(MethodLevelAnnotatedService.class);

    service.performGet();

    HttpRequestValues requestValues = this.client.getRequestValues();
    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
    assertThat(requestValues.getUriTemplate()).isEqualTo("");
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
  void typeAndMethodAnnotatedService() throws Exception {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
    proxyFactory.setEmbeddedValueResolver(value -> (value.equals("${baseUrl}") ? "/base" : value));
    proxyFactory.afterPropertiesSet();

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

  private void verifyClientInvocation(String methodName, @Nullable TypeReference<?> expectedBodyType) {
    assertThat(this.client.getInvokedMethodName()).isEqualTo(methodName);
    assertThat(this.client.getBodyType()).isEqualTo(expectedBodyType);
  }

  @SuppressWarnings("unused")
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
  private interface BlockingService {

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
