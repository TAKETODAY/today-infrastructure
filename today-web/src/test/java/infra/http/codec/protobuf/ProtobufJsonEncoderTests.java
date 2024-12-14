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

package infra.http.codec.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.core.testfixture.io.buffer.DataBufferTestUtils;
import infra.http.MediaType;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.core.ResolvableType.forClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProtobufJsonEncoder}.
 *
 * @author Brian Clozel
 */
class ProtobufJsonEncoderTests extends AbstractEncoderTests<ProtobufJsonEncoder> {

  private Msg msg1 =
          Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

  private Msg msg2 =
          Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();

  public ProtobufJsonEncoderTests() {
    super(new ProtobufJsonEncoder(JsonFormat.printer().omittingInsignificantWhitespace()));
  }

  @Override
  @Test
  protected void canEncode() throws Exception {
    assertThat(this.encoder.canEncode(forClass(Msg.class), null)).isFalse();
    assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_NDJSON)).isFalse();
    assertThat(this.encoder.canEncode(forClass(Object.class), MediaType.APPLICATION_JSON)).isFalse();
  }

  @Override
  @Test
  protected void encode() throws Exception {
    Mono<Message> input = Mono.just(this.msg1);
    ResolvableType inputType = forClass(Msg.class);

    testEncode(input, inputType, MediaType.APPLICATION_JSON, null, step -> step
            .assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"))
            .verifyComplete());
    testEncodeError(input, inputType, MediaType.APPLICATION_JSON, null);
    testEncodeCancel(input, inputType, MediaType.APPLICATION_JSON, null);
  }

  @Test
  void encodeEmptyMono() {
    Mono<Message> input = Mono.empty();
    ResolvableType inputType = forClass(Msg.class);
    Flux<DataBuffer> result = this.encoder.encode(input, this.bufferFactory, inputType,
            MediaType.APPLICATION_JSON, null);
    StepVerifier.create(result)
            .verifyComplete();
  }

  @Test
  void encodeStream() {
    Flux<Message> input = Flux.just(this.msg1, this.msg2);
    ResolvableType inputType = forClass(Msg.class);

    testEncode(input, inputType, MediaType.APPLICATION_JSON, null, step -> step
            .assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "[{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"))
            .assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, ",{\"foo\":\"Bar\",\"blah\":{\"blah\":456}}"))
            .assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "]"))
            .verifyComplete());
  }

  @Test
  void encodeEmptyFlux() {
    Flux<Message> input = Flux.empty();
    ResolvableType inputType = forClass(Msg.class);
    Flux<DataBuffer> result = this.encoder.encode(input, this.bufferFactory, inputType,
            MediaType.APPLICATION_JSON, null);
    StepVerifier.create(result)
            .assertNext(buffer -> assertBufferEqualsJson(buffer, "["))
            .assertNext(buffer -> assertBufferEqualsJson(buffer, "]"))
            .verifyComplete();
  }

  private void assertBufferEqualsJson(DataBuffer actual, String expected) {
    byte[] bytes = DataBufferTestUtils.dumpBytes(actual);
    String json = new String(bytes, StandardCharsets.UTF_8);
    assertThat(json).isEqualTo(expected);
    actual.release();
  }

}
