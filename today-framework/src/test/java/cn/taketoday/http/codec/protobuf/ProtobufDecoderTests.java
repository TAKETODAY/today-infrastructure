/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.codec.protobuf;

import com.google.protobuf.Message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.AbstractDecoderTests;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.MediaType;
import cn.taketoday.protobuf.Msg;
import cn.taketoday.protobuf.SecondMsg;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.core.ResolvableType.fromClass;
import static cn.taketoday.core.io.buffer.DataBufferUtils.release;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ProtobufDecoder}.
 *
 * @author Sebastien Deleuze
 */
public class ProtobufDecoderTests extends AbstractDecoderTests<ProtobufDecoder> {

  private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

  private final SecondMsg secondMsg = SecondMsg.newBuilder().setBlah(123).build();

  private final Msg testMsg1 = Msg.newBuilder().setFoo("Foo").setBlah(secondMsg).build();

  private final SecondMsg secondMsg2 = SecondMsg.newBuilder().setBlah(456).build();

  private final Msg testMsg2 = Msg.newBuilder().setFoo("Bar").setBlah(secondMsg2).build();

  public ProtobufDecoderTests() {
    super(new ProtobufDecoder());
  }

  @Test
  public void extensionRegistryNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ProtobufDecoder(null));
  }

  @Override
  @Test
  public void canDecode() {
    assertThat(this.decoder.canDecode(fromClass(Msg.class), null)).isTrue();
    assertThat(this.decoder.canDecode(fromClass(Msg.class), PROTOBUF_MIME_TYPE)).isTrue();
    assertThat(this.decoder.canDecode(fromClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(this.decoder.canDecode(fromClass(Msg.class), MediaType.APPLICATION_JSON)).isFalse();
    assertThat(this.decoder.canDecode(fromClass(Object.class), PROTOBUF_MIME_TYPE)).isFalse();
  }

  @Override
  @Test
  public void decodeToMono() {
    Mono<DataBuffer> input = dataBuffer(this.testMsg1);

    testDecodeToMonoAll(input, Msg.class, step -> step
            .expectNext(this.testMsg1)
            .verifyComplete());
  }

  @Test
  public void decodeChunksToMono() {
    byte[] full = this.testMsg1.toByteArray();
    byte[] chunk1 = Arrays.copyOfRange(full, 0, full.length / 2);
    byte[] chunk2 = Arrays.copyOfRange(full, chunk1.length, full.length);

    Flux<DataBuffer> input = Flux.just(chunk1, chunk2)
            .flatMap(bytes -> Mono.defer(() -> {
              DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(bytes.length);
              dataBuffer.write(bytes);
              return Mono.just(dataBuffer);
            }));

    testDecodeToMono(input, Msg.class, step -> step
            .expectNext(this.testMsg1)
            .verifyComplete());
  }

  @Override
  @Test
  public void decode() {
    Flux<DataBuffer> input = Flux.just(this.testMsg1, this.testMsg2)
            .flatMap(msg -> Mono.defer(() -> {
              DataBuffer buffer = this.bufferFactory.allocateBuffer();
              try {
                msg.writeDelimitedTo(buffer.asOutputStream());
                return Mono.just(buffer);
              }
              catch (IOException e) {
                release(buffer);
                return Mono.error(e);
              }
            }));

    testDecodeAll(input, Msg.class, step -> step
            .expectNext(this.testMsg1)
            .expectNext(this.testMsg2)
            .verifyComplete());
  }

  @Test
  public void decodeSplitChunks() {

    Flux<DataBuffer> input = Flux.just(this.testMsg1, this.testMsg2)
            .flatMap(msg -> Mono.defer(() -> {
              DataBuffer buffer = this.bufferFactory.allocateBuffer();
              try {
                msg.writeDelimitedTo(buffer.asOutputStream());
                return Mono.just(buffer);
              }
              catch (IOException e) {
                release(buffer);
                return Mono.error(e);
              }
            }))
            .flatMap(buffer -> {
              int len = buffer.readableByteCount() / 2;
              Flux<DataBuffer> result = Flux.just(
                      DataBufferUtils.retain(buffer.slice(0, len)),
                      DataBufferUtils.retain(buffer.slice(len, buffer.readableByteCount() - len))
              );
              release(buffer);
              return result;
            });

    testDecode(input, Msg.class, step -> step
            .expectNext(this.testMsg1)
            .expectNext(this.testMsg2)
            .verifyComplete());
  }

  @Test  // SPR-17429
  public void decodeSplitMessageSize() {
    this.decoder.setMaxMessageSize(100009);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      builder.append("azertyuiop");
    }
    Msg bigMessage = Msg.newBuilder().setFoo(builder.toString()).setBlah(secondMsg2).build();

    Flux<DataBuffer> input = Flux.just(bigMessage, bigMessage)
            .flatMap(msg -> Mono.defer(() -> {
              DataBuffer buffer = this.bufferFactory.allocateBuffer();
              try {
                msg.writeDelimitedTo(buffer.asOutputStream());
                return Mono.just(buffer);
              }
              catch (IOException e) {
                release(buffer);
                return Mono.error(e);
              }
            }))
            .flatMap(buffer -> {
              int len = 2;
              Flux<DataBuffer> result = Flux.just(
                      DataBufferUtils.retain(buffer.slice(0, len)),
                      DataBufferUtils
                              .retain(buffer.slice(len, buffer.readableByteCount() - len))
              );
              release(buffer);
              return result;
            });

    testDecode(input, Msg.class, step -> step
            .expectNext(bigMessage)
            .expectNext(bigMessage)
            .verifyComplete());
  }

  @Test
  public void decodeMergedChunks() throws IOException {
    DataBuffer buffer = this.bufferFactory.allocateBuffer();
    this.testMsg1.writeDelimitedTo(buffer.asOutputStream());
    this.testMsg1.writeDelimitedTo(buffer.asOutputStream());

    ResolvableType elementType = fromClass(Msg.class);
    Flux<Message> messages = this.decoder.decode(Mono.just(buffer), elementType, null, emptyMap());

    StepVerifier.create(messages)
            .expectNext(testMsg1)
            .expectNext(testMsg1)
            .verifyComplete();
  }

  @Test
  public void exceedMaxSize() {
    this.decoder.setMaxMessageSize(1);
    Mono<DataBuffer> input = dataBuffer(this.testMsg1);

    testDecode(input, Msg.class, step -> step
            .verifyError(DecodingException.class));
  }

  private Mono<DataBuffer> dataBuffer(Msg msg) {
    return Mono.fromCallable(() -> {
      byte[] bytes = msg.toByteArray();
      DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
      buffer.write(bytes);
      return buffer;
    });
  }

}
