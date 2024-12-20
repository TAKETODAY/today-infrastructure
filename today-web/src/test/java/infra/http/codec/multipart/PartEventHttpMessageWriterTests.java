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

package infra.http.codec.multipart;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.StringDecoder;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static infra.http.codec.multipart.MultipartHttpMessageWriterTests.parse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 14:11
 */
public class PartEventHttpMessageWriterTests extends AbstractLeakCheckingTests {

  private final PartEventHttpMessageWriter writer = new PartEventHttpMessageWriter();

  private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);

  @Test
  public void canWrite() {
    assertThat(this.writer.canWrite(ResolvableType.forClass(PartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(ResolvableType.forClass(FilePartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(ResolvableType.forClass(FormPartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
  }

  @Test
  void write() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);
    Mono<FormPartEvent> formPartEvent = FormPartEvent.create("text part", "text");

    Flux<FilePartEvent> filePartEvents =
            FilePartEvent.create("file part", "file.txt", MediaType.APPLICATION_OCTET_STREAM,
                    Flux.just(
                            this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
                            this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
                            this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
                    ));

    Flux<PartEvent> partEvents = Flux.concat(
            formPartEvent,
            filePartEvents
    );

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(partEvents, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
            .block(Duration.ofSeconds(5));

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(2);

    Part part = requestParts.getFirst("text part");
    assertThat(part.name()).isEqualTo("text part");
    assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    String value = decodeToString(part);
    assertThat(value).isEqualTo("text");

    part = requestParts.getFirst("file part");
    assertThat(part.name()).isEqualTo("file part");
    assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
    assertThat(decodeToString(part)).isEqualTo("AaBbCc");
  }

  @Test
  void writeFormEventStream() {
    Flux<Map.Entry<String, String>> body = Flux.just(
            new AbstractMap.SimpleEntry<>("name 1", "value 1"),
            new AbstractMap.SimpleEntry<>("name 2", "value 2+1"),
            new AbstractMap.SimpleEntry<>("name 2", "value 2+2")
    );

    Flux<PartEvent> partEvents = body
            .concatMap(entry -> FormPartEvent.create(entry.getKey(), entry.getValue()));

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(partEvents, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
            .block(Duration.ofSeconds(5));

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(2);

    Part part = requestParts.getFirst("name 1");
    assertThat(part.name()).isEqualTo("name 1");
    assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    String value = decodeToString(part);
    assertThat(value).isEqualTo("value 1");

    List<Part> parts = requestParts.get("name 2");
    assertThat(parts).hasSize(2);

    part = parts.get(0);
    assertThat(part.name()).isEqualTo("name 2");
    assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    value = decodeToString(part);
    assertThat(value).isEqualTo("value 2+1");

    part = parts.get(1);
    assertThat(part.name()).isEqualTo("name 2");
    assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    value = decodeToString(part);
    assertThat(value).isEqualTo("value 2+2");
  }

  @SuppressWarnings("ConstantConditions")
  private String decodeToString(Part part) {
    return StringDecoder.textPlainOnly().decodeToMono(part.content(),
            ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
            Collections.emptyMap()).block(Duration.ZERO);
  }

}
