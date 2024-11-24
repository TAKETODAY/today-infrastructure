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

package infra.web.client.reactive;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import infra.core.ParameterizedTypeReference;
import infra.core.codec.CharSequenceEncoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.client.reactive.ClientHttpRequest;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.HttpMessageWriter;
import infra.web.reactive.function.BodyInserter;
import infra.web.testfixture.http.client.reactive.MockClientHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.http.HttpMethod.DELETE;
import static infra.http.HttpMethod.GET;
import static infra.http.HttpMethod.OPTIONS;
import static infra.http.HttpMethod.POST;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultClientRequestBuilder}.
 *
 * @author Arjen Poutsma
 */
public class DefaultClientRequestBuilderTests {

  private static final URI DEFAULT_URL = URI.create("https://example.com");

  @Test
  public void from() {
    ClientRequest other = ClientRequest.create(GET, DEFAULT_URL)
            .header("foo", "bar")
            .cookie("baz", "qux")
            .attribute("attributeKey", "attributeValue")
            .attribute("anotherAttributeKey", "anotherAttributeValue")
            .httpRequest(request -> { })
            .build();

    ClientRequest result = ClientRequest.from(other)
            .headers(httpHeaders -> httpHeaders.setOrRemove("foo", "baar"))
            .cookies(cookies -> cookies.setOrRemove("baz", "quux"))
            .build();

    assertThat(result.uri()).isEqualTo(DEFAULT_URL);
    assertThat(result.method()).isEqualTo(GET);
    assertThat(result.headers().size()).isEqualTo(1);
    assertThat(result.headers().getFirst("foo")).isEqualTo("baar");
    assertThat(result.cookies().size()).isEqualTo(1);
    assertThat(result.cookies().getFirst("baz")).isEqualTo("quux");
    assertThat(result.httpRequest()).isNotNull();
    assertThat(result.attributes().get("attributeKey")).isEqualTo("attributeValue");
    assertThat(result.attributes().get("anotherAttributeKey")).isEqualTo("anotherAttributeValue");
  }

  @Test
  public void fromCopiesBody() {
    String body = "foo";
    BodyInserter<String, ClientHttpRequest> inserter = (response, strategies) -> {
      byte[] bodyBytes = body.getBytes(UTF_8);
      DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);
      return response.writeWith(Mono.just(buffer));
    };

    ClientRequest other = ClientRequest.create(POST, DEFAULT_URL).body(inserter).build();
    ClientRequest result = ClientRequest.from(other).build();

    List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

    ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);
    given(strategies.messageWriters()).willReturn(messageWriters);

    MockClientHttpRequest request = new MockClientHttpRequest(POST, "/");
    result.writeTo(request, strategies).block();

    String copiedBody = request.getBodyAsString().block();

    assertThat(copiedBody).isEqualTo("foo");
  }

  @Test
  public void method() {
    ClientRequest.Builder builder = ClientRequest.create(DELETE, DEFAULT_URL);
    assertThat(builder.build().method()).isEqualTo(DELETE);

    builder.method(OPTIONS);
    assertThat(builder.build().method()).isEqualTo(OPTIONS);
  }

  @Test
  public void uri() throws URISyntaxException {
    URI url1 = new URI("https://example.com/foo");
    URI url2 = new URI("https://example.com/bar");
    ClientRequest.Builder builder = ClientRequest.create(DELETE, url1);
    assertThat(builder.build().uri()).isEqualTo(url1);

    builder.uri(url2);
    assertThat(builder.build().uri()).isEqualTo(url2);
  }

  @Test
  public void cookie() {
    ClientRequest result = ClientRequest.create(GET, DEFAULT_URL).cookie("foo", "bar").build();
    assertThat(result.cookies().getFirst("foo")).isEqualTo("bar");
  }

  @Test
  public void build() {
    ClientRequest result = ClientRequest.create(GET, DEFAULT_URL)
            .header("MyKey", "MyValue")
            .cookie("foo", "bar")
            .httpRequest(request -> {
              MockClientHttpRequest nativeRequest = request.getNativeRequest();
              nativeRequest.getHeaders().add("MyKey2", "MyValue2");
            })
            .build();

    MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
    ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);

    result.writeTo(request, strategies).block();

    assertThat(request.getHeaders().getFirst("MyKey")).isEqualTo("MyValue");
    assertThat(request.getHeaders().getFirst("MyKey2")).isEqualTo("MyValue2");
    assertThat(request.getCookies().getFirst("foo").getValue()).isEqualTo("bar");

    StepVerifier.create(request.getBody()).expectComplete().verify();
  }

  @Test
  public void bodyInserter() {
    String body = "foo";
    BodyInserter<String, ClientHttpRequest> inserter = (response, strategies) -> {
      byte[] bodyBytes = body.getBytes(UTF_8);
      DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);

      return response.writeWith(Mono.just(buffer));
    };

    ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(inserter).build();

    List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

    ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);
    given(strategies.messageWriters()).willReturn(messageWriters);

    MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
    result.writeTo(request, strategies).block();
    assertThat(request.getBody()).isNotNull();

    StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
  }

  @Test
  public void bodyClass() {
    String body = "foo";
    Publisher<String> publisher = Mono.just(body);
    ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(publisher, String.class).build();

    List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

    ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);
    given(strategies.messageWriters()).willReturn(messageWriters);

    MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
    result.writeTo(request, strategies).block();
    assertThat(request.getBody()).isNotNull();

    StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
  }

  @Test
  public void bodyTypeReference() {
    String body = "foo";
    Publisher<String> publisher = Mono.just(body);
    ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() { };
    ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(publisher, typeReference).build();

    List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

    ExchangeStrategies strategies = Mockito.mock(ExchangeStrategies.class);
    given(strategies.messageWriters()).willReturn(messageWriters);

    MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
    result.writeTo(request, strategies).block();
    assertThat(request.getBody()).isNotNull();

    StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
  }

}
