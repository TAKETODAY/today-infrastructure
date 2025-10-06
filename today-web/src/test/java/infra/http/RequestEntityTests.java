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

package infra.http;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.core.ParameterizedTypeReference;
import infra.util.DataSize;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link infra.http.RequestEntity}.
 *
 * @author Arjen Poutsma
 * @author Parviz Rozikov
 */
class RequestEntityTests {

  @Test
  void normal() throws URISyntaxException {
    String headerName = "My-Custom-Header";
    String headerValue = "HeaderValue";
    URI url = new URI("https://example.com");
    Integer entity = 42;

    RequestEntity<Object> requestEntity =
            RequestEntity.method(HttpMethod.GET, url)
                    .header(headerName, headerValue).body(entity);

    assertThat(requestEntity).isNotNull();
    assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(requestEntity.hasHeader(headerName)).isTrue();
    assertThat(requestEntity.headers().getFirst(headerName)).isEqualTo(headerValue);
    assertThat(requestEntity.getBody()).isEqualTo(entity);
  }

  @Test
  void uriVariablesExpansion() throws URISyntaxException {
    URI uri = UriComponentsBuilder.forURIString("https://example.com/{foo}").buildAndExpand("bar").toURI();
    RequestEntity.get(uri).accept(MediaType.TEXT_PLAIN).build();

    String url = "https://www.{host}.com/{path}";
    String host = "example";
    String path = "foo/bar";
    URI expected = new URI("https://www.example.com/foo/bar");

    uri = UriComponentsBuilder.forURIString(url).buildAndExpand(host, path).toURI();
    RequestEntity<?> entity = RequestEntity.get(uri).build();
    assertThat(entity.getURI()).isEqualTo(expected);

    Map<String, String> uriVariables = new HashMap<>(2);
    uriVariables.put("host", host);
    uriVariables.put("path", path);

    uri = UriComponentsBuilder.forURIString(url).buildAndExpand(uriVariables).toURI();
    entity = RequestEntity.get(uri).build();
    assertThat(entity.getURI()).isEqualTo(expected);
  }

  @Test
  void uriExpansion() {
    RequestEntity<Void> entity =
            RequestEntity.get("https://www.{host}.com/{path}", "example", "foo/bar").build();

    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
    RequestEntity.UriTemplateRequestEntity<Void> ext = (RequestEntity.UriTemplateRequestEntity<Void>) entity;

    assertThat(ext.getUriTemplate()).isEqualTo("https://www.{host}.com/{path}");
    assertThat(ext.getVars()).containsExactly("example", "foo/bar");
  }

  @Test
  void get() {
    RequestEntity<Void> requestEntity = RequestEntity.get(URI.create("https://example.com")).accept(
            MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG).build();

    assertThat(requestEntity).isNotNull();
    assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(requestEntity.hasHeader(HttpHeaders.ACCEPT)).isTrue();
    assertThat(requestEntity.headers().getFirst(HttpHeaders.ACCEPT)).isEqualTo("image/gif, image/jpeg, image/png");
    assertThat(requestEntity.getBody()).isNull();
  }

