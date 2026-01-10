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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ResponseCookie;
import infra.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 16:55
 */
class DefaultRenderingResponseBuilderTests {

  @Test
  void createWithName() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("viewName");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void createFromExistingResponse() {
    RenderingResponse original = RenderingResponse.create("originalView")
            .modelAttribute("key", "value")
            .status(HttpStatus.NOT_FOUND)
            .header("X-Custom", "original")
            .build();

    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder(original);
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("originalView");
    assertThat(response.model()).containsEntry("key", "value");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.headers().getFirst("X-Custom")).isEqualTo("original");
  }

  @Test
  void statusMethods() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.status(HttpStatus.ACCEPTED).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);

    response = builder.status(404).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void cookieMethods() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.cookie("sessionId", "abc123")
            .cookie(ResponseCookie.from("theme", "dark").build())
            .build();

    assertThat(response.cookies().getFirst("sessionId").getValue()).isEqualTo("abc123");
    assertThat(response.cookies().getFirst("theme").getValue()).isEqualTo("dark");
  }

  @Test
  void cookiesConsumer() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.cookies(cookies -> {
      cookies.add("session", ResponseCookie.from("session", "abc").build());
      cookies.add("preferences", ResponseCookie.from("preferences", "dark-mode").build());
    }).build();

    assertThat(response.cookies().getFirst("session").getValue()).isEqualTo("abc");
    assertThat(response.cookies().getFirst("preferences").getValue()).isEqualTo("dark-mode");
  }

  @Test
  void cookiesCollection() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    ResponseCookie cookie1 = ResponseCookie.from("auth", "token123").build();
    ResponseCookie cookie2 = ResponseCookie.from("lang", "zh").build();

    RenderingResponse response = builder.cookies(java.util.List.of(cookie1, cookie2)).build();

    assertThat(response.cookies().getFirst("auth").getValue()).isEqualTo("token123");
    assertThat(response.cookies().getFirst("lang").getValue()).isEqualTo("zh");
  }

  @Test
  void cookiesMap() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    LinkedMultiValueMap<String, ResponseCookie> cookieMap = new LinkedMultiValueMap<>();
    cookieMap.add("user", ResponseCookie.from("user", "john").build());
    cookieMap.add("theme", ResponseCookie.from("theme", "light").build());

    RenderingResponse response = builder.cookies(cookieMap).build();

    assertThat(response.cookies().getFirst("user").getValue()).isEqualTo("john");
    assertThat(response.cookies().getFirst("theme").getValue()).isEqualTo("light");
  }

  @Test
  void modelAttributeWithGeneratedName() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.modelAttribute("testValue").build();

    assertThat(response.model()).containsKey("string");
    assertThat(response.model().get("string")).isEqualTo("testValue");
  }

  @Test
  void modelAttributeWithNameAndValue() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.modelAttribute("customKey", "customValue").build();

    assertThat(response.model()).containsEntry("customKey", "customValue");
  }

  @Test
  void modelAttributesFromArray() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.modelAttributes("value1", 123, true).build();

    assertThat(response.model()).containsKey("string");
    assertThat(response.model()).containsKey("integer");
    assertThat(response.model()).containsKey("boolean");
    assertThat(response.model().get("string")).isEqualTo("value1");
    assertThat(response.model().get("integer")).isEqualTo(123);
    assertThat(response.model().get("boolean")).isEqualTo(true);
  }

  @Test
  void modelAttributesFromCollection() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.modelAttributes(java.util.List.of("item1", "item2", "item3")).build();

    assertThat(response.model()).containsKey("string");
    assertThat(response.model().get("string")).isEqualTo("item3");
  }

  @Test
  void modelAttributesFromMap() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.modelAttributes(java.util.Map.of("key1", "value1", "key2", "value2")).build();

    assertThat(response.model()).containsEntry("key1", "value1");
    assertThat(response.model()).containsEntry("key2", "value2");
  }

  @Test
  void headerMethods() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.header("X-Custom-Header", "custom-value")
            .header("Content-Type", "text/html")
            .build();

    assertThat(response.headers().getFirst("X-Custom-Header")).isEqualTo("custom-value");
    assertThat(response.headers().getFirst("Content-Type")).isEqualTo("text/html");
  }

  @Test
  void headersConsumer() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    RenderingResponse response = builder.headers(headers -> headers.add("X-Frame-Options", "DENY")).build();

    assertThat(response.headers().getFirst("X-Frame-Options")).isEqualTo("DENY");
  }

  @Test
  void headersMap() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("viewName");

    HttpHeaders existingHeaders = HttpHeaders.forWritable();
    existingHeaders.add("X-Content-Type-Options", "nosniff");
    existingHeaders.add("X-XSS-Protection", "1; mode=block");

    RenderingResponse response = builder.headers(existingHeaders).build();

    assertThat(response.headers().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.headers().getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  @Test
  void builderChaining() {
    DefaultRenderingResponseBuilder builder = new DefaultRenderingResponseBuilder("chainedView");

    RenderingResponse response = builder
            .modelAttribute("chainedKey", "chainedValue")
            .status(HttpStatus.ACCEPTED)
            .header("X-Chained", "true")
            .cookie("sessionId", "chain123")
            .build();

    assertThat(response.name()).isEqualTo("chainedView");
    assertThat(response.model()).containsEntry("chainedKey", "chainedValue");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.headers().getFirst("X-Chained")).isEqualTo("true");
    assertThat(response.cookies().getFirst("sessionId").getValue()).isEqualTo("chain123");
  }

}