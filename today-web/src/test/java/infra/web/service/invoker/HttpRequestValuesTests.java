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

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpRequestValues}.
 *
 * @author Rossen Stoyanchev
 */
class HttpRequestValuesTests {

  @Test
  void defaultUri() {
    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.GET).build();

    assertThat(requestValues.getURI()).isNull();
    assertThat(requestValues.getUriTemplate()).isEmpty();
    assertThat(requestValues.getUriBuilderFactory()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = { "POST", "PUT", "PATCH" })
  @SuppressWarnings("unchecked")
  void formData(String httpMethod) {

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.valueOf(httpMethod))
            .setContentType(MediaType.APPLICATION_FORM_URLENCODED)
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    Object body = requestValues.getBodyValue();
    assertThat((MultiValueMap<String, String>) body).hasSize(2)
            .containsEntry("param1", List.of("1st value"))
            .containsEntry("param2", List.of("2nd value A", "2nd value B"));
  }

  @Test
  void queryParamsWithUriTemplate() {

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
            .setUriTemplate("/path")
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    String uriTemplate = requestValues.getUriTemplate();
    assertThat(uriTemplate).isNotNull();

    assertThat(uriTemplate)
            .isEqualTo("/path?" +
                    "{param1}={param1[0]}&" +
                    "{param2}={param2[0]}&" +
                    "{param2}={param2[1]}");

    assertThat(requestValues.getUriVariables())
            .containsOnlyKeys("param1", "param2", "param1[0]", "param2[0]", "param2[1]")
            .containsEntry("param1", "param1")
            .containsEntry("param2", "param2")
            .containsEntry("param1[0]", "1st value")
            .containsEntry("param2[0]", "2nd value A")
            .containsEntry("param2[1]", "2nd value B");

    URI uri = UriComponentsBuilder.forURIString(uriTemplate)
            .encode()
            .build(requestValues.getUriVariables());

    assertThat(uri.toString())
            .isEqualTo("/path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
  }

  @Test
  void queryParamWithSemicolon() {
    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
            .setUriTemplate("/path")
            .addRequestParameter("userId:eq", "test value")
            .build();

    String uriTemplate = requestValues.getUriTemplate();
    assertThat(uriTemplate).isEqualTo("/path?{userId%3Aeq}={userId%3Aeq[0]}");

    URI uri = UriComponentsBuilder.forURIString(uriTemplate)
            .encode()
            .build(requestValues.getUriVariables());

    assertThat(uri.toString())
            .isEqualTo("/path?userId%3Aeq=test%20value");
  }

  @Test
  void queryParamsWithPreparedUri() {

    URI uri = URI.create("/my%20path");

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
            .setURI(uri)
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    assertThat(requestValues.getURI().toString())
            .isEqualTo("/my%20path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
  }

  @Test
  void requestPart() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("foo", "bar");
    HttpEntity<String> entity = new HttpEntity<>("body", headers);

    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addRequestPart("form field", "form value")
            .addRequestPart("entity", entity)
            .build();

    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(map).hasSize(2);
    assertThat(map.getFirst("form field")).isEqualTo("form value");
    assertThat(map.getFirst("entity")).isEqualTo(entity);
  }

  @Test
  void requestPartAndRequestParam() {

    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setUriTemplate("/path")
            .addRequestPart("form field", "form value")
            .addRequestParameter("query param", "query value")
            .build();

    String uriTemplate = requestValues.getUriTemplate();
    assertThat(uriTemplate).isNotNull();

    assertThat(uriTemplate).isEqualTo("/path?{query param}={query param[0]}");

    URI uri = UriComponentsBuilder.forURIString(uriTemplate)
            .encode()
            .build(requestValues.getUriVariables());
    assertThat(uri.toString())
            .isEqualTo("/path?query%20param=query%20value");

    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(map).hasSize(1);
    assertThat(map.getFirst("form field")).isEqualTo("form value");
  }

  @Test
  void emptyHeadersAndCookies() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setHttpMethod(HttpMethod.GET)
            .setUriTemplate("/api")
            .build();

    assertThat(requestValues.getHeaders()).isEmpty();
    assertThat(requestValues.getCookies()).isEmpty();
  }

  @Test
  void multipleHeaderValues() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addHeader("X-Custom", "value1", "value2")
            .build();

    assertThat(requestValues.getHeaders().get("X-Custom"))
            .containsExactly("value1", "value2");
  }

  @Test
  void multipleCookieValues() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addCookie("sessionId", "123", "456")
            .build();

    assertThat(requestValues.getCookies().get("sessionId"))
            .containsExactly("123", "456");
  }

  @Test
  void attributesHandling() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addAttribute("key1", "value1")
            .addAttribute("key2", 42)
            .build();

    assertThat(requestValues.getAttributes())
            .hasSize(2)
            .containsEntry("key1", "value1")
            .containsEntry("key2", 42);
  }

  @Test
  void versionHandling() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setApiVersion("v1.0")
            .build();

    assertThat(requestValues.getApiVersion()).isEqualTo("v1.0");
  }

  @ParameterizedTest
  @ValueSource(strings = { "GET", "HEAD", "DELETE", "OPTIONS", "TRACE" })
  void nonBodyHttpMethods(String method) {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setHttpMethod(HttpMethod.valueOf(method))
            .setUriTemplate("/api")
            .build();

    assertThat(requestValues.getBodyValue()).isNull();
  }

  @Test
  void uriVariablesWithTemplateAndParams() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setUriTemplate("/api/{version}/users/{id}")
            .setUriVariable("version", "v1")
            .setUriVariable("id", "123")
            .addRequestParameter("sort", "name")
            .build();

    assertThat(requestValues.getUriVariables())
            .containsEntry("version", "v1")
            .containsEntry("id", "123")
            .containsEntry("sort[0]", "name");
  }

  @Test
  void acceptMultipleMediaTypes() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
            .build();

    assertThat(requestValues.getHeaders().getAccept())
            .containsExactly(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
  }

  @Test
  void mixedUriParamsAndQueryParams() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setUriTemplate("/api/{resource}")
            .setUriVariable("resource", "users")
            .addRequestParameter("page", "1")
            .addRequestParameter("size", "20")
            .addRequestParameter("sort", "name", "age")
            .build();

    assertThat(requestValues.getUriTemplate()).contains("/api/{resource}");
    assertThat(requestValues.getUriVariables())
            .containsEntry("resource", "users")
            .containsEntry("page[0]", "1")
            .containsEntry("size[0]", "20")
            .containsEntry("sort[0]", "name")
            .containsEntry("sort[1]", "age");
  }

  @Test
  void nullAndEmptyValues() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addHeader("X-Empty", "")
            .addCookie("empty-cookie", "")
            .setUriVariable("nullParam", null)
            .build();

    assertThat(requestValues.getHeaders().get("X-Empty")).containsExactly("");
    assertThat(requestValues.getCookies().get("empty-cookie")).containsExactly("");
    assertThat(requestValues.getUriVariables()).containsEntry("nullParam", null);
  }

  @Test
  void contentTypeWithCharset() {
    MediaType mediaType = MediaType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setContentType(mediaType)
            .build();

    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(mediaType);
  }

  @Test
  void headersCaseInsensitivity() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("CONTENT-TYPE", "application/xml")
            .build();

    assertThat(requestValues.getHeaders().get("content-type"))
            .containsExactly("application/json", "application/xml");
  }

  @Test
  void multipleAttributesOfSameType() {
    List<String> list1 = List.of("value1");
    List<String> list2 = List.of("value2");

    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addAttribute("list1", list1)
            .addAttribute("list2", list2)
            .build();

    assertThat(requestValues.getAttributes())
            .hasSize(2)
            .containsEntry("list1", list1)
            .containsEntry("list2", list2);
  }

  @Test
  void specialCharactersInUriTemplate() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setUriTemplate("/api/users?filter={filter}&sort={sort}")
            .setUriVariable("filter", "name:eq:john")
            .setUriVariable("sort", "+name,-age")
            .build();

    assertThat(requestValues.getUriTemplate())
            .contains("filter={filter}")
            .contains("sort={sort}");
    assertThat(requestValues.getUriVariables())
            .containsEntry("filter", "name:eq:john")
            .containsEntry("sort", "+name,-age");
  }

  @Test
  void customHeadersOverrideDefaultHeaders() {
    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setAccept(List.of(MediaType.APPLICATION_JSON))
            .addHeader("Accept", "application/xml")
            .build();

    assertThat(requestValues.getHeaders().getAccept())
            .containsExactly(MediaType.APPLICATION_JSON, MediaType.valueOf("application/xml"));
  }

  @Test
  void customContentTypeAndBodyPublisher() {
    Flux<String> bodyPublisher = Flux.just("data");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };
    MediaType mediaType = MediaType.valueOf("application/custom+json");

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setContentType(mediaType)
            .setBodyPublisher(bodyPublisher, elementType)
            .build();

    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(mediaType);
    assertThat(requestValues.getBodyPublisher()).isEqualTo(bodyPublisher);
  }

  @Test
  void multipleRequestPartsWithHeaders() {
    Flux<byte[]> fileContent = Flux.just("file data".getBytes());
    ParameterizedTypeReference<byte[]> elementType = new ParameterizedTypeReference<>() { };
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPart("text", new HttpEntity<>("text data", headers))
            .addRequestPartPublisher("file", fileContent, elementType)
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> parts = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(parts.getFirst("text")).isInstanceOf(HttpEntity.class);
  }

  @Test
  void emptyBodyPublisher() {
    Flux<String> emptyPublisher = Flux.empty();
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(emptyPublisher, elementType)
            .build();

    assertThat(requestValues.getBodyPublisher()).isEqualTo(emptyPublisher);
    assertThat(requestValues.getBodyValue()).isNull();
  }

  @Test
  void bodyPublisherWithMultipleElements() {
    Flux<Integer> numberPublisher = Flux.range(1, 5);
    ParameterizedTypeReference<Integer> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(numberPublisher, elementType)
            .build();

    assertThat(requestValues.getBodyPublisher()).isEqualTo(numberPublisher);
    assertThat(requestValues.getBodyPublisherElementType()).isEqualTo(elementType);
  }

  @Test
  void requestPartPublisherWithCustomCharset() {
    Flux<String> textContent = Flux.just("测试内容");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };
    MediaType mediaType = MediaType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setContentType(mediaType)
            .addRequestPartPublisher("text", textContent, elementType)
            .build();

    assertThat(requestValues.getHeaders().getContentType()).isEqualTo(mediaType);
    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

}
