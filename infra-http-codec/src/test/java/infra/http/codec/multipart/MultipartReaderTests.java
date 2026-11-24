/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.http.codec.multipart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.DecodingException;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MultipartReaderTests extends AbstractLeakCheckingTests {

  private static final ResolvableType PART = ResolvableType.forClass(Part.class);

  private static final ResolvableType PART_EVENT = ResolvableType.forClass(PartEvent.class);

  private static final ResolvableType PART_MAP = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();

  @Test
  void groupsRepeatedNamesWithoutChangingTheirOrder() {
    MultipartHttpMessageReader reader = new MultipartHttpMessageReader(this.partReader);
    assertThat(reader.getReadableMediaTypes()).containsExactly(
            MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED);
    assertThat(reader.canRead(PART_MAP, null)).isTrue();
    assertThat(reader.canRead(PART_MAP, MediaType.MULTIPART_RELATED)).isTrue();
    assertThat(reader.canRead(PART_MAP, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(reader.canRead(PART, MediaType.MULTIPART_FORM_DATA)).isFalse();

    StepVerifier.create(reader.read(PART_MAP, request(field("tag", "one"), field("other", "two"), field("tag", "three")), Map.of()))
            .assertNext(parts -> {
              assertThat(parts.keySet()).containsExactlyInAnyOrder("tag", "other");
              assertThat(parts.get("tag")).extracting(part -> ((FormFieldPart) part).value()).containsExactly("one", "three");
              assertThat(((FormFieldPart) parts.getFirst("other")).value()).isEqualTo("two");
            }).expectComplete().verify(TIMEOUT);
  }

  @Test
  void partAndEventReadersRequireAPartTypeAndABoundary() {
    PartEventHttpMessageReader eventReader = new PartEventHttpMessageReader();
    MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.MULTIPART_FORM_DATA).build();
    assertThat(this.partReader.canRead(PART, null)).isTrue();
    assertThat(this.partReader.canRead(PART, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(this.partReader.canRead(PART_EVENT, MediaType.MULTIPART_FORM_DATA)).isFalse();
    assertThat(eventReader.canRead(PART, MediaType.MULTIPART_FORM_DATA)).isFalse();
    assertThat(eventReader.canRead(PART_EVENT, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(this.partReader.getReadableMediaTypes()).contains(MediaType.MULTIPART_FORM_DATA);
    assertThat(eventReader.getReadableMediaTypes()).contains(MediaType.MULTIPART_FORM_DATA);
    StepVerifier.create(this.partReader.readMono(PART, request, Map.of()))
            .expectError(UnsupportedOperationException.class).verify(TIMEOUT);
    StepVerifier.create(eventReader.readMono(PART_EVENT, request, Map.of()))
            .expectError(UnsupportedOperationException.class).verify(TIMEOUT);
    for (Flux<?> result : new Flux<?>[] { this.partReader.read(PART, request, Map.of()), eventReader.read(PART_EVENT, request, Map.of()) }) {
      StepVerifier.create(result).expectErrorSatisfies(error -> assertThat(error)
              .isInstanceOf(DecodingException.class).hasMessageContaining("No multipart boundary")).verify(TIMEOUT);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void keepsBinaryPartsInMemoryOrStreamsThemWithoutCreatingFiles(boolean streaming, @TempDir Path storage) throws Exception {
    this.partReader.setStreaming(streaming);
    this.partReader.setMaxInMemorySize(-1);
    this.partReader.setMaxParts(-1);
    this.partReader.setFileStorageDirectory(storage);
    this.partReader.setHeadersCharset(UTF_8);

    StepVerifier.create(this.partReader.read(PART, request(file("abcdefghij"), field("note", "done")), Map.of())
                    .concatMap(part -> part.content().map(buffer -> {
                      try {
                        return buffer.toString(UTF_8);
                      }
                      finally {
                        buffer.release();
                      }
                    }).collectList().map(chunks -> part.name() + ":" + String.join("", chunks))))
            .expectNext("document:abcdefghij", "note:done").expectComplete().verify(TIMEOUT);
    assertThat(storage).isEmptyDirectory();
  }

  @Test
  void reportsStorageFailureWhenTheSpillDirectoryDisappears(@TempDir Path tempDir) throws Exception {
    Path storage = tempDir.resolve("uploads");
    this.partReader.setFileStorageDirectory(storage);
    this.partReader.setBlockingOperationScheduler(Schedulers.immediate());
    this.partReader.setMaxInMemorySize(2);
    Files.delete(storage);

    StepVerifier.create(this.partReader.read(PART, request(file("abcdefghij")), Map.of()))
            .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Could not create temp file")).verify(TIMEOUT);
  }

  @Test
  void allowsMovingAndDeletingADiskBackedPart(@TempDir Path tempDir) throws Exception {
    Path storage = tempDir.resolve("uploads");
    Path destination = tempDir.resolve("saved.txt");
    this.partReader.setFileStorageDirectory(storage);
    this.partReader.setBlockingOperationScheduler(Schedulers.immediate());
    this.partReader.setMaxInMemorySize(2);
    this.partReader.setMaxDiskUsagePerPart(20);

    Mono<Void> save = this.partReader.read(PART, request(file("abcdefghij")), Map.of()).single()
            .cast(FilePart.class).flatMap(part -> part.transferTo(destination.toFile()).then(part.delete()));
    StepVerifier.create(save).expectComplete().verify(TIMEOUT);
    assertThat(destination).hasContent("abcdefghij");
    assertThat(storage).isEmptyDirectory();
  }

  @Test
  void deletesAFileWhenALaterChunkExceedsTheDiskBudget(@TempDir Path storage) throws Exception {
    this.partReader.setFileStorageDirectory(storage);
    this.partReader.setBlockingOperationScheduler(Schedulers.immediate());
    this.partReader.setMaxInMemorySize(2);
    this.partReader.setMaxDiskUsagePerPart(12);

    StepVerifier.create(this.partReader.read(PART, request(file("x".repeat(64))), Map.of()))
            .expectErrorMessage("Part exceeded the disk usage limit of 12 bytes").verify(TIMEOUT);
    assertThat(storage).isEmptyDirectory();
  }

  @Test
  void reportsAnIoErrorWhenTransferringADiskBackedPartIntoAMissingDirectory(@TempDir Path storage) throws Exception {
    this.partReader.setFileStorageDirectory(storage);
    this.partReader.setBlockingOperationScheduler(Schedulers.immediate());
    this.partReader.setMaxInMemorySize(2);

    StepVerifier.create(this.partReader.read(PART, request(file("abcdefghij")), Map.of()).single().cast(FilePart.class)
                    .flatMap(part -> part.transferTo(storage.resolve("missing/saved.txt"))
                            .onErrorResume(error -> part.delete().then(Mono.error(error)))))
            .expectError(NoSuchFileException.class).verify(TIMEOUT);
    assertThat(storage).isEmptyDirectory();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void propagatesAnUpstreamFailureAndReleasesBufferedContent(boolean streaming) {
    this.partReader.setStreaming(streaming);
    var source = TestPublisher.<DataBuffer>create();
    var failure = new IllegalStateException("upload disconnected");
    var parts = this.partReader.read(PART, request(source.flux()), Map.of());

    StepVerifier.create(parts.concatMap(Part::content).doOnNext(DataBuffer.RELEASE_CONSUMER).then())
            .then(() -> source.next(bufferFactory.wrap(file("x".repeat(32)).getBytes(UTF_8)),
                    bufferFactory.wrap("y".repeat(32).getBytes(UTF_8))))
            .then(() -> source.error(failure))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure)).verify(TIMEOUT);
  }

  @Test
  void exposesEmptyFieldsAndEmptyBinaryPartsAsLastEvents() {
    var eventReader = new PartEventHttpMessageReader();
    var input = request(Flux.fromArray(new String[] {
            field("empty", ""), "--boundary\r\nContent-Disposition: form-data; name=\"binary\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n", "\r\n--boundary-", "-\r\n" })
            .map(chunk -> bufferFactory.wrap(chunk.getBytes(UTF_8))));

    StepVerifier.create(eventReader.read(PART_EVENT, input, Map.of()))
            .assertNext(event -> assertThat(event).isInstanceOfSatisfying(FormPartEvent.class, field -> {
              assertThat(field.name()).isEqualTo("empty");
              assertThat(field.value()).isEmpty();
              assertThat(field.isLast()).isTrue();
            }))
            .assertNext(event -> {
              DataBuffer content = event.content();
              try {
                assertThat(event.name()).isEqualTo("binary");
                assertThat(event).isNotInstanceOfAny(FormPartEvent.class, FilePartEvent.class);
                assertThat(content.readableBytes()).isZero();
                assertThat(event.isLast()).isTrue();
              }
              finally {
                content.release();
              }
            }).expectComplete().verify(TIMEOUT);
  }

  @Test
  void rejectsAFormFieldThatExceedsTheInMemoryLimit() {
    this.partReader.setMaxInMemorySize(4);
    StepVerifier.create(this.partReader.read(PART, request(field("note", "too long")), Map.of()))
            .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(DataBufferLimitException.class)
                    .hasMessage("Form field value exceeded the memory usage limit of 4 bytes")).verify(TIMEOUT);
  }

  @Test
  void decodesEventHeadersWithTheConfiguredCharset() {
    var reader = new PartEventHttpMessageReader();
    assertThatIllegalArgumentException().isThrownBy(() -> reader.setHeadersCharset(null))
            .withMessage("Charset is required");
    reader.setHeadersCharset(ISO_8859_1);
    var content = bufferFactory.wrap((field("café", "value") + "--boundary--\r\n").getBytes(ISO_8859_1));

    StepVerifier.create(reader.read(PART_EVENT, request(Flux.just(content)), Map.of()))
            .assertNext(event -> assertThat(event).isInstanceOfSatisfying(FormPartEvent.class, part -> {
              assertThat(part.name()).isEqualTo("café");
              assertThat(part.value()).isEqualTo("value");
            })).expectComplete().verify(TIMEOUT);
  }

  @Test
  void reportsAnEmptyFileAndRetainsTheContentOfABinaryPartWithoutAFilename() {
    var reader = new PartEventHttpMessageReader();
    String binary = "--boundary\r\nContent-Disposition: form-data; name=\"binary\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\nv\r\n";
    reader.setMaxPartSize(1);
    StepVerifier.create(reader.read(PART_EVENT, request(file(""), binary), Map.of())
                    .windowUntil(PartEvent::isLast)
                    .concatMap(events -> events.map(event -> {
                      var content = event.content();
                      try {
                        if (event instanceof FilePartEvent file) {
                          assertThat(file.filename()).isEqualTo("test.txt");
                          assertThat(file.isLast()).isTrue();
                          assertThat(content.readableBytes()).isZero();
                        }
                        else {
                          assertThat(event).isNotInstanceOf(FormPartEvent.class);
                          assertThat(event.name()).isEqualTo("binary");
                        }
                        return content.toString(UTF_8);
                      }
                      finally {
                        content.release();
                      }
                    }).collectList().map(chunks -> String.join("", chunks))))
            .expectNext("", "v").expectComplete().verify(TIMEOUT);
  }

  private MockServerHttpRequest request(String... parts) {
    String body = String.join("", parts) + "--boundary--\r\n";
    return request(Flux.range(0, (body.length() + 2) / 3)
                    .map(index -> bufferFactory.wrap(body.substring(index * 3, Math.min(index * 3 + 3, body.length())).getBytes(UTF_8)))
                    .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER));
  }

  private static MockServerHttpRequest request(Flux<DataBuffer> body) {
    return MockServerHttpRequest.post("/").contentType(MediaType.parseMediaType("multipart/form-data;boundary=boundary")).body(body);
  }

  private static String field(String name, String value) {
    return "--boundary\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
  }

  private static String file(String value) {
    return "--boundary\r\nContent-Disposition: form-data; name=\"document\"; filename=\"test.txt\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n" + value + "\r\n";
  }
}
