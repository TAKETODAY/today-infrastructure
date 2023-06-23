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

package cn.taketoday.http.codec;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.json.AbstractLeakCheckingTests;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpResponse;
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
            ResolvableType.forInstance(new DefaultMultiValueMap<String, String>()),
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
    MultiValueMap<String, String> body = new DefaultMultiValueMap<>(new LinkedHashMap<>());
    body.set("name 1", "value 1");
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
    assertThat(headers.getContentType().toString()).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
    assertThat(headers.getContentLength()).isEqualTo(expected.length());
  }

  private Consumer<DataBuffer> stringConsumer(String expected) {
    return dataBuffer -> {
      String value = dataBuffer.toString(UTF_8);
      DataBufferUtils.release(dataBuffer);
      assertThat(value).isEqualTo(expected);
    };
  }

}
