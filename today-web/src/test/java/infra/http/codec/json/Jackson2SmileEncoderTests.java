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

package infra.http.codec.json;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.codec.Pojo;
import infra.http.codec.ServerSentEvent;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.http.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Jackson2SmileEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2SmileEncoderTests extends AbstractEncoderTests<Jackson2SmileEncoder> {

  private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
  private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

  private final Jackson2SmileEncoder encoder = new Jackson2SmileEncoder();

  private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();

  public Jackson2SmileEncoderTests() {
    super(new Jackson2SmileEncoder());

  }

  @Override
  @Test
  public void canEncode() {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, SMILE_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, STREAM_SMILE_MIME_TYPE)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

    //
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
  }

  @Test
  public void canNotEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();

    ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
    assertThat(this.encoder.canEncode(sseType, SMILE_MIME_TYPE)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    List<Pojo> list = Arrays.asList(
            new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar"));

    Flux<Pojo> input = Flux.fromIterable(list);

    testEncode(input, Pojo.class, step -> step
            .consumeNextWith(dataBuffer -> {
              try {
                Object actual = this.mapper.reader().forType(List.class)
                        .readValue(dataBuffer.asInputStream());
                assertThat(actual).isEqualTo(list);
              }
              catch (IOException e) {
                throw new UncheckedIOException(e);
              }
              finally {
                dataBuffer.release();
              }
            }));
  }

  @Test
  public void encodeError() throws Exception {
    Mono<Pojo> input = Mono.error(new InputException());

    testEncode(input, Pojo.class, step -> step
            .expectError(InputException.class)
            .verify());

  }

  @Test
  public void encodeAsStream() throws Exception {
    Pojo pojo1 = new Pojo("foo", "bar");
    Pojo pojo2 = new Pojo("foofoo", "barbar");
    Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
    Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
    ResolvableType type = ResolvableType.forClass(Pojo.class);

    Flux<DataBuffer> result = this.encoder
            .encode(input, bufferFactory, type, STREAM_SMILE_MIME_TYPE, null);

    Mono<MappingIterator<Pojo>> joined = DataBufferUtils.join(result)
            .map(buffer -> {
              try {
                return this.mapper.reader().forType(Pojo.class).readValues(buffer.asInputStream(true));
              }
              catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });

    StepVerifier.create(joined)
            .assertNext(iter -> assertThat(iter).toIterable().contains(pojo1, pojo2, pojo3))
            .verifyComplete();
  }

}
