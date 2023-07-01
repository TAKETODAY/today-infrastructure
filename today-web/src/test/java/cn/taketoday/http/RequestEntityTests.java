/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link cn.taketoday.http.RequestEntity}.
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
    assertThat(requestEntity.getHeaders().containsKey(headerName)).isTrue();
    assertThat(requestEntity.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
    assertThat(requestEntity.getBody()).isEqualTo(entity);
  }

  @Test
  void uriVariablesExpansion() throws URISyntaxException {
    URI uri = UriComponentsBuilder.fromUriString("https://example.com/{foo}").buildAndExpand("bar").toUri();
    RequestEntity.get(uri).accept(MediaType.TEXT_PLAIN).build();

    String url = "https://www.{host}.com/{path}";
    String host = "example";
    String path = "foo/bar";
    URI expected = new URI("https://www.example.com/foo/bar");

    uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(host, path).toUri();
    RequestEntity<?> entity = RequestEntity.get(uri).build();
    assertThat(entity.getUrl()).isEqualTo(expected);

    Map<String, String> uriVariables = new HashMap<>(2);
    uriVariables.put("host", host);
    uriVariables.put("path", path);

    uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).toUri();
    entity = RequestEntity.get(uri).build();
    assertThat(entity.getUrl()).isEqualTo(expected);
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
    assertThat(requestEntity.getHeaders().containsKey(HttpHeaders.ACCEPT)).isTrue();
    assertThat(requestEntity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("image/gif, image/jpeg, image/png");
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
    assertThat(responseEntity.getUrl()).isEqualTo(new URI("https://example.com"));
    HttpHeaders responseHeaders = responseEntity.getHeaders();

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
    // SPR-13154
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
    // gh-27531
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

}
