/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.web.util;

import org.junit.jupiter.api.Test;

import infra.lang.Nullable;

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
}