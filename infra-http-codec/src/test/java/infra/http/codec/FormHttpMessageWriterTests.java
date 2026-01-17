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

package infra.http.codec;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.MappingMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
public class FormHttpMessageWriterTests extends AbstractLeakCheckingTests {

  private final FormHttpMessageWriter writer = new FormHttpMessageWriter();

  @Test
  public void canWrite() {
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

    // No generic information
    assertThat(this.writer.canWrite(
            ResolvableType.forInstance(new MappingMultiValueMap<String, String>()),
            MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            null)).isFalse();

    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, Object.class, String.class),
            null)).isFalse();

    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
            MediaType.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  public void writeForm() {
    MultiValueMap<String, String> body = new MappingMultiValueMap<>(new LinkedHashMap<>());
    body.setOrRemove("name 1", "value 1");
    body.add("name 2", "value 2+1");
    body.add("name 2", "value 2+2");
    body.add("name 3", null);
    MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);
    this.writer.write(Mono.just(body), null, MediaType.APPLICATION_FORM_URLENCODED, response, null).block();

    String expected = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
    StepVerifier.create(response.getBody())
            .consumeNextWith(stringConsumer(expected))
            .expectComplete()
            .verify();
    HttpHeaders headers = response.getHeaders();
    assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
    assertThat(headers.getContentLength()).isEqualTo(expected.length());
  }

  private Consumer<DataBuffer> stringConsumer(String expected) {
    return dataBuffer -> {
      String value = dataBuffer.toString(UTF_8);
      dataBuffer.release();
      assertThat(value).isEqualTo(expected);
    };
  }

}
