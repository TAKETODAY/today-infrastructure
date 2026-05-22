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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import infra.core.codec.DecodingException;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.MediaType;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.core.ResolvableType.forClass;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * @author Arjen Poutsma
 */
class DefaultPartHttpMessageReaderTests extends AbstractLeakCheckingTests {

  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer iaculis metus id vestibulum nullam.";

  private static final String MUSPI_MEROL = new StringBuilder(LOREM_IPSUM).reverse().toString();

  private static final int BUFFER_SIZE = 64;

  @ParameterizedDefaultPartHttpMessageReaderTest
  void canRead(DefaultPartHttpMessageReader reader) {
    assertThat(reader.canRead(forClass(Part.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void simple(DefaultPartHttpMessageReader reader) throws InterruptedException {
    MockServerHttpRequest request = createRequest(
            "simple.multipart", "simple-boundary");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(2);
    StepVerifier.create(result)
            .consumeNextWith(part -> testPart(part, null,
                    "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak.", latch)).as("Part 1")
            .consumeNextWith(part -> testPart(part, null,
                    "This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n", latch)).as("Part 2")
            .verifyComplete();

    latch.await();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void noHeaders(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "no-header.multipart", "boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(part -> {
              assertThat(part.headers().isEmpty()).isTrue();
              part.content().subscribe(DataBuffer.RELEASE_CONSUMER);
            })
            .verifyComplete();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void noEndBoundary(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "no-end-boundary.multipart", "boundary");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(part -> {
              assertThat(part.headers().getFirst("Header")).isEqualTo("Value");
              part.content().subscribe(DataBuffer.RELEASE_CONSUMER);
            })
            .expectError(DecodingException.class)
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void garbage(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "garbage-1.multipart", "boundary");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void noEndHeader(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "no-end-header.multipart", "boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void noEndBody(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "no-end-body.multipart", "boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void cancelPart(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "simple.multipart", "simple-boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(part -> part.content().subscribe(DataBuffer.RELEASE_CONSUMER))
            .thenCancel()
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void cancelBody(DefaultPartHttpMessageReader reader) throws Exception {
    MockServerHttpRequest request = createRequest(
            "simple.multipart", "simple-boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(1);
    StepVerifier.create(result, 1)
            .consumeNextWith(part -> part.content().subscribe(new CancelSubscriber()))
            .thenRequest(1)
            .consumeNextWith(part -> testPart(part, null,
                    "This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n", latch)).as("Part 2")
            .verifyComplete();

    latch.await(3, TimeUnit.SECONDS);
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void cancelBodyThenPart(DefaultPartHttpMessageReader reader) {
    MockServerHttpRequest request = createRequest(
            "simple.multipart", "simple-boundary");
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result, 1)
            .consumeNextWith(part -> part.content().subscribe(new CancelSubscriber()))
            .thenCancel()
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void firefox(DefaultPartHttpMessageReader reader) throws InterruptedException {
    testBrowser(reader, "firefox.multipart",
            "---------------------------18399284482060392383840973206");
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void chrome(DefaultPartHttpMessageReader reader) throws InterruptedException {
    testBrowser(reader, "chrome.multipart",
            "----WebKitFormBoundaryEveBLvRT65n21fwU");
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void safari(DefaultPartHttpMessageReader reader) throws InterruptedException {
    testBrowser(reader, "safari.multipart",
            "----WebKitFormBoundaryG8fJ50opQOML0oGD");
  }

  @Test
  void tooManyParts() throws InterruptedException {
    MockServerHttpRequest request = createRequest(
            "files.multipart", "----WebKitFormBoundaryG8fJ50opQOML0oGD");

    DefaultPartHttpMessageReader reader = new DefaultPartHttpMessageReader();
    reader.setMaxParts(1);

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(1);
    StepVerifier.create(result)
            .consumeNextWith(part -> testBrowserFile(part, "file2", "a.txt", LOREM_IPSUM, latch)).as("Part 1")
            .expectError(DecodingException.class)
            .verify();

    latch.await();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void quotedBoundary(DefaultPartHttpMessageReader reader) throws InterruptedException {
    MockServerHttpRequest request = createRequest(
            "simple.multipart", "\"simple-boundary\"");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(2);
    StepVerifier.create(result)
            .consumeNextWith(part -> testPart(part, null,
                    "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak.", latch)).as("Part 1")
            .consumeNextWith(part -> testPart(part, null,
                    "This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n", latch)).as("Part 2")
            .verifyComplete();

    latch.await();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void utf8Headers(DefaultPartHttpMessageReader reader) throws InterruptedException {
    MockServerHttpRequest request = createRequest(
            "utf8.multipart", "\"simple-boundary\"");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(1);
    StepVerifier.create(result)
            .consumeNextWith(part -> {
              assertThat(part.headers().contains("Føø", Collections.singletonList("Bår"))).isTrue();
              testPart(part, null, "This is plain ASCII text.", latch);
            })
            .verifyComplete();

    latch.await();
  }

  @Test
  void largeBufferForHeaderDoesNotExceedLimit() throws InterruptedException {
    Flux<DataBuffer> body = DataBufferUtils
            .readByteChannel(new ClassPathResource("files.multipart", getClass())::readableChannel, bufferFactory, 282);

    MediaType contentType = new MediaType("multipart", "form-data", singletonMap("boundary", "----WebKitFormBoundaryG8fJ50opQOML0oGD"));
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(contentType)
            .body(body);

    DefaultPartHttpMessageReader reader = new DefaultPartHttpMessageReader();

    reader.setMaxHeadersSize(230);

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(2);
    StepVerifier.create(result)
            .consumeNextWith(part -> testPart(part, null, LOREM_IPSUM, latch))
            .consumeNextWith(part -> testPart(part, null, MUSPI_MEROL, latch))
            .verifyComplete();

    latch.await();
  }

  @Test
  void exceedHeaderLimit() {
    MockServerHttpRequest request = createRequest("files.multipart", "\"----WebKitFormBoundaryG8fJ50opQOML0oGD\"");

    DefaultPartHttpMessageReader reader = new DefaultPartHttpMessageReader();
    reader.setMaxHeadersSize(80);
    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DataBufferLimitException.class)
            .verify();
  }

  @ParameterizedDefaultPartHttpMessageReaderTest
  void emptyLastPart(DefaultPartHttpMessageReader reader) throws InterruptedException {
    MockServerHttpRequest request = createRequest(
            "empty-part.multipart", "LiG0chJ0k7YtLt-FzTklYFgz50i88xJCW5jD");

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());

    CountDownLatch latch = new CountDownLatch(2);
    StepVerifier.create(result)
            .consumeNextWith(part -> testPart(part, null, "", latch))
            .consumeNextWith(part -> testPart(part, null, "", latch))
            .verifyComplete();

    latch.await();
  }

  private void testBrowser(DefaultPartHttpMessageReader reader, String fileName, String boundary)
          throws InterruptedException {

    MockServerHttpRequest request = createRequest(fileName, boundary);

    Flux<Part> result = reader.read(forClass(Part.class), request, emptyMap());
    CountDownLatch latch = new CountDownLatch(3);
    StepVerifier.create(result)
            .consumeNextWith(part -> testBrowserFormField(part, "text1", "a")).as("text1")
            .consumeNextWith(part -> testBrowserFormField(part, "text2", "b")).as("text2")
            .consumeNextWith(part -> testBrowserFile(part, "file1", "a.txt", LOREM_IPSUM, latch)).as("file1")
            .consumeNextWith(part -> testBrowserFile(part, "file2", "a.txt", LOREM_IPSUM, latch)).as("file2-1")
            .consumeNextWith(part -> testBrowserFile(part, "file2", "b.txt", MUSPI_MEROL, latch)).as("file2-2")
            .verifyComplete();
    latch.await();
  }

  private MockServerHttpRequest createRequest(String fileName, String boundary) {
    Resource resource = new ClassPathResource("/infra/http/codec/multipart/" + fileName);
    Flux<DataBuffer> body = DataBufferUtils
            .readByteChannel(resource::readableChannel, bufferFactory, BUFFER_SIZE);

    MediaType contentType = new MediaType("multipart", "form-data", singletonMap("boundary", boundary));
    return MockServerHttpRequest.post("/")
            .contentType(contentType)
            .body(body);
  }

  private void testPart(Part part, @Nullable String expectedName, String expectedContents, CountDownLatch latch) {
    if (expectedName != null) {
      assertThat(part.name()).isEqualTo(expectedName);
    }

    Mono<String> content = DataBufferUtils.join(part.content())
            .map(buffer -> {
              byte[] bytes = new byte[buffer.readableBytes()];
              buffer.read(bytes);
              buffer.release();
              return new String(bytes, UTF_8);
            });

    content.subscribe(s -> assertThat(s).isEqualTo(expectedContents),
            throwable -> {
              throw new AssertionError(throwable.getMessage(), throwable);
            },
            latch::countDown);
  }

  private static void testBrowserFormField(Part part, String name, String value) {
    assertThat(part).isInstanceOf(FormFieldPart.class);
    assertThat(part.name()).isEqualTo(name);
    FormFieldPart formField = (FormFieldPart) part;
    assertThat(formField.value()).isEqualTo(value);
  }

  private static void testBrowserFile(Part part, String name, String filename, String contents, CountDownLatch latch) {
    try {
      assertThat(part).isInstanceOf(FilePart.class);
      assertThat(part.name()).isEqualTo(name);
      FilePart filePart = (FilePart) part;
      assertThat(filePart.filename()).isEqualTo(filename);

      Path tempFile = Files.createTempFile("DefaultMultipartMessageReaderTests", null);

      filePart.transferTo(tempFile)
              .subscribe(null,
                      throwable -> {
                        throw Exceptions.bubble(throwable);
                      },
                      () -> {
                        try {
                          verifyContents(tempFile, contents);
                        }
                        finally {
                          latch.countDown();
                        }

                      });
    }
    catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  private static void verifyContents(Path tempFile, String contents) {
    try {
      String result = String.join("", Files.readAllLines(tempFile));
      assertThat(result).isEqualTo(contents);
    }
    catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }

  private static class CancelSubscriber extends BaseSubscriber<DataBuffer> {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      request(1);
    }

    @Override
    protected void hookOnNext(DataBuffer buffer) {
      buffer.release();
      cancel();
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest
  @MethodSource("infra.http.codec.multipart.DefaultPartHttpMessageReaderTests#messageReaders()")
  @interface ParameterizedDefaultPartHttpMessageReaderTest {
  }

  static Stream<Arguments> messageReaders() {
    DefaultPartHttpMessageReader inMemory = new DefaultPartHttpMessageReader();
    inMemory.setStreaming(false);
    inMemory.setMaxInMemorySize(1000);

    DefaultPartHttpMessageReader onDisk = new DefaultPartHttpMessageReader();
    onDisk.setStreaming(false);
    onDisk.setMaxInMemorySize(100);

    return Stream.of(
            argumentSet("streaming", onDisk),
            argumentSet("in-memory", inMemory),
            argumentSet("on-disk", onDisk));
  }

}
