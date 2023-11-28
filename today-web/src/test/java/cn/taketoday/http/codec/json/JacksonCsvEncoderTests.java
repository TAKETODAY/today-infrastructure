/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.Pojo;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractJackson2Encoder} for the CSV variant and how resources are managed.
 *
 * @author Brian Clozel
 */
class JacksonCsvEncoderTests extends AbstractEncoderTests<JacksonCsvEncoderTests.JacksonCsvEncoder> {

  public JacksonCsvEncoderTests() {
    super(new JacksonCsvEncoder());
  }

  @Test
  @Override
  public void canEncode() throws Exception {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, JacksonCsvEncoder.TEXT_CSV)).isTrue();
  }

  @Test
  @Override
  public void encode() throws Exception {
    Flux<Object> input = Flux.just(new Pojo("spring", "framework"),
            new Pojo("spring", "data"),
            new Pojo("spring", "boot"));

    testEncode(input, Pojo.class, step -> step
            .consumeNextWith(expectString("bar,foo\nframework,spring\n"))
            .consumeNextWith(expectString("data,spring\n"))
            .consumeNextWith(expectString("boot,spring\n"))
            .verifyComplete());
  }

  // this test did not fail directly but logged a NullPointerException dropped by the reactive pipeline
  @Test
  void encodeEmptyFlux() {
    Flux<Object> input = Flux.empty();
    testEncode(input, Pojo.class, step -> step.verifyComplete());
  }

  static class JacksonCsvEncoder extends AbstractJackson2Encoder {
    public static final MediaType TEXT_CSV = new MediaType("text", "csv");

    public JacksonCsvEncoder() {
      this(CsvMapper.builder().build(), TEXT_CSV);
    }

    @Override
    protected byte[] getStreamingMediaTypeSeparator(MimeType mimeType) {
      // CsvMapper emits newlines
      return new byte[0];
    }

    public JacksonCsvEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
      super(mapper, mimeTypes);
      Assert.isInstanceOf(CsvMapper.class, mapper);
      setStreamingMediaTypes(List.of(TEXT_CSV));
    }

    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {
      var mapper = (CsvMapper) getObjectMapper();
      return writer.with(mapper.schemaFor(elementType.toClass()).withHeader());
    }
  }
}
