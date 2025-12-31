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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ResponseCookie;
import infra.util.LinkedMultiValueMap;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 14:40
 */
class ModelAndViewRenderingResponseBuilderTests {

  @Test
  void basicModelAndViewRenderingResponseBuilder() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.addObject("key", "value");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.build();

    assertThat(response.name()).isEqualTo("viewName");
    assertThat(response.model()).containsEntry("key", "value");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithStatus() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.status(HttpStatus.NOT_FOUND).build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithNumericStatus() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.status(404).build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithHeaders() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.header("X-Custom-Header", "custom-value")
            .header("Content-Type", "text/html")
            .build();

    assertThat(response.headers().getFirst("X-Custom-Header")).isEqualTo("custom-value");
    assertThat(response.headers().getFirst("Content-Type")).isEqualTo("text/html");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithCookies() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookie("sessionId", "abc123")
            .cookie(ResponseCookie.from("theme", "dark").build())
            .build();

    assertThat(response.cookies().getFirst("sessionId").getValue()).isEqualTo("abc123");
    assertThat(response.cookies().getFirst("theme").getValue()).isEqualTo("dark");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithMultipleCookieValues() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookie("preferences", "theme=dark", "lang=en")
            .build();

    assertThat(response.cookies().get("preferences")).hasSize(2);
    assertThat(response.cookies().get("preferences").get(0).getValue()).isEqualTo("theme=dark");
    assertThat(response.cookies().get("preferences").get(1).getValue()).isEqualTo("lang=en");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithModelData() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.addObject("user", "john");
    modelAndView.addObject("role", "admin");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.build();

    Map<String, Object> model = response.model();
    assertThat(model).containsEntry("user", "john");
    assertThat(model).containsEntry("role", "admin");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithHeadersConsumer() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.headers(headers -> headers.add("X-Frame-Options", "DENY"))
            .build();

    assertThat(response.headers().getFirst("X-Frame-Options")).isEqualTo("DENY");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithHeadersMap() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    HttpHeaders existingHeaders = HttpHeaders.forWritable();
    existingHeaders.add("X-Content-Type-Options", "nosniff");
    existingHeaders.add("X-XSS-Protection", "1; mode=block");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.headers(existingHeaders).build();

    assertThat(response.headers().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.headers().getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithCookiesConsumer() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookies(cookies -> {
      cookies.add("session", ResponseCookie.from("session", "abc").build());
      cookies.add("preferences", ResponseCookie.from("preferences", "dark-mode").build());
    }).build();

    assertThat(response.cookies().getFirst("session").getValue()).isEqualTo("abc");
    assertThat(response.cookies().getFirst("preferences").getValue()).isEqualTo("dark-mode");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithCookiesCollection() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ResponseCookie cookie1 = ResponseCookie.from("auth", "token123").build();
    ResponseCookie cookie2 = ResponseCookie.from("lang", "zh").build();

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookies(java.util.List.of(cookie1, cookie2)).build();

    assertThat(response.cookies().getFirst("auth").getValue()).isEqualTo("token123");
    assertThat(response.cookies().getFirst("lang").getValue()).isEqualTo("zh");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithCookiesMap() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    LinkedMultiValueMap<String, ResponseCookie> cookieMap = new LinkedMultiValueMap<>();
    cookieMap.add("user", ResponseCookie.from("user", "john").build());
    cookieMap.add("theme", ResponseCookie.from("theme", "light").build());

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookies(cookieMap).build();

    assertThat(response.cookies().getFirst("user").getValue()).isEqualTo("john");
    assertThat(response.cookies().getFirst("theme").getValue()).isEqualTo("light");
  }

  @Test
  void modelAndViewRenderingResponseBuilderChaining() {
    ModelAndView modelAndView = new ModelAndView("dashboard");
    modelAndView.addObject("title", "Admin Dashboard");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder
            .status(HttpStatus.ACCEPTED)
            .header("Cache-Control", "no-cache")
            .cookie("sessionId", "xyz789")
            .build();

    assertThat(response.name()).isEqualTo("dashboard");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.headers().getFirst("Cache-Control")).isEqualTo("no-cache");
    assertThat(response.cookies().getFirst("sessionId").getValue()).isEqualTo("xyz789");
    assertThat(response.model()).containsEntry("title", "Admin Dashboard");
  }

  @Test
  void modelAndViewRenderingResponseWriteToInternal() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.addObject("data", "test");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.status(HttpStatus.CREATED).build();

    assertThat(response.name()).isEqualTo("viewName");
    assertThat(response.model()).containsEntry("data", "test");
    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void modelAndViewRenderingResponseWithNullStatus() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.setStatus(null);

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithNullCookiesCollection() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookies((java.util.Collection<ResponseCookie>) null).build();

    assertThat(response.cookies().size()).isEqualTo(0);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithNullCookiesMap() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookies((LinkedMultiValueMap<String, ResponseCookie>) null).build();

    assertThat(response.cookies().size()).isEqualTo(0);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithNullHeaders() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.headers((HttpHeaders) null).build();

    assertThat(response.headers().size()).isEqualTo(0);
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithEmptyCookieValues() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.cookie("emptyCookie").build();

    assertThat(response.cookies().getFirst("emptyCookie")).isNull();
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithMultipleHeadersSameName() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.header("X-Forwarded-For", "192.168.1.1")
            .header("X-Forwarded-For", "192.168.1.2")
            .build();

    assertThat(response.headers().get("X-Forwarded-For")).containsExactly("192.168.1.2");
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithRemoveHeader() {
    ModelAndView modelAndView = new ModelAndView("viewName");

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.header("X-Custom-Header", "value")
            .header("X-Custom-Header") // This should remove the header
            .build();

    assertThat(response.headers().containsKey("X-Custom-Header")).isFalse();
  }

  @Test
  void modelAndViewRenderingResponseBuilderWithComplexModel() {
    ModelAndView modelAndView = new ModelAndView("complexView");
    modelAndView.addObject("stringData", "testString");
    modelAndView.addObject("numericData", 123);
    modelAndView.addObject("booleanData", true);
    modelAndView.addObject("nullData", null);

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.build();

    Map<String, Object> model = response.model();
    assertThat(model).containsEntry("stringData", "testString");
    assertThat(model).containsEntry("numericData", 123);
    assertThat(model).containsEntry("booleanData", true);
    assertThat(model).containsEntry("nullData", null);
  }

  @Test
  void modelAndViewRenderingBuilderOverrideStatus() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.setStatus(HttpStatus.BAD_REQUEST);

    ModelAndViewRenderingResponseBuilder builder = new ModelAndViewRenderingResponseBuilder(modelAndView);
    RenderingResponse response = builder.status(HttpStatus.OK).build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

}