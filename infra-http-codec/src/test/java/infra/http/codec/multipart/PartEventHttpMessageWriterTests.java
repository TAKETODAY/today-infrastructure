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

package infra.http.codec.multipart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.StringDecoder;
import infra.core.io.ByteArrayResource;
import infra.core.io.FileSystemResource;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    assertThat(this.writer.canWrite(ResolvableType.forClass(PartEvent.class), null)).isTrue();
    assertThat(this.writer.canWrite(ResolvableType.forClass(PartEvent.class), MediaType.TEXT_PLAIN)).isFalse();
    assertThat(this.writer.canWrite(ResolvableType.forClass(String.class), MediaType.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  void fileEventPreservesFilenameAndUsesTheDetectedMediaType(@TempDir Path directory) throws Exception {
    Path file = Files.writeString(directory.resolve("résumé.txt"), "file contents");

    StepVerifier.create(FilePartEvent.create("document", file))
            .assertNext(event -> {
              assertThat(event.name()).isEqualTo("document");
              assertThat(event.filename()).isEqualTo("résumé.txt");
              assertThat(event.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
              assertThat(event.isLast()).isFalse();
              try {
                assertThat(event.content().toString(StandardCharsets.UTF_8)).isEqualTo("file contents");
              }
              finally {
                event.content().release();
              }
            })
            .assertNext(event -> {
              assertThat(event.isLast()).isTrue();
              assertThat(event.content().readableBytes()).isZero();
            })
            .expectComplete().verify(Duration.ofSeconds(5));
  }

  @Test
  void emptyFileEventUsesCustomHeadersAndAnOctetStreamFallback(@TempDir Path directory) throws Exception {
    Path file = Files.createFile(directory.resolve("no-extension"));
    StepVerifier.create(FilePartEvent.create("document", new FileSystemResource(file), headers -> headers.set("X-Part", "test")))
            .assertNext(event -> {
              assertThat(event.headers().getFirst("X-Part")).isEqualTo("test");
              assertThat(event.headers().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
              assertThat(event.isLast()).isTrue();
              assertThat(event.content().readableBytes()).isZero();
            }).expectComplete().verify(Duration.ofSeconds(5));
    StepVerifier.create(FilePartEvent.create("document", new ByteArrayResource(new byte[0])))
            .expectError(FileNotFoundException.class).verify(Duration.ofSeconds(5));
  }

  @Test
  void formEventUsesItsDeclaredCharset() {
    StepVerifier.create(FormPartEvent.create("label", "é", headers ->
                    headers.setContentType(MediaType.TEXT_PLAIN.withCharset(StandardCharsets.ISO_8859_1))))
            .assertNext(event -> {
              assertThat(event.name()).isEqualTo("label");
              assertThat(event.value()).isEqualTo("é");
              var content = event.content();
              try {
                assertThat(content.readableBytes()).isEqualTo(1);
                assertThat(content.read()).isEqualTo((byte) 0xe9);
              }
              finally {
                content.release();
              }
            }).verifyComplete();
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
