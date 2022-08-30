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

package cn.taketoday.web.reactive.function.client;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.TypeReference;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.web.reactive.function.BodyInserter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.http.HttpMethod.DELETE;
import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.http.HttpMethod.OPTIONS;
import static cn.taketoday.http.HttpMethod.POST;
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
            .headers(httpHeaders -> httpHeaders.set("foo", "baar"))
            .cookies(cookies -> cookies.set("baz", "quux"))
            .build();

    assertThat(result.url()).isEqualTo(DEFAULT_URL);
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

    ExchangeStrategies strategies = mock(ExchangeStrategies.class);
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
  public void url() throws URISyntaxException {
    URI url1 = new URI("https://example.com/foo");
    URI url2 = new URI("https://example.com/bar");
    ClientRequest.Builder builder = ClientRequest.create(DELETE, url1);
    assertThat(builder.build().url()).isEqualTo(url1);

    builder.url(url2);
    assertThat(builder.build().url()).isEqualTo(url2);
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
    ExchangeStrategies strategies = mock(ExchangeStrategies.class);

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

    ExchangeStrategies strategies = mock(ExchangeStrategies.class);
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

    ExchangeStrategies strategies = mock(ExchangeStrategies.class);
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
    TypeReference<String> typeReference = new TypeReference<>() { };
    ClientRequest result = ClientRequest.create(POST, DEFAULT_URL).body(publisher, typeReference).build();

    List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));

    ExchangeStrategies strategies = mock(ExchangeStrategies.class);
    given(strategies.messageWriters()).willReturn(messageWriters);

    MockClientHttpRequest request = new MockClientHttpRequest(GET, "/");
    result.writeTo(request, strategies).block();
    assertThat(request.getBody()).isNotNull();

    StepVerifier.create(request.getBody()).expectNextCount(1).verifyComplete();
  }

}