  @Test
  void headers() throws URISyntaxException {
    MediaType accept = MediaType.TEXT_PLAIN;
    long ifModifiedSince = 12345L;
    String ifNoneMatch = "\"foo\"";
    long contentLength = 67890;
    MediaType contentType = MediaType.TEXT_PLAIN;

    RequestEntity<Void> responseEntity = RequestEntity.post(new URI("https://example.com")).
            accept(accept).
            acceptCharset(StandardCharsets.UTF_8).
            ifModifiedSince(ifModifiedSince).
            ifNoneMatch(ifNoneMatch).
            contentLength(contentLength).
            contentType(contentType).
            headers(headers -> Assertions.assertThat(headers).hasSize(6)).
            build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(responseEntity.getURI()).isEqualTo(new URI("https://example.com"));
    HttpHeaders responseHeaders = responseEntity.headers();

    assertThat(responseHeaders.getFirst(HttpHeaders.ACCEPT)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(responseHeaders.getFirst(HttpHeaders.ACCEPT_CHARSET)).isEqualTo("utf-8");
    assertThat(responseHeaders.getFirst(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo("Thu, 01 Jan 1970 00:00:12 GMT");
    assertThat(responseHeaders.getFirst(HttpHeaders.IF_NONE_MATCH)).isEqualTo(ifNoneMatch);
    assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo(String.valueOf(contentLength));
    assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType.toString());

    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  void methods() throws URISyntaxException {
    URI url = new URI("https://example.com");

    RequestEntity<?> entity = RequestEntity.get(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);

    entity = RequestEntity.post(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.POST);

    entity = RequestEntity.head(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.HEAD);

    entity = RequestEntity.options(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.OPTIONS);

    entity = RequestEntity.put(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PUT);

    entity = RequestEntity.patch(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PATCH);

    entity = RequestEntity.delete(url).build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.DELETE);

  }

  @Test
  void types() throws URISyntaxException {
    URI url = new URI("https://example.com");
    List<String> body = Arrays.asList("foo", "bar");
    ParameterizedTypeReference<?> typeReference = new ParameterizedTypeReference<List<String>>() { };

    RequestEntity<?> entity = RequestEntity.post(url).body(body, typeReference.getType());
    assertThat(entity.getType()).isEqualTo(typeReference.getType());
  }

  @Test
  void equalityWithUrl() {
    RequestEntity<Void> requestEntity1 = RequestEntity.method(HttpMethod.GET, "http://test.api/path/").build();
    RequestEntity<Void> requestEntity2 = RequestEntity.method(HttpMethod.GET, "http://test.api/path/").build();
    RequestEntity<Void> requestEntity3 = RequestEntity.method(HttpMethod.GET, "http://test.api/pathX/").build();

    assertThat(requestEntity1).isEqualTo(requestEntity2);
    assertThat(requestEntity2).isEqualTo(requestEntity1);
    assertThat(requestEntity1).isNotEqualTo(requestEntity3);
    assertThat(requestEntity3).isNotEqualTo(requestEntity2);
    assertThat(requestEntity1.hashCode()).isEqualTo(requestEntity2.hashCode());
    assertThat(requestEntity1.hashCode()).isNotEqualTo(requestEntity3.hashCode());
  }

  @Test
  void equalityWithUriTemplate() {
    Map<String, Object> vars = Collections.singletonMap("id", "1");

    RequestEntity<Void> requestEntity1 =
            RequestEntity.method(HttpMethod.GET, "http://test.api/path/{id}", vars).build();
    RequestEntity<Void> requestEntity2 =
            RequestEntity.method(HttpMethod.GET, "http://test.api/path/{id}", vars).build();
    RequestEntity<Void> requestEntity3 =
            RequestEntity.method(HttpMethod.GET, "http://test.api/pathX/{id}", vars).build();

    assertThat(requestEntity1).isEqualTo(requestEntity2);
    assertThat(requestEntity2).isEqualTo(requestEntity1);
    assertThat(requestEntity1).isNotEqualTo(requestEntity3);
    assertThat(requestEntity3).isNotEqualTo(requestEntity2);
    assertThat(requestEntity1.hashCode()).isEqualTo(requestEntity2.hashCode());
    assertThat(requestEntity1.hashCode()).isNotEqualTo(requestEntity3.hashCode());
  }

  @Test
  void constructorWithMethodAndUri() {
    HttpMethod method = HttpMethod.GET;
    URI uri = URI.create("https://example.com");
    RequestEntity<Void> entity = new RequestEntity<>(method, uri);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isNull();
    assertThat(entity.headers()).isEmpty();
    assertThat(entity.getHeaders()).isEmpty();
  }

  @Test
  void constructorWithBodyMethodAndUri() {
    String body = "test body";
    HttpMethod method = HttpMethod.POST;
    URI uri = URI.create("https://example.com");
    RequestEntity<String> entity = new RequestEntity<>(body, method, uri);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.headers()).isEmpty();
    assertThat(entity.getHeaders()).isEmpty();
    assertThat(entity.getHeaders()).isSameAs(entity.headers());
  }

  @Test
  void constructorWithBodyMethodUriAndType() {
    String body = "test body";
    HttpMethod method = HttpMethod.POST;
    URI uri = URI.create("https://example.com");
    RequestEntity<String> entity = new RequestEntity<>(body, method, uri, String.class);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getType()).isEqualTo(String.class);
    assertThat(entity.headers()).isEmpty();
    assertThat(entity.getHeaders()).isEmpty();
    assertThat(entity.getHeaders()).isSameAs(entity.headers());
  }

