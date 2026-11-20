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

package infra.mock.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import infra.core.io.buffer.DataBufferUtils;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.MediaType;
import infra.http.reactive.server.SslInfo;
import infra.util.LinkedMultiValueMap;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class MockServerHttpRequestTests {

  @Test
  void methodFactoriesBuildEmptyRequests() {
    List<MockServerHttpRequest> requests = List.of(
            MockServerHttpRequest.get("/").build(), MockServerHttpRequest.head("/").build(),
            MockServerHttpRequest.post("/").build(), MockServerHttpRequest.put("/").build(),
            MockServerHttpRequest.patch("/").build(), MockServerHttpRequest.delete("/").build(),
            MockServerHttpRequest.options("/").build());
    assertThat(requests).extracting(MockServerHttpRequest::getMethod)
            .containsExactly(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
                    HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS);
    for (MockServerHttpRequest request : requests) {
      assertThat(request.getURI()).isEqualTo(URI.create("/"));
      assertThat(request.getLocalAddress()).isNull();
      assertThat(request.getRemoteAddress()).isNull();
      assertThat(request.getSslInfo()).isNull();
      StepVerifier.create(request.getBody()).verifyComplete();
    }
    assertThat(MockServerHttpRequest.method("GET", "/").build().getMethod()).isEqualTo(HttpMethod.GET);
    assertThatIllegalArgumentException().isThrownBy(() -> MockServerHttpRequest.method("PROPFIND", "/"));
    assertThatIllegalArgumentException().isThrownBy(() -> MockServerHttpRequest.method(" ", "/"))
            .withMessage("HTTP method is required.");
    assertThatIllegalArgumentException().isThrownBy(() -> MockServerHttpRequest.method((HttpMethod) null, URI.create("/")))
            .withMessageContaining("HTTP method is required");
    assertThatIllegalStateException().isThrownBy(requests.get(0)::getNativeRequest)
            .withMessageContaining("no native request");
  }

  @Test
  void builderPreservesAddressesSslContextAndEncodedQuery() {
    InetSocketAddress local = new InetSocketAddress("127.0.0.1", 8080);
    InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 1234);
    SslInfo ssl = mock(SslInfo.class);
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("other", "a/b");
    MockServerHttpRequest.BodyBuilder builder = MockServerHttpRequest.post("https://example.org/app/{name}?existing=1", "a b")
            .contextPath("/app").localAddress(local).remoteAddress(remote)
            .queryParam("q", "hello world").queryParams(query);
    builder.sslInfo(ssl);
    MockServerHttpRequest request = builder.build();
    assertThat(request.getLocalAddress()).isSameAs(local);
    assertThat(request.getRemoteAddress()).isSameAs(remote);
    assertThat(request.getSslInfo()).isSameAs(ssl);
    assertThat(request.getPath().contextPath().value()).isEqualTo("/app");
    assertThat(request.getURI().getRawPath()).isEqualTo("/app/a%20b");
    assertThat(request.getQueryParams()).containsEntry("q", List.of("hello world"))
            .containsEntry("other", List.of("a/b")).containsEntry("existing", List.of("1"));
  }

  @Test
  void builderSetsStandardAndCustomHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-First", "one");
    LinkedMultiValueMap<String, String> extra = new LinkedMultiValueMap<>();
    extra.add("X-Second", "two");
    HttpRange range = HttpRange.createByteRange(0, 10);
    MockServerHttpRequest request = MockServerHttpRequest.put("/")
            .headers(headers).headers(extra).header("X-Third", "three", "four")
            .accept(MediaType.APPLICATION_JSON).acceptCharset(StandardCharsets.UTF_8)
            .acceptLanguageAsLocales(Locale.CHINESE).contentLength(42).contentType(MediaType.TEXT_PLAIN)
            .ifModifiedSince(1000).ifUnmodifiedSince(2000).ifNoneMatch("\"tag\"").range(range).build();
    HttpHeaders actual = request.getHeaders();
    assertThat(actual.getFirst("X-First")).isEqualTo("one");
    assertThat(actual.getFirst("X-Second")).isEqualTo("two");
    assertThat(actual.get("X-Third")).containsExactly("three", "four");
    assertThat(actual.getAccept()).containsExactly(MediaType.APPLICATION_JSON);
    assertThat(actual.getAcceptCharset()).containsExactly(StandardCharsets.UTF_8);
    assertThat(actual.getAcceptLanguageAsLocales()).containsExactly(Locale.CHINESE);
    assertThat(actual.getContentLength()).isEqualTo(42);
    assertThat(actual.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(actual.getIfModifiedSince()).isEqualTo(1000);
    assertThat(actual.getIfUnmodifiedSince()).isEqualTo(2000);
    assertThat(actual.getIfNoneMatch()).containsExactly("\"tag\"");
    assertThat(actual.getRange()).containsExactly(range);
  }

  @Test
  void cookiesAreAddedWithoutOverwritingExplicitCookieHeader() {
    HttpCookie first = new HttpCookie("first", "1");
    HttpCookie second = new HttpCookie("second", "2");
    LinkedMultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
    cookies.add("second", second);
    MockServerHttpRequest request = MockServerHttpRequest.get("/").cookie(first).cookies(cookies).build();
    assertThat(request.getCookies()).containsEntry("first", List.of(first)).containsEntry("second", List.of(second));
    assertThat(request.getHeaders().get(HttpHeaders.COOKIE)).containsExactly("first=1", "second=2");
    MockServerHttpRequest explicit = MockServerHttpRequest.get("/").header(HttpHeaders.COOKIE, "explicit=3")
            .cookie(first).build();
    assertThat(explicit.getHeaders().get(HttpHeaders.COOKIE)).containsExactly("explicit=3");
  }

  @Test
  void stringBodyUsesUtf8OrContentTypeCharset() {
    assertBody(MockServerHttpRequest.post("/").body("你好"), "你好".getBytes(StandardCharsets.UTF_8));
    assertBody(MockServerHttpRequest.post("/").contentType(MediaType.TEXT_PLAIN).body("你好"),
            "你好".getBytes(StandardCharsets.UTF_8));
    assertBody(MockServerHttpRequest.post("/")
            .contentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1)).body("é"),
            new byte[] { (byte) 0xe9 });
  }

  private void assertBody(MockServerHttpRequest request, byte[] expected) {
    StepVerifier.create(DataBufferUtils.join(request.getBody()))
            .assertNext(buffer -> {
              try {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.read(bytes);
                assertThat(bytes).isEqualTo(expected);
              }
              finally {
                buffer.release();
              }
            }).verifyComplete();
  }

}
