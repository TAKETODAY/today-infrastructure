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

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.util.UriComponentsBuilder.ParserType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link UriComponentsBuilder}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author David Eckel
 */
class UriComponentsBuilderTests {

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void examplesInReferenceManual(ParserType parserType) {
    final String expected = "/hotel%20list/New%20York?q=foo%2Bbar";

    URI uri = UriComponentsBuilder.forPath("/hotel list/{city}")
            .queryParam("q", "{q}")
            .encode()
            .buildAndExpand("New York", "foo+bar")
            .toURI();
    assertThat(uri).asString().isEqualTo(expected);

    uri = UriComponentsBuilder.forPath("/hotel list/{city}")
            .queryParam("q", "{q}")
            .build("New York", "foo+bar");
    assertThat(uri).asString().isEqualTo(expected);

    uri = UriComponentsBuilder.forURIString("/hotel list/{city}?q={q}", parserType)
            .build("New York", "foo+bar");
    assertThat(uri).asString().isEqualTo(expected);
  }

  @Test
  void plain() {
    UriComponentsBuilder builder = UriComponentsBuilder.create();
    UriComponents result = builder.scheme("https").host("example.com")
            .path("foo").queryParam("bar").fragment("baz").build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("example.com");
    assertThat(result.getPath()).isEqualTo("foo");
    assertThat(result.getQuery()).isEqualTo("bar");
    assertThat(result.getFragment()).isEqualTo("baz");

    URI expected = URI.create("https://example.com/foo?bar#baz");
    assertThat(result.toURI()).as("Invalid result URI").isEqualTo(expected);
  }

  @Test
  void multipleFromSameBuilder() {
    UriComponentsBuilder builder = UriComponentsBuilder.create()
            .scheme("https").host("example.com").pathSegment("foo");
    UriComponents result1 = builder.build();
    builder = builder.pathSegment("foo2").queryParam("bar").fragment("baz");
    UriComponents result2 = builder.build();

    assertThat(result1.getScheme()).isEqualTo("https");
    assertThat(result1.getHost()).isEqualTo("example.com");
    assertThat(result1.getPath()).isEqualTo("/foo");
    URI expected = URI.create("https://example.com/foo");
    assertThat(result1.toURI()).as("Invalid result URI").isEqualTo(expected);

    assertThat(result2.getScheme()).isEqualTo("https");
    assertThat(result2.getHost()).isEqualTo("example.com");
    assertThat(result2.getPath()).isEqualTo("/foo/foo2");
    assertThat(result2.getQuery()).isEqualTo("bar");
    assertThat(result2.getFragment()).isEqualTo("baz");
    expected = URI.create("https://example.com/foo/foo2?bar#baz");
    assertThat(result2.toURI()).as("Invalid result URI").isEqualTo(expected);
  }

  @Test
  void fromPath() {
    UriComponents result = UriComponentsBuilder.forPath("foo").queryParam("bar").fragment("baz").build();

    assertThat(result.getPath()).isEqualTo("foo");
    assertThat(result.getQuery()).isEqualTo("bar");
    assertThat(result.getFragment()).isEqualTo("baz");
    assertThat(result.toUriString()).as("Invalid result URI String").isEqualTo("foo?bar#baz");

    URI expected = URI.create("foo?bar#baz");
    assertThat(result.toURI()).as("Invalid result URI").isEqualTo(expected);

    result = UriComponentsBuilder.forPath("/foo").build();
    assertThat(result.getPath()).isEqualTo("/foo");

    expected = URI.create("/foo");
    assertThat(result.toURI()).as("Invalid result URI").isEqualTo(expected);
  }

