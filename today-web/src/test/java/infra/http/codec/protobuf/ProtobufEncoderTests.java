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

package infra.http.codec.protobuf;

import com.google.protobuf.Message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.MediaType;
import infra.http.codec.json.AbstractEncoderTests;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static infra.core.ResolvableType.forClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProtobufEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class ProtobufEncoderTests extends AbstractEncoderTests<ProtobufEncoder> {

  private final static MimeType PROTOBUF_MIME_TYPE = new MimeType("application", "x-protobuf");

  private Msg msg1 =
          Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

  private Msg msg2 =
          Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();

  public ProtobufEncoderTests() {
    super(new ProtobufEncoder());
  }

  @Override
  @Test
  public void canEncode() {
    assertThat(this.encoder.canEncode(forClass(Msg.class), null)).isTrue();
    assertThat(this.encoder.canEncode(forClass(Msg.class), PROTOBUF_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_JSON)).isFalse();
    assertThat(this.encoder.canEncode(forClass(Object.class), PROTOBUF_MIME_TYPE)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Mono<Message> input = Mono.just(this.msg1);

    testEncodeAll(input, Msg.class, step -> step
            .consumeNextWith(dataBuffer -> {
              try {
                assertThat(Msg.parseFrom(dataBuffer.asInputStream())).isEqualTo(this.msg1);

              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
              finally {
                DataBufferUtils.release(dataBuffer);
              }
            })
            .verifyComplete());
  }

  @Test
  public void encodeStream() {
    Flux<Message> input = Flux.just(this.msg1, this.msg2);

    testEncodeAll(input, Msg.class, step -> step
            .consumeNextWith(expect(this.msg1))
            .consumeNextWith(expect(this.msg2))
            .verifyComplete());
  }

  protected final Consumer<DataBuffer> expect(Msg msg) {
    return dataBuffer -> {
      try {
        assertThat(Msg.parseDelimitedFrom(dataBuffer.asInputStream())).isEqualTo(msg);

      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      finally {
        DataBufferUtils.release(dataBuffer);
      }
    };
  }
}