  @Test
  void constructorWithHeadersMethodAndUri() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Custom-Header", "custom-value");
    HttpMethod method = HttpMethod.PUT;
    URI uri = URI.create("https://example.com");
    RequestEntity<Void> entity = new RequestEntity<>(headers, method, uri);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isNull();
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Collections.singletonList("custom-value"));
  }

  @Test
  void constructorWithBodyHeadersMethodAndUri() {
    String body = "test body";
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Custom-Header", "custom-value");
    HttpMethod method = HttpMethod.PUT;
    URI uri = URI.create("https://example.com");
    RequestEntity<String> entity = new RequestEntity<>(body, headers, method, uri);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Collections.singletonList("custom-value"));
  }

  @Test
  void getTypeReturnsBodyClassWhenTypeNotSet() {
    String body = "test body";
    RequestEntity<String> entity = new RequestEntity<>(body, HttpMethod.GET, URI.create("https://example.com"));

    assertThat(entity.getType()).isEqualTo(String.class);
  }

  @Test
  void getTypeReturnsExplicitTypeWhenSet() {
    String body = "test body";
    RequestEntity<String> entity = new RequestEntity<>(body, HttpMethod.GET, URI.create("https://example.com"), String.class);

    assertThat(entity.getType()).isEqualTo(String.class);
  }

  @Test
  void getTypeReturnsNullWhenBodyAndTypeAreNull() {
    RequestEntity<Void> entity = new RequestEntity<>(HttpMethod.GET, URI.create("https://example.com"));

    assertThat(entity.getType()).isNull();
  }

  @Test
  void getURIThrowsExceptionForUriTemplateRequestEntity() {
    RequestEntity.UriTemplateRequestEntity<String> entity = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", new Object[] { 123 }, null);

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(entity::getURI)
            .withMessageContaining("The RequestEntity was created with a URI template and variables");
  }

  @Test
  void equalsAndHashCode() {
    RequestEntity<String> entity1 = new RequestEntity<>("body", HttpMethod.GET, URI.create("https://example.com"));
    RequestEntity<String> entity2 = new RequestEntity<>("body", HttpMethod.GET, URI.create("https://example.com"));
    RequestEntity<String> entity3 = new RequestEntity<>("body", HttpMethod.POST, URI.create("https://example.com"));

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
    assertThat(entity1).isNotEqualTo(entity3);
  }

  @Test
  void toStringReturnsFormattedString() {
    RequestEntity<String> entity = new RequestEntity<>("body", HttpMethod.GET, URI.create("https://example.com"));

    assertThat(entity.toString()).isEqualTo("<GET https://example.com,body,[]>");
  }

  @Test
  void staticGetMethodWithUri() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.get(URI.create("https://example.com"));

    RequestEntity<Void> entity = builder.build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
  }

  @Test
  void staticGetMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.get("https://example.com/{id}", 123);

    assertThat(builder).isNotNull();
  }

  @Test
  void staticPostMethodWithUri() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void staticPostMethodWithUriTemplate() {
    RequestEntity.BodyBuilder builder = RequestEntity.post("https://example.com/{id}", 123);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void staticPutMethodWithUri() {
    RequestEntity.BodyBuilder builder = RequestEntity.put(URI.create("https://example.com"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PUT);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void staticDeleteMethodWithUri() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.delete(URI.create("https://example.com"));

    RequestEntity<Void> entity = builder.build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.DELETE);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
  }

  @Test
  void builderHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Custom-Header", "custom-value");

    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.headers(headers);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Collections.singletonList("custom-value"));
  }

  @Test
  void builderAccept() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
  }

  @Test
  void builderContentType() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentType(MediaType.APPLICATION_JSON);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void builderContentLength() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentLength(100L);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getContentLength()).isEqualTo(100L);
  }

  @Test
  void uriTemplateRequestEntityProperties() {
    Map<String, Object> varsMap = new HashMap<>();
    varsMap.put("id", 123);
    RequestEntity.UriTemplateRequestEntity<String> entity = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, varsMap);

    assertThat(entity.getUriTemplate()).isEqualTo("https://example.com/{id}");
    assertThat(entity.getVarsMap()).isEqualTo(varsMap);
    assertThat(entity.getVars()).isNull();
  }

  @Test
  void uriTemplateRequestEntityEqualsAndHashCode() {
    Map<String, Object> varsMap = new HashMap<>();
    varsMap.put("id", 123);
    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, varsMap);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, varsMap);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void uriTemplateRequestEntityToString() {
    Map<String, Object> varsMap = new HashMap<>();
    varsMap.put("id", 123);
    RequestEntity.UriTemplateRequestEntity<String> entity = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, varsMap);

    assertThat(entity.toString()).isEqualTo("<GET https://example.com/{id},body,[]>");
  }

  @Test
  void constructorAllParameters() {
    String body = "test body";
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Custom-Header", "custom-value");
    HttpMethod method = HttpMethod.POST;
    URI uri = URI.create("https://example.com");

    RequestEntity<String> entity = new RequestEntity<>(body, headers, method, uri, String.class);

    assertThat(entity.getMethod()).isEqualTo(method);
    assertThat(entity.getURI()).isEqualTo(uri);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getType()).isEqualTo(String.class);
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Collections.singletonList("custom-value"));
  }

  @Test
  void staticHeadMethodWithUri() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.head(URI.create("https://example.com"));

    RequestEntity<Void> entity = builder.build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.HEAD);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
  }

  @Test
  void staticHeadMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.head("https://example.com/{id}", 123);

    assertThat(builder).isNotNull();
  }

  @Test
  void staticPatchMethodWithUri() {
    RequestEntity.BodyBuilder builder = RequestEntity.patch(URI.create("https://example.com"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PATCH);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void staticPatchMethodWithUriTemplate() {
    RequestEntity.BodyBuilder builder = RequestEntity.patch("https://example.com/{id}", 123);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PATCH);
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void staticOptionsMethodWithUri() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.options(URI.create("https://example.com"));

    RequestEntity<Void> entity = builder.build();
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.OPTIONS);
    assertThat(entity.getURI().toString()).isEqualTo("https://example.com");
  }

  @Test
  void staticOptionsMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.options("https://example.com/{id}", 123);

    assertThat(builder).isNotNull();
  }

  @Test
  void staticDeleteMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.delete("https://example.com/{id}", 123);

    assertThat(builder).isNotNull();
  }

  @Test
  void builderHeaderWithMultipleValues() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.header("Custom-Header", "value1", "value2");

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Arrays.asList("value1", "value2"));
  }

  @Test
  void builderHeadersWithEmptyHeaders() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.headers(HttpHeaders.forWritable());

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.headers()).isEmpty();
    assertThat(entity.getHeaders()).isEmpty();
  }

  @Test
  void builderHeadersConsumer() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.headers(headers -> headers.set("Custom-Header", "custom-value"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders()).containsEntry("Custom-Header", Collections.singletonList("custom-value"));
  }

  @Test
  void builderAcceptCharset() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.acceptCharset(StandardCharsets.UTF_8, StandardCharsets.US_ASCII);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getAcceptCharset()).containsExactly(StandardCharsets.UTF_8, StandardCharsets.US_ASCII);
  }

  @Test
  void builderIfModifiedSinceWithZonedDateTime() {
    ZonedDateTime dateTime = ZonedDateTime.now();
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.ifModifiedSince(dateTime);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getFirst(HttpHeaders.IF_MODIFIED_SINCE)).isNotNull();
  }

  @Test
  void builderIfModifiedSinceWithInstant() {
    Instant instant = Instant.now();
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.ifModifiedSince(instant);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getFirst(HttpHeaders.IF_MODIFIED_SINCE)).isNotNull();
  }

  @Test
  void builderIfModifiedSinceWithLong() {
    long timestamp = System.currentTimeMillis();
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.ifModifiedSince(timestamp);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getFirst(HttpHeaders.IF_MODIFIED_SINCE)).isNotNull();
  }

  @Test
  void builderIfNoneMatch() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.ifNoneMatch("\"etag1\"", "\"etag2\"");

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getIfNoneMatch()).containsExactly("\"etag1\"", "\"etag2\"");
  }

  @Test
  void builderContentLengthWithDataSize() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentLength(DataSize.ofBytes(100));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getContentLength()).isEqualTo(100L);
  }

  @Test
  void builderContentTypeWithString() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentType("application/json");

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void builderBodyWithType() {
    List<String> body = Arrays.asList("foo", "bar");
    ParameterizedTypeReference<List<String>> typeReference = new ParameterizedTypeReference<List<String>>() { };

    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    RequestEntity<List<String>> entity = builder.body(body, typeReference.getType());

    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getType()).isEqualTo(typeReference.getType());
  }

  @Test
  void methodWithUriTemplateAndArrayVars() {
    RequestEntity.BodyBuilder builder = RequestEntity.method(HttpMethod.POST, "https://example.com/{id}", 123);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);

    RequestEntity.UriTemplateRequestEntity<String> uriTemplateEntity =
            (RequestEntity.UriTemplateRequestEntity<String>) entity;
    assertThat(uriTemplateEntity.getUriTemplate()).isEqualTo("https://example.com/{id}");
    assertThat(uriTemplateEntity.getVars()).containsExactly(123);
  }

  @Test
  void methodWithUriTemplateAndMapVars() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("id", 123);
    RequestEntity.BodyBuilder builder = RequestEntity.method(HttpMethod.POST, "https://example.com/{id}", vars);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);

    RequestEntity.UriTemplateRequestEntity<String> uriTemplateEntity =
            (RequestEntity.UriTemplateRequestEntity<String>) entity;
    assertThat(uriTemplateEntity.getUriTemplate()).isEqualTo("https://example.com/{id}");
    assertThat(uriTemplateEntity.getVarsMap()).isEqualTo(vars);
  }

  @Test
  void uriTemplateRequestEntityEqualityWithArrayVars() {
    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", new Object[] { 123 }, null);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", new Object[] { 123 }, null);
    RequestEntity.UriTemplateRequestEntity<String> entity3 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", new Object[] { 456 }, null);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
    assertThat(entity1).isNotEqualTo(entity3);
  }

  @Test
  void getURIWithRegularRequestEntity() {
    URI uri = URI.create("https://example.com");
    RequestEntity<String> entity = new RequestEntity<>("body", HttpMethod.GET, uri);

    assertThat(entity.getURI()).isEqualTo(uri);
  }

  @Test
  void formatMethod() {
    String result = RequestEntity.format(HttpMethod.GET, "https://example.com", "body", null);
    assertThat(result).isEqualTo("<GET https://example.com,body,[]>");
  }

  @Test
  void formatMethodWithHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Custom-Header", "custom-value");
    String result = RequestEntity.format(HttpMethod.GET, "https://example.com", "body", headers);
    assertThat(result).isEqualTo("<GET https://example.com,body,[Custom-Header:\"custom-value\"]>");
  }

  @Test
  void defaultBodyBuilderWithUriTemplateArray() {
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(
            HttpMethod.POST, "https://example.com/{id}", 123);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
  }

  @Test
  void defaultBodyBuilderWithUriTemplateMap() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("id", 123);
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(
            HttpMethod.POST, "https://example.com/{id}", vars);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
  }

  @Test
  void defaultBodyBuilderWithoutUriThrowsException() {
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(HttpMethod.POST, null, (Object[]) null);

    assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("Neither URI nor URI template");
  }

  @Test
  void builderAcceptWithNoMediaTypes() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.accept();

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getAccept()).isEmpty();
  }

  @Test
  void builderAcceptCharsetWithNoCharsets() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.acceptCharset();

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getAcceptCharset()).isEmpty();
  }

  @Test
  void builderHeadersConsumerModifyExistingHeader() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.header("Existing-Header", "original-value");
    builder.headers(headers -> headers.set("Existing-Header", "modified-value"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getFirst("Existing-Header")).isEqualTo("modified-value");
  }

  @Test
  void builderHeadersConsumerRemoveHeader() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.header("ToRemove-Header", "value");
    builder.headers(headers -> headers.remove("ToRemove-Header"));

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().containsKey("ToRemove-Header")).isFalse();
  }

  @Test
  void builderContentTypeWithNullMediaType() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentType((MediaType) null);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.headers().getContentType()).isNull();
  }

  @Test
  void builderContentTypeWithNullString() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentType((String) null);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity.getHeaders().getContentType()).isNull();
  }

  @Test
  void builderBodyWithNullBody() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));

    RequestEntity<String> entity = builder.body(null);
    assertThat(entity.getBody()).isNull();
  }

  @Test
  void builderBodyWithNullBodyAndType() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));

    RequestEntity<String> entity = builder.body(null, String.class);
    assertThat(entity.getBody()).isNull();
    assertThat(entity.getType()).isEqualTo(String.class);
  }

  @Test
  void methodWithNullUriTemplateAndArrayVars() {
    RequestEntity.BodyBuilder builder = RequestEntity.method(HttpMethod.POST, (String) null, 123);

    assertThatIllegalStateException()
            .isThrownBy(() -> builder.build())
            .withMessage("Neither URI nor URI template");
  }

  @Test
  void methodWithNullUriTemplateAndMapVars() {
    RequestEntity.BodyBuilder builder = RequestEntity.method(HttpMethod.POST, (String) null, Collections.emptyMap());

    assertThatIllegalStateException()
            .isThrownBy(() -> builder.build())
            .withMessage("Neither URI nor URI template");
  }

  @Test
  void uriTemplateRequestEntityEqualityWithNullArrayVars() {
    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, null);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, null);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void uriTemplateRequestEntityEqualityWithMapVarsDifferentOrder() {
    Map<String, Object> vars1 = new HashMap<>();
    vars1.put("id", 123);
    vars1.put("name", "test");

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("name", "test");
    vars2.put("id", 123);

    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, vars1);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, vars2);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void uriTemplateRequestEntityEqualityWithDifferentMapVars() {
    Map<String, Object> vars1 = new HashMap<>();
    vars1.put("id", 123);

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("id", 456);

    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, vars1);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, vars2);

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void uriTemplateRequestEntityEqualityWithOneMapAndOneArray() {
    Map<String, Object> varsMap = new HashMap<>();
    varsMap.put("id", 123);

    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", null, varsMap);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}", new Object[] { 123 }, null);

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void defaultBodyBuilderWithNullUri() {
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(HttpMethod.POST, (URI) null);

    assertThatIllegalStateException()
            .isThrownBy(() -> builder.build())
            .withMessage("Neither URI nor URI template");
  }

  @Test
  void defaultBodyBuilderWithUriTemplateAndNullArrayVars() {
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(
            HttpMethod.POST, "https://example.com/{id}", (Object[]) null);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
  }

  @Test
  void defaultBodyBuilderWithUriTemplateAndNullMapVars() {
    RequestEntity.DefaultBodyBuilder builder = new RequestEntity.DefaultBodyBuilder(
            HttpMethod.POST, "https://example.com/{id}", (Map<String, ?>) null);

    RequestEntity<String> entity = builder.body("test");
    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
  }

  @Test
  void formatMethodWithNullMethod() {
    String result = RequestEntity.format(null, "https://example.com", "body", null);
    assertThat(result).isEqualTo("<null https://example.com,body,[]>");
  }

  @Test
  void constructorWithNullMethodAndValidUri() {
    URI uri = URI.create("https://example.com");
    RequestEntity<Void> entity = new RequestEntity<>(null, uri);

    assertThat(entity.getMethod()).isNull();
    assertThat(entity.getURI()).isEqualTo(uri);
  }

  @Test
  void getTypeWithExplicitTypeAndNullBody() {
    RequestEntity<Void> entity = new RequestEntity<>((Void) null, HttpMethod.GET, URI.create("https://example.com"), String.class);

    assertThat(entity.getType()).isEqualTo(String.class);
  }

  @Test
  void getTypeWithExplicitTypeAndNonNullBody() {
    RequestEntity<String> entity = new RequestEntity<>("body", HttpMethod.GET, URI.create("https://example.com"), Integer.class);

    assertThat(entity.getType()).isEqualTo(Integer.class);
  }

  @Test
  void testEmptyConstructor() {
    RequestEntity<Void> entity = new RequestEntity<>(HttpMethod.GET, URI.create("https://example.com"));
    assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(entity.getURI()).isEqualTo(URI.create("https://example.com"));
    assertThat(entity.getBody()).isNull();
    assertThat(entity.getType()).isNull();
  }

  @Test
  void testConstructorWithBodyAndType() {
    List<String> body = Arrays.asList("item1", "item2");
    ParameterizedTypeReference<List<String>> typeReference = new ParameterizedTypeReference<List<String>>() { };

    RequestEntity<List<String>> entity = new RequestEntity<>(body, HttpMethod.POST,
            URI.create("https://example.com"), typeReference.getType());

    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getType()).isEqualTo(typeReference.getType());
  }

  @Test
  void testGetMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.get("https://example.com/{id}", 123);
    RequestEntity<Void> entity = builder.build();

    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
    RequestEntity.UriTemplateRequestEntity<?> uriTemplateEntity =
            (RequestEntity.UriTemplateRequestEntity<?>) entity;

    assertThat(uriTemplateEntity.getUriTemplate()).isEqualTo("https://example.com/{id}");
    assertThat(uriTemplateEntity.getVars()).containsExactly(123);
  }

  @Test
  void testPutMethodWithHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Authorization", "Bearer token");
    headers.set("X-Custom-Header", "custom-value");

    RequestEntity<String> entity = new RequestEntity<>("body", headers, HttpMethod.PUT,
            URI.create("https://example.com"));

    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PUT);
    assertThat(entity.getURI()).isEqualTo(URI.create("https://example.com"));
    assertThat(entity.getBody()).isEqualTo("body");
    assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer token");
    assertThat(entity.getHeaders().getFirst("X-Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void testDeleteMethodWithUriTemplate() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.delete("https://example.com/resource/{id}", 42);
    RequestEntity<Void> entity = builder.build();

    assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
    RequestEntity.UriTemplateRequestEntity<?> uriTemplateEntity =
            (RequestEntity.UriTemplateRequestEntity<?>) entity;

    assertThat(uriTemplateEntity.getUriTemplate()).isEqualTo("https://example.com/resource/{id}");
    assertThat(uriTemplateEntity.getVars()).containsExactly(42);
  }

  @Test
  void testOptionsMethod() {
    RequestEntity.HeadersBuilder<?> builder = RequestEntity.options(URI.create("https://example.com"));
    RequestEntity<Void> entity = builder.build();

    assertThat(entity.getMethod()).isEqualTo(HttpMethod.OPTIONS);
    assertThat(entity.getURI()).isEqualTo(URI.create("https://example.com"));
    assertThat(entity.getBody()).isNull();
  }

  @Test
  void testPatchMethod() {
    RequestEntity.BodyBuilder builder = RequestEntity.patch(URI.create("https://example.com/resource/123"));
    RequestEntity<Map<String, Object>> entity = builder.body(Collections.singletonMap("status", "updated"));

    assertThat(entity.getMethod()).isEqualTo(HttpMethod.PATCH);
    assertThat(entity.getURI()).isEqualTo(URI.create("https://example.com/resource/123"));
    assertThat(entity.getBody()).containsEntry("status", "updated");
  }

  @Test
  void testBuilderHeaderManipulationWithConsumer() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));

    builder.headers(httpHeaders -> {
      httpHeaders.set("X-Original", "original-value");
      httpHeaders.set("X-To-Be-Removed", "removed-value");
    });

    builder.headers(httpHeaders -> {
      httpHeaders.set("X-Original", "replaced-value");
      httpHeaders.remove("X-To-Be-Removed");
      httpHeaders.add("X-Added-Later", "added-value");
    });

    RequestEntity<String> entity = builder.body("test");

    assertThat(entity.getHeaders().getFirst("X-Original")).isEqualTo("replaced-value");
    assertThat(entity.getHeaders().containsKey("X-To-Be-Removed")).isFalse();
    assertThat(entity.getHeaders().getFirst("X-Added-Later")).isEqualTo("added-value");
  }

  @Test
  void testBuilderContentTypeWithMediaTypeString() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentType("application/json;charset=UTF-8");

    RequestEntity<String> entity = builder.body("test");

    assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/json;charset=UTF-8"));
  }

  @Test
  void testBuilderContentLengthWithZeroValue() {
    RequestEntity.BodyBuilder builder = RequestEntity.post(URI.create("https://example.com"));
    builder.contentLength(0L);

    RequestEntity<String> entity = builder.body("");

    assertThat(entity.getHeaders().getContentLength()).isEqualTo(0L);
  }

  @Test
  void testUriTemplateRequestEntityEqualityWithSameArrayVars() {
    RequestEntity.UriTemplateRequestEntity<String> entity1 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}/{name}", new Object[] { 123, "test" }, null);
    RequestEntity.UriTemplateRequestEntity<String> entity2 = new RequestEntity.UriTemplateRequestEntity<>(
            "body", null, HttpMethod.GET, null, "https://example.com/{id}/{name}", new Object[] { 123, "test" }, null);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).hasSameHashCodeAs(entity2);
  }

  @Test
  void testToStringWithAllFields() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Custom", "value");
    RequestEntity<String> entity = new RequestEntity<>("body", headers,
            HttpMethod.POST, URI.create("https://example.com"), String.class);

    assertThat(entity.toString()).isEqualTo("<POST https://example.com,body,[X-Custom:\"value\"]>");
  }

}
