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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import infra.util.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Arjen Poutsma
 * @author Marcel Overdijk
 * @author Kazuki Shimizu
 * @author TODAY 2021/4/15 14:21
 * @since 3.0
 */
class ResponseEntityTests {

  @Test
  public void normal() {
    String headerName = "My-Custom-Header";
    String headerValue1 = "HeaderValue1";
    String headerValue2 = "HeaderValue2";
    Integer entity = 42;

    ResponseEntity<Integer> responseEntity =
            ResponseEntity.status(HttpStatus.OK).header(headerName, headerValue1, headerValue2).body(entity);

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.headers().containsKey(headerName)).isTrue();
    List<String> list = responseEntity.headers().get(headerName);
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0)).isEqualTo(headerValue1);
    assertThat(list.get(1)).isEqualTo(headerValue2);
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  public void okNoBody() {
    ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void okEntity() {
    Integer entity = 42;
    ResponseEntity<Integer> responseEntity = ResponseEntity.ok(entity);

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  public void ofOptional() {
    Integer entity = 42;
    ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.of(entity));

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  public void ofEmptyOptional() {
    ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.empty());

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void createdLocation() throws URISyntaxException {
    URI location = new URI("location");
    ResponseEntity<Void> responseEntity = ResponseEntity.created(location).build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(responseEntity.headers().containsKey(HttpHeaders.LOCATION)).isTrue();
    assertThat(responseEntity.headers().getFirst(HttpHeaders.LOCATION)).isEqualTo(location.toString());
    assertThat(responseEntity.getBody()).isNull();

    ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
  }

  @Test
  public void acceptedNoBody() throws URISyntaxException {
    ResponseEntity<Void> responseEntity = ResponseEntity.accepted().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void acceptedNoBodyWithAlternativeBodyType() throws URISyntaxException {
    ResponseEntity<String> responseEntity = ResponseEntity.accepted().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void noContent() throws URISyntaxException {
    ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void badRequest() throws URISyntaxException {
    ResponseEntity<Void> responseEntity = ResponseEntity.badRequest().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody()).isNull();
  }

  @Test
  public void notFound() throws URISyntaxException {
    ResponseEntity<Void> responseEntity = ResponseEntity.notFound().build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(responseEntity.getBody()).isNull();

    assertThat(ResponseEntity.notFound().body("body").getBody()).isEqualTo("body");
  }

  @Test
  public void unprocessableEntity() throws URISyntaxException {
    ResponseEntity<String> responseEntity = ResponseEntity.unprocessableEntity().body("error");

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(responseEntity.getBody()).isEqualTo("error");
  }

  @Test
  public void headers() throws URISyntaxException {
    URI location = new URI("location");
    long contentLength = 67890;
    MediaType contentType = MediaType.TEXT_PLAIN;

    ResponseEntity<Void> responseEntity = ResponseEntity.ok().
            allow(HttpMethod.GET).
            lastModified(12345L).
            location(location).
            contentLength(contentLength).
            contentType(contentType).
            headers(headers -> Assertions.assertThat(headers).hasSize(5)).
            build();

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders responseHeaders = responseEntity.headers();

    assertThat(responseHeaders.getFirst(HttpHeaders.ALLOW)).isEqualTo(HttpMethod.GET.name());
    assertThat(responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED)).isEqualTo("Thu, 01 Jan 1970 00:00:12 GMT");
    assertThat(responseHeaders.getFirst(HttpHeaders.LOCATION)).isEqualTo(location.toASCIIString());
    assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo(String.valueOf(contentLength));
    assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType.toString());

    assertThat((Object) responseEntity.getBody()).isNull();
  }

  @Test
  public void Etagheader() throws URISyntaxException {

    ResponseEntity<Void> responseEntity = ResponseEntity.ok().eTag("\"foo\"").build();
    assertThat(responseEntity.headers().getETag()).isEqualTo("\"foo\"");

    responseEntity = ResponseEntity.ok().eTag("foo").build();
    assertThat(responseEntity.headers().getETag()).isEqualTo("\"foo\"");

    responseEntity = ResponseEntity.ok().eTag("W/\"foo\"").build();
    assertThat(responseEntity.headers().getETag()).isEqualTo("W/\"foo\"");

    responseEntity = ResponseEntity.ok().eTag(null).build();
    assertThat(responseEntity.headers().getETag()).isNull();
  }

  @Test
  public void headersCopy() {
    HttpHeaders customHeaders = HttpHeaders.forWritable();
    customHeaders.setOrRemove("X-CustomHeader", "vale");

    ResponseEntity<Void> responseEntity = ResponseEntity.ok().headers(customHeaders).build();
    HttpHeaders responseHeaders = responseEntity.headers();

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseHeaders.size()).isEqualTo(1);
    assertThat(responseHeaders.get("X-CustomHeader").size()).isEqualTo(1);
    assertThat(responseHeaders.getFirst("X-CustomHeader")).isEqualTo("vale");
  }

  @Test
  public void headersCopyWithEmptyAndNull() {
    ResponseEntity<Void> responseEntityWithEmptyHeaders =
            ResponseEntity.ok().headers(new DefaultHttpHeaders()).build();
    ResponseEntity<Void> responseEntityWithNullHeaders =
            ResponseEntity.ok().headers((HttpHeaders) null).build();

    assertThat(responseEntityWithEmptyHeaders.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntityWithEmptyHeaders.headers().isEmpty()).isTrue();
    assertThat(responseEntityWithNullHeaders.toString()).isEqualTo(responseEntityWithEmptyHeaders.toString());
  }

  @Test
  public void emptyCacheControl() {
    Integer entity = 42;

    ResponseEntity<Integer> responseEntity =
            ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.empty())
                    .body(entity);

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.hasHeader(HttpHeaders.CACHE_CONTROL)).isFalse();
    assertThat(responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  public void cacheControl() {
    Integer entity = 42;

    ResponseEntity<Integer> responseEntity =
            ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().
                            mustRevalidate().proxyRevalidate().sMaxAge(30, TimeUnit.MINUTES))
                    .body(entity);

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.headers().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
    assertThat(responseEntity.getBody()).isEqualTo((int) entity);
    String cacheControlHeader = responseEntity.headers().getCacheControl();
    assertThat(cacheControlHeader).isEqualTo(
            "max-age=3600, must-revalidate, private, proxy-revalidate, s-maxage=1800");
  }

  @Test
  public void cacheControlNoCache() {
    Integer entity = 42;

    ResponseEntity<Integer> responseEntity =
            ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.noStore())
                    .body(entity);

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.headers().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);

    String cacheControlHeader = responseEntity.headers().getCacheControl();
    assertThat(cacheControlHeader).isEqualTo("no-store");
  }

  @Test
  public void statusCodeAsInt() {
    Integer entity = 42;
    ResponseEntity<Integer> responseEntity = ResponseEntity.status(200).body(entity);

    assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  public void customStatusCode() {
    Integer entity = 42;
    ResponseEntity<Integer> responseEntity = ResponseEntity.status(299).body(entity);

    assertThat(responseEntity.getStatusCodeValue()).isEqualTo(299);
    assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
  }

  @Test
  void problemDetail() {
    ProblemDetail body = ProblemDetail.forRawStatusCode(400);
    ResponseEntity<ProblemDetail> response = ResponseEntity.of(body)
            .build();
    assertThat(response.getBody()).isEqualTo(body);
  }

  @Test
  void equalsAndHashCode_withSameStatusBodyAndHeaders_shouldBeEqual() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Custom-Header", "value1");
    ResponseEntity<String> entity1 = new ResponseEntity<>("body", headers1, HttpStatus.OK);

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Custom-Header", "value1");
    ResponseEntity<String> entity2 = new ResponseEntity<>("body", headers2, HttpStatus.OK);

    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
  }

  @Test
  void equalsAndHashCode_withDifferentStatus_shouldNotBeEqual() {
    ResponseEntity<String> entity1 = new ResponseEntity<>("body", HttpStatus.OK);
    ResponseEntity<String> entity2 = new ResponseEntity<>("body", HttpStatus.NOT_FOUND);

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void equalsAndHashCode_withDifferentBody_shouldNotBeEqual() {
    ResponseEntity<String> entity1 = new ResponseEntity<>("body1", HttpStatus.OK);
    ResponseEntity<String> entity2 = new ResponseEntity<>("body2", HttpStatus.OK);

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void equalsAndHashCode_withDifferentHeaders_shouldNotBeEqual() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Custom-Header", "value1");
    ResponseEntity<String> entity1 = new ResponseEntity<>("body", headers1, HttpStatus.OK);

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Custom-Header", "value2");
    ResponseEntity<String> entity2 = new ResponseEntity<>("body", headers2, HttpStatus.OK);

    assertThat(entity1).isNotEqualTo(entity2);
  }

  @Test
  void equals_withNull_shouldNotBeEqual() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", HttpStatus.OK);
    assertThat(entity).isNotEqualTo(null);
  }

  @Test
  void equals_withSameReference_shouldBeEqual() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", HttpStatus.OK);
    assertThat(entity).isEqualTo(entity);
  }

  @Test
  void equals_withDifferentClass_shouldNotBeEqual() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", HttpStatus.OK);
    assertThat(entity).isNotEqualTo("different type");
  }

  @Test
  void toString_withBodyAndHeaders_shouldReturnFormattedString() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Custom-Header", "value");
    ResponseEntity<String> entity = new ResponseEntity<>("test body", headers, HttpStatus.OK);

    String result = entity.toString();
    assertThat(result).contains("200");
    assertThat(result).contains("OK");
    assertThat(result).contains("test body");
    assertThat(result).contains("Custom-Header");
  }

  @Test
  void toString_withoutBody_shouldReturnFormattedString() {
    ResponseEntity<Void> entity = new ResponseEntity<>(null, null, HttpStatus.NO_CONTENT);

    String result = entity.toString();
    assertThat(result).contains("204");
    assertThat(result).contains("No Content");
  }

  @Test
  void getStatusCode_withRawStatusCode_shouldReturnCorrectEnum() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", null, 200);
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void getStatusCodeValue_withEnumStatus_shouldReturnIntValue() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", HttpStatus.CREATED);
    assertThat(entity.getStatusCodeValue()).isEqualTo(201);
  }

  @Test
  void getStatusCodeValue_withRawStatus_shouldReturnSameValue() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", null, 418);
    assertThat(entity.getStatusCodeValue()).isEqualTo(418);
  }

  @Test
  void status_withHttpStatusCode_shouldCreateBuilder() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CONFLICT);
    ResponseEntity<String> entity = builder.body("conflict");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void status_withIntValue_shouldCreateBuilder() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(418);
    ResponseEntity<String> entity = builder.body("I'm a teapot");

    assertThat(entity.getStatusCodeValue()).isEqualTo(418);
  }

  @Test
  void ok_withBody_shouldCreateOkResponse() {
    ResponseEntity<String> entity = ResponseEntity.ok("success");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo("success");
  }

  @Test
  void created_withUri_shouldSetLocationHeader() throws Exception {
    URI location = new URI("http://example.com/resource");
    ResponseEntity.BodyBuilder builder = ResponseEntity.created(location);

    ResponseEntity<String> entity = builder.body("created");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(entity.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void accepted_shouldCreateAcceptedResponse() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.accepted();
    ResponseEntity<String> entity = builder.body("accepted");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void noContent_shouldCreateNoContentResponse() {
    ResponseEntity.HeadersBuilder<?> builder = ResponseEntity.noContent();
    ResponseEntity<Void> entity = builder.build();

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(entity.getBody()).isNull();
  }

  @Test
  void badRequest_shouldCreateBadRequestResponse() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.badRequest();
    ResponseEntity<String> entity = builder.body("bad request");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void notFound_shouldCreateNotFoundResponse() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.notFound();
    ResponseEntity<String> entity = builder.body("not found");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unprocessableEntity_shouldCreateUnprocessableEntityResponse() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.unprocessableEntity();
    ResponseEntity<String> entity = builder.body("unprocessable");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  void header_shouldAddSingleHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .header("X-Custom", "custom-value")
            .body("test");

    assertThat(entity.headers().getFirst("X-Custom")).isEqualTo("custom-value");
  }

  @Test
  void headers_withConsumer_shouldModifyHeaders() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .headers(headers -> headers.add("X-Custom", "value"))
            .body("test");

    assertThat(entity.headers().getFirst("X-Custom")).isEqualTo("value");
  }

  @Test
  void allow_shouldSetAllowHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .allow(HttpMethod.GET, HttpMethod.POST)
            .body("test");

    assertThat(entity.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
  }

  @Test
  void contentLength_withLong_shouldSetContentLengthHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .contentLength(1024L)
            .body("test");

    assertThat(entity.headers().getContentLength()).isEqualTo(1024L);
  }

  @Test
  void contentType_withMediaType_shouldSetContentTypeHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"key\":\"value\"}");

    assertThat(entity.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void location_shouldSetLocationHeader() throws Exception {
    URI uri = new URI("http://example.com");
    ResponseEntity<String> entity = ResponseEntity.ok()
            .location(uri)
            .body("test");

    assertThat(entity.headers().getLocation()).isEqualTo(uri);
  }

  @Test
  void eTag_shouldSetETagHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .eTag("\"v1\"")
            .body("test");

    assertThat(entity.headers().getETag()).isEqualTo("\"v1\"");
  }

  @Test
  void lastModified_withZonedDateTime_shouldSetLastModifiedHeader() {
    ZonedDateTime dateTime = ZonedDateTime.now();
    ResponseEntity<String> entity = ResponseEntity.ok()
            .lastModified(dateTime)
            .body("test");

    assertThat(entity.headers().getLastModified()).isNotNull();
  }

  @Test
  void cacheControl_shouldSetCacheControlHeader() {
    CacheControl cacheControl = CacheControl.maxAge(30, TimeUnit.SECONDS);
    ResponseEntity<String> entity = ResponseEntity.ok()
            .cacheControl(cacheControl)
            .body("test");

    assertThat(entity.headers().getCacheControl()).isEqualTo("max-age=30");
  }

  @Test
  void varyBy_shouldSetVaryHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .varyBy("Accept-Encoding", "User-Agent")
            .body("test");

    assertThat(entity.headers().getVary()).containsExactlyInAnyOrder("Accept-Encoding", "User-Agent");
  }

  @Test
  void of_withPresentOptional_shouldReturnOkResponse() {
    ResponseEntity<String> entity = ResponseEntity.of(Optional.of("present"));

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo("present");
  }

  @Test
  void of_withEmptyOptional_shouldReturnNotFoundResponse() {
    ResponseEntity<String> entity = ResponseEntity.of(Optional.empty());

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(entity.getBody()).isNull();
  }

  @Test
  void constructor_withStatusCodeOnly_shouldCreateInstance() {
    ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isNull();
    assertThat(responseEntity.headers().isEmpty()).isTrue();
  }

  @Test
  void constructor_withBodyAndStatusCode_shouldCreateInstance() {
    String body = "response body";
    ResponseEntity<String> responseEntity = new ResponseEntity<>(body, HttpStatus.CREATED);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(responseEntity.getBody()).isEqualTo(body);
    assertThat(responseEntity.headers().isEmpty()).isTrue();
  }

  @Test
  void constructor_withHeadersAndStatusCode_shouldCreateInstance() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Custom-Header", "custom-value");
    ResponseEntity<Void> responseEntity = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(responseEntity.getBody()).isNull();
    assertThat(responseEntity.headers().getFirst("Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void constructor_withBodyHeadersAndStatusCode_shouldCreateInstance() {
    String body = "response body";
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Custom-Header", "custom-value");
    ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, HttpStatus.OK);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isEqualTo(body);
    assertThat(responseEntity.headers().getFirst("Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void constructor_withBodyHeadersAndRawStatusCode_shouldCreateInstance() {
    String body = "response body";
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Custom-Header", "custom-value");
    ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, 200);

    assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity.getBody()).isEqualTo(body);
    assertThat(responseEntity.headers().getFirst("Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void constructor_withNullStatusCode_shouldThrowException() {
    assertThatThrownBy(() -> new ResponseEntity<>((HttpStatusCode) null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getStatusCode_withEnum_shouldReturnSameEnum() {
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", HttpStatus.OK);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void getStatusCode_withRawValue_shouldReturnEnum() {
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", null, 418);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
  }

  @Test
  void getStatusCodeValue_withEnum_shouldReturnValue() {
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", HttpStatus.CREATED);
    assertThat(responseEntity.getStatusCodeValue()).isEqualTo(201);
  }

  @Test
  void getStatusCodeValue_withRawValue_shouldReturnSameValue() {
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", null, 299);
    assertThat(responseEntity.getStatusCodeValue()).isEqualTo(299);
  }

  @Test
  void status_withNull_shouldThrowException() {
    assertThatThrownBy(() -> ResponseEntity.status((HttpStatusCode) null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ok_shouldCreateBuilder() {
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    ResponseEntity<String> entity = builder.body("test");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void of_withNullOptional_shouldThrowException() {
    assertThatThrownBy(() -> ResponseEntity.of((Optional<String>) null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void of_withProblemDetail_shouldCreateBuilder() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(400);
    ResponseEntity.HeadersBuilder<?> builder = ResponseEntity.of(problemDetail);
    ResponseEntity<ProblemDetail> entity = builder.build();

    assertThat(entity.getStatusCodeValue()).isEqualTo(400);
    assertThat(entity.getBody()).isEqualTo(problemDetail);
  }

  @Test
  void lastModified_withInstant_shouldSetHeader() {
    Instant instant = Instant.now();
    ResponseEntity<String> entity = ResponseEntity.ok()
            .lastModified(instant)
            .body("test");

    assertThat(entity.headers().getLastModified()).isNotNull();
  }

  @Test
  void lastModified_withLong_shouldSetHeader() {
    long timestamp = System.currentTimeMillis();
    ResponseEntity<String> entity = ResponseEntity.ok()
            .lastModified(timestamp)
            .body("test");

    assertThat(entity.headers().getLastModified()).isNotNull();
  }

  @Test
  void contentLength_withDataSize_shouldSetHeader() {
    DataSize dataSize = DataSize.ofKilobytes(1);
    ResponseEntity<String> entity = ResponseEntity.ok()
            .contentLength(dataSize)
            .body("test");

    assertThat(entity.headers().getContentLength()).isEqualTo(1024L);
  }

  @Test
  void contentType_withString_shouldSetHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .contentType("application/json")
            .body("test");

    assertThat(entity.headers().getContentType().toString()).isEqualTo("application/json");
  }

  @Test
  void headers_withNull_shouldNotThrowException() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .headers((HttpHeaders) null)
            .body("test");

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo("test");
  }

  @Test
  void varyBy_withMultipleHeaders_shouldSetHeader() {
    ResponseEntity<String> entity = ResponseEntity.ok()
            .varyBy("Accept", "Accept-Language", "Accept-Encoding")
            .body("test");

    assertThat(entity.headers().getVary()).containsExactlyInAnyOrder("Accept", "Accept-Language", "Accept-Encoding");
  }

  @Test
  void toString_withHttpStatus_shouldIncludeReasonPhrase() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", HttpStatus.OK);
    String result = entity.toString();

    assertThat(result).contains("200");
    assertThat(result).contains("OK");
  }

  @Test
  void toString_withCustomStatusCode_shouldNotIncludeReasonPhrase() {
    ResponseEntity<String> entity = new ResponseEntity<>("body", null, 299);
    String result = entity.toString();

    assertThat(result).contains("299");
    assertThat(result).doesNotContain("Unknown");
  }

}