  @Test
  void fromHierarchicalUri() {
    URI uri = URI.create("https://example.com/foo?bar#baz");
    UriComponents result = UriComponentsBuilder.forURI(uri).build();

    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getHost()).isEqualTo("example.com");
    assertThat(result.getPath()).isEqualTo("/foo");
    assertThat(result.getQuery()).isEqualTo("bar");
    assertThat(result.getFragment()).isEqualTo("baz");
    assertThat(result.toURI()).as("Invalid result URI").isEqualTo(uri);
  }

  @Test
  void fromOpaqueUri() {
    URI uri = URI.create("mailto:foo@bar.com#baz");
    UriComponents result = UriComponentsBuilder.forURI(uri).build();

    assertThat(result.getScheme()).isEqualTo("mailto");
    assertThat(result.getSchemeSpecificPart()).isEqualTo("foo@bar.com");
    assertThat(result.getFragment()).isEqualTo("baz");
    assertThat(result.toURI()).as("Invalid result URI").isEqualTo(uri);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriEncodedQuery(ParserType parserType) {
    URI uri = URI.create("https://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
    String fromUri = UriComponentsBuilder.forURI(uri).build().getQueryParams().get("param").get(0);
    String fromUriString = UriComponentsBuilder.forURIString(uri.toString(), parserType)
            .build().getQueryParams().get("param").get(0);

    assertThat(fromUriString).isEqualTo(fromUri);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriString(ParserType parserType) {
    UriComponents result = UriComponentsBuilder.forURIString("https://www.ietf.org/rfc/rfc3986.txt", parserType).build();
    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getUserInfo()).isNull();
    assertThat(result.getHost()).isEqualTo("www.ietf.org");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("/rfc/rfc3986.txt");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("rfc", "rfc3986.txt"));
    assertThat(result.getQuery()).isNull();
    assertThat(result.getFragment()).isNull();

    String url = "https://arjen:foobar@java.sun.com:80" +
            "/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
    result = UriComponentsBuilder.forURIString(url, parserType).build();
    assertThat(result.getScheme()).isEqualTo("https");
    assertThat(result.getUserInfo()).isEqualTo("arjen:foobar");
    assertThat(result.getHost()).isEqualTo("java.sun.com");
    assertThat(result.getPort()).isEqualTo(80);
    assertThat(result.getPath()).isEqualTo("/javase/6/docs/api/java/util/BitSet.html");
    assertThat(result.getQuery()).isEqualTo("foo=bar");
    MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(1);
    expectedQueryParams.add("foo", "bar");
    assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
    assertThat(result.getFragment()).isEqualTo("and(java.util.BitSet)");

    result = UriComponentsBuilder.forURIString("mailto:java-net@java.sun.com#baz", parserType).build();
    assertThat(result.getScheme()).isEqualTo("mailto");
    assertThat(result.getUserInfo()).isNull();
    assertThat(result.getHost()).isNull();
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getSchemeSpecificPart()).isEqualTo("java-net@java.sun.com");
    assertThat(result.getPath()).isNull();
    assertThat(result.getQuery()).isNull();
    assertThat(result.getFragment()).isEqualTo("baz");

    result = UriComponentsBuilder.forURIString("mailto:user@example.com?subject=foo", parserType).build();
    assertThat(result.getScheme()).isEqualTo("mailto");
    assertThat(result.getUserInfo()).isNull();
    assertThat(result.getHost()).isNull();
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getSchemeSpecificPart()).isEqualTo("user@example.com?subject=foo");
    assertThat(result.getPath()).isNull();
    assertThat(result.getQuery()).isNull();
    assertThat(result.getFragment()).isNull();

    result = UriComponentsBuilder.forURIString("docs/guide/collections/designfaq.html#28", parserType).build();
    assertThat(result.getScheme()).isNull();
    assertThat(result.getUserInfo()).isNull();
    assertThat(result.getHost()).isNull();
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("docs/guide/collections/designfaq.html");
    assertThat(result.getQuery()).isNull();
    assertThat(result.getFragment()).isEqualTo("28");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriStringQueryParamWithReservedCharInValue(ParserType parserType) {
    String uri = "https://www.google.com/ig/calculator?q=1USD=?EUR";
    UriComponents result = UriComponentsBuilder.forURIString(uri, parserType).build();

    assertThat(result.getQuery()).isEqualTo("q=1USD=?EUR");
    assertThat(result.getQueryParams().getFirst("q")).isEqualTo("1USD=?EUR");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriStringQueryParamEncodedAndContainingPlus(ParserType parserType) {
    String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
    URI uri = UriComponentsBuilder.forURIString(httpUrl, parserType).build(true).toURI();

    assertThat(uri.toString()).isEqualTo(httpUrl);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriStringIPv6Host(ParserType parserType) {
    UriComponents result = UriComponentsBuilder
            .forURIString("http://[1abc:2abc:3abc::5ABC:6abc]:8080/resource", parserType).build().encode();
    assertThat(result.getHost()).isEqualToIgnoringCase("[1abc:2abc:3abc::5ABC:6abc]");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriStringInvalidIPv6Host(ParserType parserType) {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.forURIString("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource", parserType));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void forUriStringNoPathWithReservedCharInQuery(ParserType parserType) {
    UriComponents result = UriComponentsBuilder.forURIString("https://example.com?foo=bar@baz", parserType).build();
    assertThat(result.getUserInfo()).isNull();
    assertThat(result.getHost()).isEqualTo("example.com");
    assertThat(result.getQueryParams()).containsKey("foo");
    assertThat(result.getQueryParams().getFirst("foo")).isEqualTo("bar@baz");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void fromHttpUrlQueryParamEncodedAndContainingPlus(ParserType parserType) {
    String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
    URI uri = UriComponentsBuilder.forURIString(httpUrl, parserType).build(true).toURI();

    assertThat(uri.toString()).isEqualTo(httpUrl);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void fromHttpUrlCaseInsensitiveScheme(ParserType parserType) {
    assertThat(UriComponentsBuilder.forURIString("HTTP://www.google.com", parserType).build().getScheme())
            .isEqualTo("http");
    assertThat(UriComponentsBuilder.forURIString("HTTPS://www.google.com", parserType).build().getScheme())
            .isEqualTo("https");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void fromHttpUrlInvalidIPv6Host(ParserType parserType) {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.forURIString("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource", parserType));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void fromHttpUrlWithoutFragment(ParserType parserType) {
    String httpUrl = "http://localhost:8080/test/print";
    UriComponents uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("http");
    assertThat(uriComponents.getUserInfo()).isNull();
    assertThat(uriComponents.getHost()).isEqualTo("localhost");
    assertThat(uriComponents.getPort()).isEqualTo(8080);
    assertThat(uriComponents.getPath()).isEqualTo("/test/print");
    assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
    assertThat(uriComponents.getQuery()).isNull();
    assertThat(uriComponents.getFragment()).isNull();
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);

    httpUrl = "http://user:test@localhost:8080/test/print?foo=bar";
    uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("http");
    assertThat(uriComponents.getUserInfo()).isEqualTo("user:test");
    assertThat(uriComponents.getHost()).isEqualTo("localhost");
    assertThat(uriComponents.getPort()).isEqualTo(8080);
    assertThat(uriComponents.getPath()).isEqualTo("/test/print");
    assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
    assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
    assertThat(uriComponents.getFragment()).isNull();
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);

    httpUrl = "http://localhost:8080/test/print?foo=bar";
    uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("http");
    assertThat(uriComponents.getUserInfo()).isNull();
    assertThat(uriComponents.getHost()).isEqualTo("localhost");
    assertThat(uriComponents.getPort()).isEqualTo(8080);
    assertThat(uriComponents.getPath()).isEqualTo("/test/print");
    assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
    assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
    assertThat(uriComponents.getFragment()).isNull();
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void fromHttpUrlWithFragment(ParserType parserType) {
    String httpUrl = "https://example.com/#baz";
    UriComponents uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("https");
    assertThat(uriComponents.getUserInfo()).isNull();
    assertThat(uriComponents.getHost()).isEqualTo("example.com");
    assertThat(uriComponents.getPort()).isEqualTo(-1);
    assertThat(uriComponents.getPath()).isEqualTo("/");
    assertThat(uriComponents.getPathSegments()).isEmpty();
    assertThat(uriComponents.getQuery()).isNull();
    assertThat(uriComponents.getFragment()).isEqualTo("baz");
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);

    httpUrl = "http://localhost:8080/test/print#baz";
    uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("http");
    assertThat(uriComponents.getUserInfo()).isNull();
    assertThat(uriComponents.getHost()).isEqualTo("localhost");
    assertThat(uriComponents.getPort()).isEqualTo(8080);
    assertThat(uriComponents.getPath()).isEqualTo("/test/print");
    assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
    assertThat(uriComponents.getQuery()).isNull();
    assertThat(uriComponents.getFragment()).isEqualTo("baz");
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);

    httpUrl = "http://localhost:8080/test/print?foo=bar#baz";
    uriComponents = UriComponentsBuilder.forURIString(httpUrl, parserType).build();
    assertThat(uriComponents.getScheme()).isEqualTo("http");
    assertThat(uriComponents.getUserInfo()).isNull();
    assertThat(uriComponents.getHost()).isEqualTo("localhost");
    assertThat(uriComponents.getPort()).isEqualTo(8080);
    assertThat(uriComponents.getPath()).isEqualTo("/test/print");
    assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
    assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
    assertThat(uriComponents.getFragment()).isEqualTo("baz");
    assertThat(uriComponents.toURI().toString()).isEqualTo(httpUrl);
  }

  @Test
    //
  void forHttpRequestWithTrailingSlash() {
    UriComponents before = UriComponentsBuilder.forPath("/foo/").build();
    UriComponents after = UriComponentsBuilder.create().uriComponents(before).build();
    assertThat(after.getPath()).isEqualTo("/foo/");
  }

  @Test
  void path() {
    UriComponentsBuilder builder = UriComponentsBuilder.forPath("/foo/bar");
    UriComponents result = builder.build();

    assertThat(result.getPath()).isEqualTo("/foo/bar");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
  }

  @Test
  void pathSegments() {
    UriComponentsBuilder builder = UriComponentsBuilder.create();
    UriComponents result = builder.pathSegment("foo").pathSegment("bar").build();

    assertThat(result.getPath()).isEqualTo("/foo/bar");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
  }

  @Test
  void pathThenPath() {
    UriComponentsBuilder builder = UriComponentsBuilder.forPath("/foo/bar").path("ba/z");
    UriComponents result = builder.build().encode();

    assertThat(result.getPath()).isEqualTo("/foo/barba/z");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "barba", "z"));
  }

  @Test
  void pathThenPathSegments() {
    UriComponentsBuilder builder = UriComponentsBuilder.forPath("/foo/bar").pathSegment("ba/z");
    UriComponents result = builder.build().encode();

    assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
  }

  @Test
  void pathSegmentsThenPathSegments() {
    UriComponentsBuilder builder = UriComponentsBuilder.create().pathSegment("foo").pathSegment("bar");
    UriComponents result = builder.build();

    assertThat(result.getPath()).isEqualTo("/foo/bar");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
  }

  @Test
  void pathSegmentsThenPath() {
    UriComponentsBuilder builder = UriComponentsBuilder.create().pathSegment("foo").path("/");
    UriComponents result = builder.build();

    assertThat(result.getPath()).isEqualTo("/foo/");
    assertThat(result.getPathSegments()).isEqualTo(Collections.singletonList("foo"));
  }

  @Test
  void pathSegmentsSomeEmpty() {
    UriComponentsBuilder builder = UriComponentsBuilder.create().pathSegment("", "foo", "", "bar");
    UriComponents result = builder.build();

    assertThat(result.getPath()).isEqualTo("/foo/bar");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
  }

  @Test
    //
  void pathWithDuplicateSlashes() {
    UriComponents uriComponents = UriComponentsBuilder.forPath("/foo/////////bar").build();
    assertThat(uriComponents.getPath()).isEqualTo("/foo/bar");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void replacePath(ParserType parserType) {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://www.ietf.org/rfc/rfc2396.txt", parserType);
    builder.replacePath("/rfc/rfc3986.txt");
    UriComponents result = builder.build();

    assertThat(result.toUriString()).isEqualTo("https://www.ietf.org/rfc/rfc3986.txt");

    builder = UriComponentsBuilder.forURIString("https://www.ietf.org/rfc/rfc2396.txt", parserType);
    builder.replacePath(null);
    result = builder.build();

    assertThat(result.toUriString()).isEqualTo("https://www.ietf.org");
  }

  @ParameterizedTest
  @EnumSource
  void query(ParserType parserType) {
    String url = "https://example.com/foo?foo=bar";
    UriComponents uric = UriComponentsBuilder.forURIString(url, parserType).query("baz=qux").build();
    assertThat(uric.getQueryParams()).isEqualTo(Map.of("foo", List.of("bar"), "baz", List.of("qux")));
  }

  @ParameterizedTest
  @EnumSource
  void queryWithNull(ParserType parserType) {
    String url = "https://example.com/foo?foo=bar";
    UriComponents uric = UriComponentsBuilder.forURIString(url, parserType).query(null).build();
    assertThat(uric.getQueryParams()).isEqualTo(Map.of("foo", List.of("bar")));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void replaceQuery(ParserType parserType) {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.com/foo?foo=bar&baz=qux", parserType);
    builder.replaceQuery("baz=42");
    UriComponents result = builder.build();

    assertThat(result.toUriString()).isEqualTo("https://example.com/foo?baz=42");

    builder = UriComponentsBuilder.forURIString("https://example.com/foo?foo=bar&baz=qux", parserType);
    builder.replaceQuery(null);
    result = builder.build();

    assertThat(result.toUriString()).isEqualTo("https://example.com/foo");
  }

  @Test
  void queryParam() {
    UriComponents result = UriComponentsBuilder.create().queryParam("baz", "qux", 42).build();

    assertThat(result.getQuery()).isEqualTo("baz=qux&baz=42");
    MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
    expectedQueryParams.add("baz", "qux");
    expectedQueryParams.add("baz", "42");
    assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
  }

  @Test
  void queryParamWithList() {
    List<String> values = Arrays.asList("qux", "42");
    UriComponents result = UriComponentsBuilder.create().queryParam("baz", values).build();

    assertThat(result.getQuery()).isEqualTo("baz=qux&baz=42");
    assertThat(result.getQueryParams()).containsOnlyKeys("baz").containsEntry("baz", values);
  }

  @Test
  void queryParamWithOptionalValue() {
    UriComponents result = UriComponentsBuilder.create()
            .queryParam("foo", Optional.empty())
            .queryParam("baz", Optional.of("qux"), 42)
            .build();

    assertThat(result.getQuery()).isEqualTo("foo&baz=qux&baz=42");
    assertThat(result.getQueryParams()).containsOnlyKeys("foo", "baz")
            .containsEntry("foo", Collections.singletonList(null))
            .containsEntry("baz", Arrays.asList("qux", "42"));
  }

  @Test
  void queryParamIfPresent() {
    UriComponents result = UriComponentsBuilder.create()
            .queryParamIfPresent("baz", Optional.of("qux"))
            .queryParamIfPresent("foo", Optional.empty())
            .build();

    assertThat(result.getQuery()).isEqualTo("baz=qux");
    assertThat(result.getQueryParams())
            .containsOnlyKeys("baz")
            .containsEntry("baz", Collections.singletonList("qux"));
  }

  @Test
  void queryParamIfPresentCollection() {
    List<String> values = Arrays.asList("foo", "bar");
    UriComponents result = UriComponentsBuilder.create()
            .queryParamIfPresent("baz", Optional.of(values))
            .build();

    assertThat(result.getQuery()).isEqualTo("baz=foo&baz=bar");
    assertThat(result.getQueryParams()).containsOnlyKeys("baz").containsEntry("baz", values);
  }

  @Test
  void emptyQueryParam() {
    UriComponentsBuilder builder = UriComponentsBuilder.create();
    UriComponents result = builder.queryParam("baz").build();

    assertThat(result.getQuery()).isEqualTo("baz");
    MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
    expectedQueryParams.add("baz", null);
    assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
  }

  @Test
  void emptyQueryParams() {
    UriComponents result = UriComponentsBuilder.create()
            .queryParam("baz", Collections.emptyList())
            .queryParam("foo", (Collection<?>) null)
            .build();

    assertThat(result.getQuery()).isEqualTo("baz&foo");
    MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
    expectedQueryParams.add("baz", null);
    expectedQueryParams.add("foo", null);
    assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
  }

  @Test
  void replaceQueryParam() {
    UriComponentsBuilder builder = UriComponentsBuilder.create().queryParam("baz", "qux", 42);
    builder.replaceQueryParam("baz", "xuq", 24);
    UriComponents result = builder.build();

    assertThat(result.getQuery()).isEqualTo("baz=xuq&baz=24");

    builder = UriComponentsBuilder.create().queryParam("baz", "qux", 42);
    builder.replaceQueryParam("baz");
    result = builder.build();

    assertThat(result.getQuery()).as("Query param should have been deleted").isNull();
  }

  @Test
  void replaceQueryParams() {
    UriComponentsBuilder builder = UriComponentsBuilder.create().queryParam("baz", Arrays.asList("qux", 42));
    builder.replaceQueryParam("baz", Arrays.asList("xuq", 24));
    UriComponents result = builder.build();

    assertThat(result.getQuery()).isEqualTo("baz=xuq&baz=24");

    builder = UriComponentsBuilder.create().queryParam("baz", Arrays.asList("qux", 42));
    builder.replaceQueryParam("baz", Collections.emptyList());
    result = builder.build();

    assertThat(result.getQuery()).as("Query param should have been deleted").isNull();
  }

  @Test
  void buildAndExpandHierarchical() {
    UriComponents result = UriComponentsBuilder.forPath("/{foo}").buildAndExpand("fooValue");
    assertThat(result.toUriString()).isEqualTo("/fooValue");

    Map<String, String> values = new HashMap<>();
    values.put("foo", "fooValue");
    values.put("bar", "barValue");
    result = UriComponentsBuilder.forPath("/{foo}/{bar}").buildAndExpand(values);
    assertThat(result.toUriString()).isEqualTo("/fooValue/barValue");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void parseBuildAndExpandHierarchical(ParserType parserType) {
    URI uri = UriComponentsBuilder
            .forURIString("{scheme}://{host}:{port}/{segment}?{query}#{fragment}", parserType)
            .buildAndExpand(Map.of(
                    "scheme", "ws", "host", "example.org", "port", "7777", "segment", "path",
                    "query", "q=1", "fragment", "foo"))
            .toURI();
    assertThat(uri.toString()).isEqualTo("ws://example.org:7777/path?q=1#foo");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void buildAndExpandOpaque(ParserType parserType) {
    UriComponents result = UriComponentsBuilder.forURIString("mailto:{user}@{domain}", parserType)
            .buildAndExpand("foo", "example.com");
    assertThat(result.toUriString()).isEqualTo("mailto:foo@example.com");

    Map<String, String> values = new HashMap<>();
    values.put("user", "foo");
    values.put("domain", "example.com");
    UriComponentsBuilder.forURIString("mailto:{user}@{domain}", parserType).buildAndExpand(values);
    assertThat(result.toUriString()).isEqualTo("mailto:foo@example.com");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void schemeVariableMixedCase(ParserType parserType) {

    BiConsumer<String, String> tester = (scheme, value) -> {
      URI uri = UriComponentsBuilder.forURIString(scheme + "://example.org", parserType)
              .buildAndExpand(Map.of("TheScheme", value))
              .toURI();
      assertThat(uri.toString()).isEqualTo("wss://example.org");
    };

    tester.accept("{TheScheme}", "wss");
    tester.accept("{TheScheme}s", "ws");
    tester.accept("ws{TheScheme}", "s");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void queryParamWithValueWithEquals(ParserType parserType) {
    UriComponents uriComponents = UriComponentsBuilder.forURIString("https://example.com/foo?bar=baz", parserType).build();
    assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar=baz");
    assertThat(uriComponents.getQueryParams().get("bar")).containsExactly("baz");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void queryParamWithoutValueWithEquals(ParserType parserType) {
    UriComponents uriComponents = UriComponentsBuilder.forURIString("https://example.com/foo?bar=", parserType).build();
    assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar=");
    assertThat(uriComponents.getQueryParams().get("bar")).element(0).asString().isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void queryParamWithoutValueWithoutEquals(ParserType parserType) {
    UriComponents uriComponents = UriComponentsBuilder.forURIString("https://example.com/foo?bar", parserType).build();
    assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar");

    assertThat(uriComponents.getQueryParams().get("bar")).element(0).isNull();
  }

  @Test
  void opaqueUriDoesNotResetOnNullInput() {
    URI uri = URI.create("urn:ietf:wg:oauth:2.0:oob");
    UriComponents result = UriComponentsBuilder.forURI(uri)
            .host(null)
            .port(-1)
            .port(null)
            .queryParams(null)
            .replaceQuery(null)
            .query(null)
            .build();

    assertThat(result.toURI()).isEqualTo(uri);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void relativeUrls(ParserType parserType) {
    String baseUrl = "https://example.com";
    assertThat(UriComponentsBuilder.forURIString(baseUrl + "/foo/../bar", parserType).build().toString())
            .isEqualTo(baseUrl + (parserType == ParserType.WHAT_WG ? "/bar" : "/foo/../bar"));
    assertThat(UriComponentsBuilder.forURIString(baseUrl + "/foo/../bar", parserType).build().toUriString())
            .isEqualTo(baseUrl + (parserType == ParserType.WHAT_WG ? "/bar" : "/foo/../bar"));
    assertThat(UriComponentsBuilder.forURIString(baseUrl + "/foo/../bar", parserType).build().toURI().getPath())
            .isEqualTo((parserType == ParserType.WHAT_WG ? "/bar" : "/foo/../bar"));
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).path("foo/../bar").build().toString())
            .isEqualTo(baseUrl + "/foo/../bar");
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).path("foo/../bar").build().toUriString())
            .isEqualTo(baseUrl + "/foo/../bar");
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).path("foo/../bar").build().toURI().getPath())
            .isEqualTo("/foo/../bar");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void emptySegments(ParserType parserType) {
    String baseUrl = "https://example.com/abc/";
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).path("/x/y/z").build().toString())
            .isEqualTo("https://example.com/abc/x/y/z");
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).pathSegment("x", "y", "z").build().toString())
            .isEqualTo("https://example.com/abc/x/y/z");
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).path("/x/").path("/y/z").build().toString())
            .isEqualTo("https://example.com/abc/x/y/z");
    assertThat(UriComponentsBuilder.forURIString(baseUrl, parserType).pathSegment("x").path("y").build().toString())
            .isEqualTo("https://example.com/abc/x/y");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void parsesEmptyFragment(ParserType parserType) {
    UriComponents components = UriComponentsBuilder.forURIString("/example#", parserType).build();
    assertThat(components.getFragment()).isNull();
    assertThat(components.toString()).isEqualTo("/example");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void parsesEmptyUri(ParserType parserType) {
    UriComponents components = UriComponentsBuilder.forURIString("", parserType).build();
    assertThat(components.toString()).isEmpty();
  }

  @Test
  void testCloneAndMerge() {
    UriComponentsBuilder builder1 = UriComponentsBuilder.create();
    builder1.scheme("http").host("e1.com").path("/p1").pathSegment("ps1").queryParam("q1", "x").fragment("f1").encode();

    UriComponentsBuilder builder2 = builder1.cloneBuilder();
    builder2.scheme("https").host("e2.com").path("p2").pathSegment("{ps2}").queryParam("q2").fragment("f2");

    builder1.queryParam("q1", "y");  // one more entry for an existing parameter

    UriComponents result1 = builder1.build();
    assertThat(result1.getScheme()).isEqualTo("http");
    assertThat(result1.getHost()).isEqualTo("e1.com");
    assertThat(result1.getPath()).isEqualTo("/p1/ps1");
    assertThat(result1.getQuery()).isEqualTo("q1=x&q1=y");
    assertThat(result1.getFragment()).isEqualTo("f1");

    UriComponents result2 = builder2.buildAndExpand("ps2;a");
    assertThat(result2.getScheme()).isEqualTo("https");
    assertThat(result2.getHost()).isEqualTo("e2.com");
    assertThat(result2.getPath()).isEqualTo("/p1/ps1/p2/ps2%3Ba");
    assertThat(result2.getQuery()).isEqualTo("q1=x&q2");
    assertThat(result2.getFragment()).isEqualTo("f2");
  }

  @Test
  void testDeepClone() {
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("ps1", "foo");
    vars.put("ps2", "bar");

    UriComponentsBuilder builder1 = UriComponentsBuilder.create();
    builder1.scheme("http").host("e1.com").userInfo("user:pwd").path("/p1").pathSegment("{ps1}")
            .pathSegment("{ps2}").queryParam("q1").fragment("f1").uriVariables(vars).encode();

    UriComponentsBuilder builder2 = builder1.cloneBuilder();

    UriComponents result1 = builder1.build();
    assertThat(result1.getScheme()).isEqualTo("http");
    assertThat(result1.getUserInfo()).isEqualTo("user:pwd");
    assertThat(result1.getHost()).isEqualTo("e1.com");
    assertThat(result1.getPath()).isEqualTo("/p1/%s/%s", vars.get("ps1"), vars.get("ps2"));
    assertThat(result1.getQuery()).isEqualTo("q1");
    assertThat(result1.getFragment()).isEqualTo("f1");
    assertThat(result1.getSchemeSpecificPart()).isNull();

    UriComponents result2 = builder2.build();
    assertThat(result2.getScheme()).isEqualTo("http");
    assertThat(result2.getUserInfo()).isEqualTo("user:pwd");
    assertThat(result2.getHost()).isEqualTo("e1.com");
    assertThat(result2.getPath()).isEqualTo("/p1/%s/%s", vars.get("ps1"), vars.get("ps2"));
    assertThat(result2.getQuery()).isEqualTo("q1");
    assertThat(result2.getFragment()).isEqualTo("f1");
    assertThat(result1.getSchemeSpecificPart()).isNull();
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void encodeTemplateWithInvalidPlaceholderSyntax(ParserType parserType) {

    BiConsumer<String, String> tester = (in, out) ->
            assertThat(UriComponentsBuilder.forURIString(in, parserType).encode().toUriString()).isEqualTo(out);

    // empty
    tester.accept("{}", "%7B%7D");
    tester.accept("{ \t}", (parserType == ParserType.WHAT_WG ? "%7B%20%7D" : "%7B%20%09%7D"));
    tester.accept("/a{}b", "/a%7B%7Db");
    tester.accept("/a{ \t}b", (parserType == ParserType.WHAT_WG ? "/a%7B%20%7Db" : "/a%7B%20%09%7Db"));

    // nested, matching
    tester.accept("{foo{}}", "%7Bfoo%7B%7D%7D");
    tester.accept("{foo{bar}baz}", "%7Bfoo%7Bbar%7Dbaz%7D");
    tester.accept("/a{foo{}}b", "/a%7Bfoo%7B%7D%7Db");
    tester.accept("/a{foo{bar}baz}b", "/a%7Bfoo%7Bbar%7Dbaz%7Db");

    // mismatched
    tester.accept("{foo{{}", "%7Bfoo%7B%7B%7D");
    tester.accept("{foo}}", "{foo}%7D");
    tester.accept("/a{foo{{}bar", "/a%7Bfoo%7B%7B%7Dbar");
    tester.accept("/a{foo}}b", "/a{foo}%7Db");

    // variable with regex
    tester.accept("{year:\\d{1,4}}", "{year:\\d{1,4}}");
    tester.accept("/a{year:\\d{1,4}}b", "/a{year:\\d{1,4}}b");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void uriComponentsNotEqualAfterNormalization(ParserType parserType) {
    UriComponents uri1 = UriComponentsBuilder.forURIString("http://test.com", parserType).build().normalize();
    UriComponents uri2 = UriComponentsBuilder.forURIString("http://test.com/", parserType).build();

    assertThat(uri1.getPathSegments()).isEmpty();
    assertThat(uri2.getPathSegments()).isEmpty();
    assertThat(uri2).isNotEqualTo(uri1);
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void uriComponentsWithMergedQueryParams(ParserType parserType) {
    String uri = UriComponentsBuilder.forURIString("http://localhost:8081", parserType)
            .uriComponents(UriComponentsBuilder.forURIString("/{path}?sort={sort}", parserType).build())
            .queryParam("sort", "another_value").build().toString();

    assertThat(uri).isEqualTo("http://localhost:8081/{path}?sort={sort}&sort=another_value");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void toURIStringWithCurlyBraces(ParserType parserType) {
    assertThat(UriComponentsBuilder.forURIString("/path?q={asa}asa", parserType).toUriString())
            .isEqualTo("/path?q=%7Basa%7Dasa");
  }

  @Test
  void verifyDoubleSlashReplacedWithSingleOne() {
    String path = UriComponentsBuilder.forPath("/home/").path("/path").build().getPath();
    assertThat(path).isEqualTo("/home/path");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void validPort(ParserType parserType) {
    UriComponents uriComponents = UriComponentsBuilder.forURIString("http://localhost:52567/path", parserType).build();
    assertThat(uriComponents.getPort()).isEqualTo(52567);
    assertThat(uriComponents.getPath()).isEqualTo("/path");

    uriComponents = UriComponentsBuilder.forURIString("http://localhost:52567?trace=false", parserType).build();
    assertThat(uriComponents.getPort()).isEqualTo(52567);
    assertThat(uriComponents.getQuery()).isEqualTo("trace=false");

    uriComponents = UriComponentsBuilder.forURIString("http://localhost:52567#fragment", parserType).build();
    assertThat(uriComponents.getPort()).isEqualTo(52567);
    assertThat(uriComponents.getFragment()).isEqualTo("fragment");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void verifyInvalidPort(ParserType parserType) {
    String url = "http://localhost:XXX/path";
    assertThatIllegalArgumentException()
            .isThrownBy(() -> UriComponentsBuilder.forURIString(url, parserType).build().toURI());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> UriComponentsBuilder.forURIString(url, parserType).build().toURI());
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void expandPortAndPathWithoutSeparator(ParserType parserType) {
    URI uri = UriComponentsBuilder
            .forURIString("ws://localhost:{port}/{path}", parserType)
            .buildAndExpand(7777, "test")
            .toURI();
    assertThat(uri.toString()).isEqualTo("ws://localhost:7777/test");
  }

  @ParameterizedTest
  @EnumSource
  void fromOpaqueUriWithUrnScheme(ParserType parserType) {
    URI uri = UriComponentsBuilder
            .forURIString("urn:text:service-{region}:{prefix}/{id}", parserType).build()
            .expand("US", "prefix1", "Id-2")
            .toURI();

    assertThat(uri.getScheme()).isEqualTo("urn");
    assertThat(uri.isOpaque()).isTrue();
    assertThat(uri.getSchemeSpecificPart()).isEqualTo("text:service-US:prefix1/Id-2");
  }

  @ParameterizedTest
  @EnumSource
  void parseBuildAndExpandQueryParamWithSameName(ParserType parserType) {
    UriComponents result = UriComponentsBuilder
            .forURIString("/?{pk1}={pv1}&{pk2}={pv2}", parserType)
            .buildAndExpand("k1", "v1", "k1", "v2");

    assertThat(result.getQuery()).isEqualTo("k1=v1&k1=v2");
    assertThat(result.getQueryParams()).containsExactly(Map.entry("k1", List.of("v1", "v2")));
  }

  @ParameterizedTest
  @EnumSource
  void singleCharFragment(ParserType parserType) {
    URI uri = UriComponentsBuilder
            .forURIString("https://localhost/resource#a", parserType)
            .build().toURI();
    assertThat(uri.toString()).isEqualTo("https://localhost/resource#a");
  }

}
