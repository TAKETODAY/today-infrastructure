/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpRange;
import infra.http.HttpStatus;
import infra.util.MimeType;
import infra.util.StringUtils;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.http.MediaType.TEXT_PLAIN;
import static infra.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceHttpMessageWriter}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceHttpMessageWriterTests {

  private static final Map<String, Object> HINTS = Collections.emptyMap();

  private final ResourceHttpMessageWriter writer = new ResourceHttpMessageWriter();

  private final MockServerHttpResponse response = new MockServerHttpResponse();

  private final Mono<Resource> input = Mono.just(new ByteArrayResource(
          "Spring Framework test resource content.".getBytes(StandardCharsets.UTF_8)));

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

    String content = "Spring Framework test resource content.";
    StepVerifier.create(this.response.getBodyAsString()).expectNext(content).expectComplete().verify();
  }

  @Test
  public void writeSingleRegion() throws Exception {

    testWrite(get("/").range(of(0, 5)).build());

    assertThat(this.response.getHeaders().getContentType()).isEqualTo(TEXT_PLAIN);
    assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-5/39");
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(6L);

    StepVerifier.create(this.response.getBodyAsString()).expectNext("Spring").expectComplete().verify();
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
                      "Spring",
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
