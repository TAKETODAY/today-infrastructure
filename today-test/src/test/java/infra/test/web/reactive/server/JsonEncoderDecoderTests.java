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

package infra.test.web.reactive.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.ResourceHttpMessageReader;
import infra.http.codec.ResourceHttpMessageWriter;
import infra.http.codec.json.Jackson2JsonDecoder;
import infra.http.codec.json.Jackson2JsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/9 20:47
 */
class JsonEncoderDecoderTests {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final HttpMessageWriter<?> jacksonMessageWriter = new EncoderHttpMessageWriter<>(
          new Jackson2JsonEncoder(objectMapper));

  private static final HttpMessageReader<?> jacksonMessageReader = new DecoderHttpMessageReader<>(
          new Jackson2JsonDecoder(objectMapper));

  @Test
  void fromWithEmptyWriters() {
    assertThat(JsonEncoderDecoder.from(List.of(), List.of(jacksonMessageReader))).isNull();
  }

  @Test
  void fromWithEmptyReaders() {
    assertThat(JsonEncoderDecoder.from(List.of(jacksonMessageWriter), List.of())).isNull();
  }

  @Test
  void fromWithSuitableWriterAndNoReader() {
    assertThat(JsonEncoderDecoder.from(List.of(jacksonMessageWriter), List.of(new ResourceHttpMessageReader()))).isNull();
  }

  @Test
  void fromWithSuitableReaderAndNoWriter() {
    assertThat(JsonEncoderDecoder.from(List.of(new ResourceHttpMessageWriter()), List.of(jacksonMessageReader))).isNull();
  }

  @Test
  void fromWithNoSuitableReaderAndWriter() {
    JsonEncoderDecoder jsonEncoderDecoder = JsonEncoderDecoder.from(
            List.of(new ResourceHttpMessageWriter(), jacksonMessageWriter),
            List.of(new ResourceHttpMessageReader(), jacksonMessageReader));
    assertThat(jsonEncoderDecoder).isNotNull();
    assertThat(jsonEncoderDecoder.encoder()).isInstanceOf(Jackson2JsonEncoder.class);
    assertThat(jsonEncoderDecoder.decoder()).isInstanceOf(Jackson2JsonDecoder.class);
  }

}