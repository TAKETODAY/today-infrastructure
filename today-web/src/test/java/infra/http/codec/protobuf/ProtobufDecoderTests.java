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

package infra.http.codec.protobuf;

import com.google.protobuf.Message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import infra.core.ResolvableType;
import infra.core.codec.DecodingException;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractDecoderTests;
import infra.http.MediaType;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.core.ResolvableType.forClass;
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
    assertThat(this.decoder.canDecode(forClass(Msg.class), null)).isTrue();
    assertThat(this.decoder.canDecode(forClass(Msg.class), PROTOBUF_MIME_TYPE)).isTrue();
    assertThat(this.decoder.canDecode(forClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(this.decoder.canDecode(forClass(Msg.class), MediaType.APPLICATION_JSON)).isFalse();
    assertThat(this.decoder.canDecode(forClass(Object.class), PROTOBUF_MIME_TYPE)).isFalse();
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
                buffer.release();
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
                buffer.release();
                return Mono.error(e);
              }
            }))
            .flatMap(buffer -> {
              int len = buffer.readableBytes() / 2;
              Flux<DataBuffer> result = Flux.just(
                      buffer.slice(0, len).retain(),
                      buffer.slice(len, buffer.readableBytes() - len).retain()
              );
              buffer.release();
              return result;
            });

    testDecode(input, Msg.class, step -> step
            .expectNext(this.testMsg1)
            .expectNext(this.testMsg2)
            .verifyComplete());
  }

  @Test
  public void decodeSplitMessageSize() {
    this.decoder.setMaxMessageSize(100009);
    Msg bigMessage = Msg.newBuilder().setFoo("azertyuiop".repeat(10000))
            .setBlah(secondMsg2).build();

    Flux<DataBuffer> input = Flux.just(bigMessage, bigMessage)
            .flatMap(msg -> Mono.defer(() -> {
              DataBuffer buffer = this.bufferFactory.allocateBuffer();
              try {
                msg.writeDelimitedTo(buffer.asOutputStream());
                return Mono.just(buffer);
              }
              catch (IOException e) {
                buffer.release();
                return Mono.error(e);
              }
            }))
            .flatMap(buffer -> {
              int len = 2;
              Flux<DataBuffer> result = Flux.just(buffer.retainedSlice(0, len),
                      buffer.retainedSlice(len, buffer.readableBytes() - len));
              buffer.release();
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

    ResolvableType elementType = forClass(Msg.class);
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
