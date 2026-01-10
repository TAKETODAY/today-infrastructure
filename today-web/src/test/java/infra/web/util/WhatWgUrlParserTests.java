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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/18 10:46
 */
class WhatWgUrlParserTests {

  private static final WhatWgUrlParser.UrlRecord EMPTY_URL_RECORD = new WhatWgUrlParser.UrlRecord();

  @Test
  void parse() {
    testParse("https://example.com", "https", "example.com", null, "", null, null);
    testParse("https://example.com/", "https", "example.com", null, "/", null, null);
    testParse("https://example.com/foo", "https", "example.com", null, "/foo", null, null);
    testParse("https://example.com/foo/", "https", "example.com", null, "/foo/", null, null);
    testParse("https://example.com:81/foo", "https", "example.com", "81", "/foo", null, null);
    testParse("/foo", "", null, null, "/foo", null, null);
    testParse("/foo/", "", null, null, "/foo/", null, null);
    testParse("/foo/../bar", "", null, null, "/bar", null, null);
    testParse("/foo/../bar/", "", null, null, "/bar/", null, null);
    testParse("//other.info/foo/bar", "", "other.info", null, "/foo/bar", null, null);
    testParse("//other.info/parent/../foo/bar", "", "other.info", null, "/foo/bar", null, null);
  }

  private void testParse(String input, String scheme, @Nullable String host, @Nullable String port, String path, @Nullable String query, @Nullable String fragment) {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse(input, EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).as("Invalid scheme").isEqualTo(scheme);
    if (host != null) {
      assertThat(result.host()).as("Host is null").isNotNull();
      assertThat(result.host().toString()).as("Invalid host").isEqualTo(host);
    }
    else {
      assertThat(result.host()).as("Host is not null").isNull();
    }
    if (port != null) {
      assertThat(result.port()).as("Port is null").isNotNull();
      assertThat(result.port().toString()).as("Invalid port").isEqualTo(port);
    }
    else {
      assertThat(result.port()).as("Port is not null").isNull();
    }
    assertThat(result.hasOpaquePath()).as("Result has opaque path").isFalse();
    assertThat(result.path().toString()).as("Invalid path").isEqualTo(path);
    assertThat(result.query()).as("Invalid query").isEqualTo(query);
    assertThat(result.fragment()).as("Invalid fragment").isEqualTo(fragment);
  }

  @Test
  void parseOpaque() {
    testParseOpaque("mailto:user@example.com?subject=foo", "user@example.com", "subject=foo");

  }

  void testParseOpaque(String input, String path, @Nullable String query) {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("mailto:user@example.com?subject=foo", EMPTY_URL_RECORD, null, null);

    assertThat(result.scheme()).as("Invalid scheme").isEqualTo("mailto");
    assertThat(result.hasOpaquePath()).as("Result has no opaque path").isTrue();
    assertThat(result.path().toString()).as("Invalid path").isEqualTo(path);
    if (query != null) {
      assertThat(result.query()).as("Query is null").isNotNull();
      assertThat(result.query()).as("Invalid query").isEqualTo(query);
    }
    else {
      assertThat(result.query()).as("Query is not null").isNull();
    }
  }

  @Test
  void shouldParseSpecialSchemes() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("http://example.com", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("http");

    result = WhatWgUrlParser.parse("https://example.com", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("https");

    result = WhatWgUrlParser.parse("ftp://example.com", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("ftp");

    result = WhatWgUrlParser.parse("file:///path", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("file");
  }

  @Test
  void shouldParseOpaquePaths() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("mailto:user@example.com", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("mailto");
    assertThat(result.hasOpaquePath()).isTrue();
    assertThat(result.path().toString()).isEqualTo("user@example.com");

    result = WhatWgUrlParser.parse("javascript:alert('hello')", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("javascript");
    assertThat(result.hasOpaquePath()).isTrue();
    assertThat(result.path().toString()).isEqualTo("alert('hello')");
  }

  @Test
  void shouldParsePathsWithNormalization() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("/foo/bar/../baz", EMPTY_URL_RECORD, null, null);
    assertThat(result.path().toString()).isEqualTo("/foo/baz");

    result = WhatWgUrlParser.parse("/foo/./bar", EMPTY_URL_RECORD, null, null);
    assertThat(result.path().toString()).isEqualTo("/foo/bar");
  }

  @Test
  void shouldParseUrlsWithPort() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("https://example.com:8080/path", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("https");
    assertThat(result.host().toString()).isEqualTo("example.com");
    assertThat(result.port().toString()).isEqualTo("8080");
    assertThat(result.path().toString()).isEqualTo("/path");
  }

  @Test
  void shouldParseUrlsWithQueryAndFragment() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("https://example.com/path?query=value#fragment", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("https");
    assertThat(result.host().toString()).isEqualTo("example.com");
    assertThat(result.path().toString()).isEqualTo("/path");
    assertThat(result.query()).isEqualTo("query=value");
    assertThat(result.fragment()).isEqualTo("fragment");
  }

  @Test
  void shouldParseRelativeUrls() {
    WhatWgUrlParser.UrlRecord base = WhatWgUrlParser.parse("https://example.com/base/", EMPTY_URL_RECORD, null, null);
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("../other", EMPTY_URL_RECORD, StandardCharsets.UTF_8, null);
    assertThat(result.path().toString()).isEqualTo("/other");

    result = WhatWgUrlParser.parse("./other", base, StandardCharsets.UTF_8, null);
    assertThat(result.path().toString()).isEqualTo("/base/other");
  }

  @Test
  void shouldParseFileUrls() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("file:///C:/Windows/System32", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("file");
    assertThat(result.host().toString()).isEqualTo("");
    assertThat(result.path().toString()).isEqualTo("/C:/Windows/System32");
  }

  @Test
  void shouldParseIpv6Hosts() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("http://[::1]/path", EMPTY_URL_RECORD, null, null);
    assertThat(result.scheme()).isEqualTo("http");
    assertThat(result.host()).isNotNull();
    assertThat(result.path().toString()).isEqualTo("/path");
  }

  @Test
  void shouldHandleUrlEncoding() {
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("https://example.com/hello%20world", EMPTY_URL_RECORD, null, null);
    assertThat(result.path().toString()).isEqualTo("/hello%20world");

    result = WhatWgUrlParser.parse("https://example.com/search?q=hello world", EMPTY_URL_RECORD, null, null);
    assertThat(result.query()).isEqualTo("q=hello world");
  }

}