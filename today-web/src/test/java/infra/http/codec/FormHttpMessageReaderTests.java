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

package infra.http.codec;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.MappingMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
public class FormHttpMessageReaderTests extends AbstractLeakCheckingTests {

  private final FormHttpMessageReader reader = new FormHttpMessageReader();

  @Test
  public void canRead() {
    assertThat(this.reader.canRead(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

    assertThat(this.reader.canRead(
            ResolvableType.forInstance(new MappingMultiValueMap<String, String>()),
            MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

    assertThat(this.reader.canRead(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

    assertThat(this.reader.canRead(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, Object.class, String.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

    assertThat(this.reader.canRead(
            ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

    assertThat(this.reader.canRead(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
            MediaType.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  public void readFormAsMono() {
    String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
    MockServerHttpRequest request = request(body);
    MultiValueMap<String, String> result = this.reader.readMono(null, request, null).block();

    assertThat(result.size()).as("Invalid result").isEqualTo(3);
    assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
    List<String> values = result.get("name 2");
    assertThat(values.size()).as("Invalid result").isEqualTo(2);
    assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
    assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
    assertThat(result.getFirst("name 3")).as("Invalid result").isNull();
  }

  @Test
  public void readFormAsFlux() {
    String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
    MockServerHttpRequest request = request(body);
    MultiValueMap<String, String> result = this.reader.read(null, request, null).single().block();

    assertThat(result.size()).as("Invalid result").isEqualTo(3);
    assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
    List<String> values = result.get("name 2");
    assertThat(values.size()).as("Invalid result").isEqualTo(2);
    assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
    assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
    assertThat(result.getFirst("name 3")).as("Invalid result").isNull();
  }

  @Test
  public void readFormError() {
    DataBuffer fooBuffer = stringBuffer("name=value");
    Flux<DataBuffer> body =
            Flux.just(fooBuffer).concatWith(Flux.error(new RuntimeException()));
    MockServerHttpRequest request = request(body);

    Flux<MultiValueMap<String, String>> result = this.reader.read(null, request, null);
    StepVerifier.create(result)
            .expectError()
            .verify();
  }

  private MockServerHttpRequest request(String body) {
    return request(Mono.just(stringBuffer(body)));
  }

  private MockServerHttpRequest request(Publisher<? extends DataBuffer> body) {
    return MockServerHttpRequest
            .method(HttpMethod.GET, "/")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(body);
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

}
