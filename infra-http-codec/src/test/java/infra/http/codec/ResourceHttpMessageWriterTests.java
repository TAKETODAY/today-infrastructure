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

package infra.http.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.io.ByteArrayResource;
import infra.core.ResolvableType;
import infra.core.io.FileSystemResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpRange;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.reactive.ZeroCopyHttpOutputMessage;
import infra.http.reactive.server.ServerHttpResponse;
import infra.util.MimeType;
import infra.util.StringUtils;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.http.MediaType.TEXT_PLAIN;
import static infra.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link ResourceHttpMessageWriter}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceHttpMessageWriterTests {

  private static final Map<String, Object> HINTS = Collections.emptyMap();

  @Test
  void ignoreRangeWhenIfRangeDoesNotMatch() {
    this.response.getHeaders().setETag("\"current\"");
    testWrite(get("/").range(of(0, 5)).header(HttpHeaders.IF_RANGE, "\"stale\"").build());
    assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isNull();
    StepVerifier.create(this.response.getBodyAsString())
            .expectNext("Infra  Framework test resource content.").expectComplete().verify();
  }

  private final ResourceHttpMessageWriter writer = new ResourceHttpMessageWriter();

  private final MockServerHttpResponse response = new MockServerHttpResponse();

  private final Mono<Resource> input = Mono.just(new ByteArrayResource(
          "Infra  Framework test resource content.".getBytes(StandardCharsets.UTF_8)));

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void sendsFileOrRequestedRegionThroughZeroCopy(boolean range, @TempDir Path directory) throws Exception {
    Path path = Files.writeString(directory.resolve("example.txt"), "abcdefghij");
    var resource = new FileSystemResource(path);
    var requestBuilder = get("/");
    if (range) {
      requestBuilder.range(of(2, 4));
    }
    ServerHttpResponse response = mock(ServerHttpResponse.class, withSettings().extraInterfaces(ZeroCopyHttpOutputMessage.class));
    ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
    HttpHeaders headers = HttpHeaders.forWritable();
    long position = range ? 2 : 0;
    long count = range ? 3 : 10;
    given(response.getHeaders()).willReturn(headers);
    given(zeroCopyResponse.writeWith(path, position, count)).willReturn(Mono.empty());

    StepVerifier.create(this.writer.write(Mono.just(resource), null, ResolvableType.forClass(Resource.class),
                    MediaType.APPLICATION_OCTET_STREAM, requestBuilder.build(), response, HINTS))
            .expectComplete().verify(Duration.ofSeconds(5));

    then(zeroCopyResponse).should().writeWith(path, position, count);
    assertThat(headers.getContentLength()).isEqualTo(count);
    assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN);
    if (range) {
      then(response).should().setStatusCode(HttpStatus.PARTIAL_CONTENT);
      assertThat(headers.getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 2-4/10");
    }
  }

  @Test
  void writesAOneShotStreamWithoutReadingItToDetermineLength() {
    byte[] bytes = "one-shot body".getBytes(StandardCharsets.UTF_8);
    Resource resource = new InputStreamResource(new ByteArrayInputStream(bytes));
    StepVerifier.create(this.writer.write(Mono.just(resource), ResolvableType.forClass(Resource.class),
                    MediaType.ALL, this.response, HINTS))
            .expectComplete().verify(Duration.ofSeconds(5));

    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
    StepVerifier.create(this.response.getBodyAsString()).expectNext("one-shot body").expectComplete().verify(Duration.ofSeconds(5));
  }

  @Test
  void preservesExplicitContentLength() {
    this.response.getHeaders().setContentLength(39);
    StepVerifier.create(this.writer.write(this.input, ResolvableType.forClass(Resource.class), TEXT_PLAIN, this.response, HINTS))
            .expectComplete().verify(Duration.ofSeconds(5));
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(39);
    assertThat(this.response.getBodyAsString().block(Duration.ofSeconds(5))).hasSize(39);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void getWritableMediaTypes() throws Exception {
    assertThat((List) this.writer.getWritableMediaTypes())
            .containsExactlyInAnyOrder(MimeType.APPLICATION_OCTET_STREAM, MimeType.ALL);
  }

  @Test
  public void writeResource() throws Exception {

    testWrite(get("/").build());

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(TEXT_PLAIN);
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(39L);
    assertThat(this.response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");

    String content = "Infra  Framework test resource content.";
    StepVerifier.create(this.response.getBodyAsString()).expectNext(content).expectComplete().verify();
  }

  @Test
  public void writeSingleRegion() throws Exception {

    testWrite(get("/").range(of(0, 5)).build());

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(TEXT_PLAIN);
    assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-5/39");
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(6L);

    StepVerifier.create(this.response.getBodyAsString()).expectNext("Infra ").expectComplete().verify();
  }

  @Test
  public void writeMultipleRegions() throws Exception {

    testWrite(get("/").range(of(0, 5), of(7, 15), of(17, 20), of(22, 38)).build());

    HttpHeaders headers = this.response.getHeaders();
    String contentType = headers.getContentType().toString();
    String boundary = contentType.substring(30);

    assertThat(contentType).startsWith("multipart/byteranges;boundary=");

    StepVerifier.create(this.response.getBodyAsString())
            .consumeNextWith(content -> {
              String[] actualRanges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);
              String[] expected = new String[] {
                      "--" + boundary,
                      "Content-Type: text/plain",
                      "Content-Range: bytes 0-5/39",
                      "Infra ",
                      "--" + boundary,
                      "Content-Type: text/plain",
                      "Content-Range: bytes 7-15/39",
                      "Framework",
                      "--" + boundary,
                      "Content-Type: text/plain",
                      "Content-Range: bytes 17-20/39",
                      "test",
                      "--" + boundary,
                      "Content-Type: text/plain",
                      "Content-Range: bytes 22-38/39",
                      "resource content.",
                      "--" + boundary + "--"
              };
              assertThat(actualRanges).isEqualTo(expected);
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void invalidRange() throws Exception {

    testWrite(get("/").header(HttpHeaders.RANGE, "invalid").build());

    assertThat(this.response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
    assertThat(this.response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  @Test
  void invalidRangePosition() {
    testWrite(get("/").header(HttpHeaders.RANGE, "bytes=2000-5000").build());

    assertThat(this.response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
    assertThat(this.response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  private void testWrite(MockServerHttpRequest request) {
    Mono<Void> mono = this.writer.write(this.input, null, null, TEXT_PLAIN, request, this.response, HINTS);
    StepVerifier.create(mono).expectComplete().verify();
  }

  private static HttpRange of(int first, int last) {
    return HttpRange.createByteRange(first, last);
  }

}
