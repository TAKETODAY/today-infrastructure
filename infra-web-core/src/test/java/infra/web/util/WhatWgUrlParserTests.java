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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

  @Test
  void parseAsciiHost() {
    // Pure ASCII host is lowercased
    testParse("https://EXAMPLE.com/foo", "https", "example.com", null, "/foo", null, null);
    // ASCII "xn--" (ACE) labels are accepted and lowercased (in any case), not validated or rejected.
    // See https://url.spec.whatwg.org/#concept-domain-to-ascii and web-platform-tests url cases.
    testParse("https://a.b.c.xn--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
    testParse("https://a.b.c.XN--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
    testParse("https://a.b.c.Xn--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
    // A trailing non-numeric "xn--" label keeps a numeric-looking host as a domain, not an IPv4 address.
    testParse("https://10.0.0.xn--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
    testParse("https://10.0.0.XN--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
    testParse("https://10.0.0.xN--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
    // Leading ACE label is handled too, and an empty "xn--" label is accepted (not a failure)
    testParse("https://XN--pokxncvks.example", "https", "xn--pokxncvks.example", null, "", null, null);
    testParse("https://xn--/", "https", "xn--", null, "/", null, null);
    testParse("file://xn--/p", "file", "xn--", null, "/p", null, null);
  }

  @Test
  void parseOpaqueHostTruncatedPercentEscape() {
    // A truncated or non-hex percent-escape in an opaque host is a validation error,
    // not a failure, and must not read past the end of the input
    // (see https://url.spec.whatwg.org/#concept-opaque-host-parser).
    List<String> errors = new ArrayList<>();
    WhatWgUrlParser.UrlRecord record = WhatWgUrlParser.parse("foo://%4", EMPTY_URL_RECORD, null, errors::add);
    assertThat(record.host().toString()).isEqualTo("%4");

    record = WhatWgUrlParser.parse("foo://%4x", EMPTY_URL_RECORD, null, errors::add);
    assertThat(record.host().toString()).isEqualTo("%4x");

    assertThat(errors).isNotEmpty();
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
    WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse(input, EMPTY_URL_RECORD, null, null);

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

  @ParameterizedTest
  @CsvSource({
          "192.0.2.1, 192.0.2.1",
          "192.0.2.1., 192.0.2.1",
          "0xc0.0000.2.1, 192.0.2.1",
          "192.0.513, 192.0.2.1",
          "0x, 0.0.0.0",
          "[::ffff:192.0.2.1], [::ffff:c000:201]",
          "[2001:0db8:0000:0000:0001:0000:0000:0001], [2001:db8::1:0:0:1]",
          "[2001:db8:1:2:3:4:5:6], [2001:db8:1:2:3:4:5:6]",
          "bücher.test, xn--bcher-kva.test",
          "b%C3%BCcher.test, xn--bcher-kva.test"
  })
  void normalizesIpAddressesAndInternationalDomains(String host, String expected) {
    var result = WhatWgUrlParser.parse("https://" + host + "/path", null, null, error -> { });

    assertThat(result.hostname()).isEqualTo(expected);
    assertThat(result.path()).hasToString("/path");
  }

  @ParameterizedTest
  @CsvSource({
          "192.0.2.1.5, exactly 4 parts",
          "192..2.1, not numeric",
          "192.bad.2.1, not numeric",
          "192.0.2.09, not numeric",
          "192.0.2.2147483648, not numeric",
          "256.0.2.1, exceeds 255",
          "192.0.2.256, exceeds 256",
          "[:1], improper compression",
          "[1:2:3:4:5:6:7:8:9], more than 8 pieces",
          "[2001::db8::1], compressed in more than one spot",
          "[2001:db8:1], fewer than 8 pieces",
          "[2001:db8:], unexpectedly ends",
          "[2001:db8::z], neither an ASCII hex digit",
          "[2001:db8::1, missing the closing",
          "[::ffff:.0.2.1], IPv4 part is empty",
          "[1:2:3:4:5:6:7:192.0.2.1], more than 6 pieces",
          "[::ffff:192.0.2.1.4], contains a non-ASCII digit",
          "[::ffff:192.a.2.1], contains a non-ASCII digit",
          "[::ffff:192.00.2.1], contains a non-ASCII digit",
          "[::ffff:192.0.256.1], exceeds 255",
          "[::ffff:192.0.2], too few parts",
          "bad%00.test, Invalid character",
          "bad%xx.test, Could not decode"
  })
  void rejectsMalformedHosts(String host, String reason) {
    assertThatExceptionOfType(InvalidUrlException.class)
            .isThrownBy(() -> WhatWgUrlParser.parse("http://" + host + "/", null, null, error -> { }))
            .withMessageContaining(reason);
  }

  @Test
  void reportsRecoverableIpv4Notation() {
    List<String> errors = new ArrayList<>();

    var url = WhatWgUrlParser.parse("http://0xc0.0.513./", null, null, errors::add);

    assertThat(url.hostname()).isEqualTo("192.0.2.1");
    assertThat(errors).hasSize(3).anyMatch(error -> error.contains("ends with"))
            .anyMatch(error -> error.contains("hexadecimal or octal"))
            .anyMatch(error -> error.contains("exceeds 255"));
  }

  @Test
  void serializesEncodedCredentialsAndUrlComponents() {
    var url = WhatWgUrlParser.parse("https://a b:p@ss@bücher.test:8443/a b?q='café'#my note",
            null, StandardCharsets.UTF_8, error -> { });

    assertThat(url.protocol()).isEqualTo("https:");
    assertThat(url.userInfo()).isEqualTo("a%20b:p%40ss");
    assertThat(url.hostString()).isEqualTo("xn--bcher-kva.test:8443");
    assertThat(url.pathname()).isEqualTo("/a%20b");
    assertThat(url.search()).isEqualTo("?q=%27caf%C3%A9%27");
    assertThat(url.hash()).isEqualTo("#my%20note");
    assertThat(url.href()).isEqualTo("https://a%20b:p%40ss@xn--bcher-kva.test:8443/a%20b?q=%27caf%C3%A9%27#my%20note");
  }

  @ParameterizedTest
  @CsvSource({
          "https://other.test/one, https://other.test/one",
          "https:next, https://user:pass@example.test:8443/base/next",
          "'/root', https://user:pass@example.test:8443/root",
          "'?new=1', https://user:pass@example.test:8443/base/item?new=1",
          "'#new', https://user:pass@example.test:8443/base/item?old=1#new",
          "'', https://user:pass@example.test:8443/base/item?old=1"
  })
  void resolvesRelativeReferencesAgainstHierarchicalBase(String reference, String expected) {
    var base = WhatWgUrlParser.parse("https://user:pass@example.test:8443/base/item?old=1#old", null, null, null);
    var resolved = WhatWgUrlParser.parse(reference, base, null, error -> { });

    assertThat(resolved.href()).isEqualTo(expected);
    assertThat(base.href()).isEqualTo("https://user:pass@example.test:8443/base/item?old=1#old");
  }

  @Test
  void allowsOnlyAFragmentReferenceAgainstAnOpaqueBase() {
    var base = WhatWgUrlParser.parse("mailto:reader@example.test?subject=hello", null, null, null);

    assertThat(WhatWgUrlParser.parse("#notes", base, null, null).href())
            .isEqualTo("mailto:reader@example.test?subject=hello#notes");
    assertThatExceptionOfType(InvalidUrlException.class)
            .isThrownBy(() -> WhatWgUrlParser.parse("next", base, null, null));
  }

  @ParameterizedTest
  @CsvSource({
          "file://localhost/C:/item, file:///C:/item",
          "file://files.test/share/item, file://files.test/share/item",
          "file://C:/item, file:///C:/item"
  })
  void handlesLocalDriveLettersAndFileAuthorities(String input, String expected) {
    assertThat(WhatWgUrlParser.parse(input, null, null, error -> { }).href()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
          "'', file:///C:/base/item?old=1",
          "'?new=1', file:///C:/base/item?new=1",
          "'#new', file:///C:/base/item?old=1#new",
          "x, file:///C:/base/x",
          "../next, file:///C:/next",
          "//files.test/share, file://files.test/share",
          "D|/next, file:///D:/next"
  })
  void resolvesFileReferencesAgainstBaseDrive(String reference, String expected) {
    var base = WhatWgUrlParser.parse("file:///C:/base/item?old=1#old", null, null, null);
    assertThat(WhatWgUrlParser.parse(reference, base, null, error -> { }).href()).isEqualTo(expected);
    assertThat(base.href()).isEqualTo("file:///C:/base/item?old=1#old");
  }

  @ParameterizedTest
  @CsvSource({
          "http://example.test:80, http://example.test/",
          "https://example.test:00443, https://example.test/",
          "ftp://example.test:21, ftp://example.test/",
          "ws://example.test:80, ws://example.test/",
          "wss://example.test:443, wss://example.test/",
          "custom://example.test:80, custom://example.test:80",
          "http://example.test:65535, http://example.test:65535/"
  })
  void dropsDefaultPortsButRetainsNonDefaultPorts(String input, String expected) {
    assertThat(WhatWgUrlParser.parse(input, null, null, null).href()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
          "'https://', does not contain a host",
          "'https://reader@', Missing host",
          "https://example.test:65536, out of range",
          "https://example.test:999999999999, 999999999999",
          "https://example.test:port, Invalid port",
          "custom://bad host/path, forbidden host code point"
  })
  void rejectsInvalidAuthorityAndPorts(String input, String reason) {
    assertThatExceptionOfType(InvalidUrlException.class).isThrownBy(() -> WhatWgUrlParser.parse(input, null, null, null))
            .withMessageContaining(reason);
  }

  @Test
  void stripsLeadingAndTrailingWhitespaceAndRemovesEmbeddedTabsAndNewlines() {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse(" \u0001https://exam\tple.test/a\nb \r", null, null, errors::add);

    assertThat(url.href()).isEqualTo("https://example.test/ab");
    assertThat(errors).hasSize(6).allMatch(error -> error.contains("is not a URL unit"));
  }

  @ParameterizedTest
  @CsvSource({
          "https:example.test/path, https://example.test/path",
          "https:////example.test/path, https://example.test/path",
          "https://example.test\\dir\\file, https://example.test/dir/file",
          "file:/path, file:///path"
  })
  void reportsRecoverableSlashValidationErrors(String input, String expected) {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse(input, null, null, errors::add);

    assertThat(url.href()).isEqualTo(expected);
    assertThat(errors).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.test/path%4", "https://example.test/?a=%4", "https://example.test/#ref%4",
          "custom:opaque%4", "custom:opaque%4x", "https://example.test/?a=%x4", "https://example.test/#ref%4x" })
  void reportsMalformedEscapesInEachUrlComponent(String input) {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse(input, null, StandardCharsets.UTF_8, errors::add);

    assertThat(url.href()).isEqualTo(input);
    assertThat(errors).singleElement().asString().contains("Invalid URL Unit");
  }

  @Test
  void usesLegacyQueryEncodingForHttpButUtf8ForWebSocketAndCustomSchemes() {
    assertThat(WhatWgUrlParser.parse("https://example.test/?q=café", null, StandardCharsets.ISO_8859_1, null).search())
            .isEqualTo("?q=caf%E9");
    assertThat(WhatWgUrlParser.parse("wss://example.test/?q=café", null, StandardCharsets.ISO_8859_1, null).search())
            .isEqualTo("?q=caf%C3%A9");
    assertThat(WhatWgUrlParser.parse("custom:/path?q=café", null, StandardCharsets.ISO_8859_1, null).search())
            .isEqualTo("?q=caf%C3%A9");
  }

  @ParameterizedTest
  @CsvSource({
          "https://EXAMPLE.test:443/a/../b, https://example.test/b",
          "https://[2001:0db8:0:0:0:0:0:1]/, https://[2001:db8::1]/",
          "file://localhost/path, file:///path",
          "custom://é.test/x, custom://%C3%A9.test/x",
          "mailto:reader@example.test, mailto:reader@example.test"
  })
  void normalizedUrlRecordsCompareEqualAndHaveTheSameHash(String input, String normalized) {
    var first = WhatWgUrlParser.parse(input, null, StandardCharsets.UTF_8, null);
    var second = WhatWgUrlParser.parse(normalized, null, StandardCharsets.UTF_8, null);

    assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(normalized);
    assertThat(first.href()).isEqualTo(normalized);
    assertThat(first).isNotEqualTo(WhatWgUrlParser.parse(normalized + "#other", null, StandardCharsets.UTF_8, null));
  }

  @ParameterizedTest
  @CsvSource({
          "/guide/%2e/intro, /guide/intro",
          "/guide/%2E, /guide/",
          "/guide/draft/.%2e/intro, /guide/intro",
          "/guide/draft/%2E./intro, /guide/intro",
          "/guide/draft/%2e%2E/intro, /guide/intro",
          "/guide/draft/.., /guide/",
          "/guide/draft/%2E%2E, /guide/",
          "/guide/.%20/%20./%2D%2E, /guide/.%20/%20./%2D%2E"
  })
  void normalizesOnlyCompleteDotSegments(String path, String expected) {
    var absolute = WhatWgUrlParser.parse("https://example.test" + path, null, StandardCharsets.UTF_8, null);
    var relative = WhatWgUrlParser.parse(path, EMPTY_URL_RECORD, StandardCharsets.UTF_8, null);

    assertThat(absolute.pathname()).isEqualTo(expected);
    assertThat(relative.pathname()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
          "custom+v1.2-test://example.test?key=value, custom+v1.2-test://example.test?key=value",
          "custom://example.test/#, custom://example.test/#",
          "custom://example.test?key=value, custom://example.test?key=value",
          "custom://example.test#top, custom://example.test#top",
          "custom:///path, custom:///path",
          "https://user@example.test/, https://user@example.test/",
          "https://:pass@example.test/, https://:pass@example.test/",
          "file:relative, file:///relative",
          "file://files.test?key=value, file://files.test/?key=value",
          "file://files.test#top, file://files.test/#top"
  })
  void serializesEmptyPathsAndOptionalUrlComponents(String input, String expected) {
    var url = WhatWgUrlParser.parse(input, null, StandardCharsets.UTF_8, null);
    assertThat(url.href()).isEqualTo(expected);
  }

  @Test
  void exposesNumericPortsAndRetainsPortAndSchemeTemplates() {
    var numeric = WhatWgUrlParser.parse("https://example.test:8443/", null, null, null);
    var template = WhatWgUrlParser.parse("{Scheme}://example.test:{Port}/", null, null, null);

    assertThat(numeric.port()).isInstanceOfSatisfying(WhatWgUrlParser.IntPort.class, port -> assertThat(port.value()).isEqualTo(8443));
    assertThat(template.port()).isInstanceOfSatisfying(WhatWgUrlParser.StringPort.class, port -> assertThat(port.value()).isEqualTo("{Port}"));
    assertThat(template.href()).isEqualTo("{Scheme}://example.test:{Port}/");
  }

  @Test
  void exposesAbsentComponentsAsEmptyStrings() {
    var opaque = WhatWgUrlParser.parse("mailto:reader@example.test#", null, null, null);

    assertThat(opaque.hostString()).isEmpty();
    assertThat(opaque.portString()).isEmpty();
    assertThat(opaque.hash()).isEmpty();
    assertThat(opaque.search()).isEmpty();
    assertThat(opaque.userInfo()).isNull();
    assertThat(opaque.href()).isEqualTo("mailto:reader@example.test#");
  }

  @Test
  void comparesEquivalentIpv4HostsIndependentlyOfTheirInputNotation() {
    var abbreviated = WhatWgUrlParser.parse("https://192.0.513/", null, null, null);
    var canonical = WhatWgUrlParser.parse("https://192.0.2.1/", null, null, null);
    var different = WhatWgUrlParser.parse("https://192.0.2.2/", null, null, null);

    assertThat(abbreviated).isEqualTo(canonical).hasSameHashCodeAs(canonical).isNotEqualTo(different);
    assertThat(abbreviated.host()).isInstanceOfSatisfying(WhatWgUrlParser.IpAddressHost.class, host ->
            assertThat(host.address()).hasToString("192.0.2.1"));
  }

  @Test
  void rejectsInternationalizedDomainsWithOverlongLabels() {
    var input = "https://" + "é".repeat(70) + ".test/";

    assertThatExceptionOfType(InvalidUrlException.class).isThrownBy(() -> WhatWgUrlParser.parse(input, null, null, null))
            .withMessageContaining("to ASCII").withCauseInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
          "https://192.0.2.1., 192.0.2.1",
          "https://192.0.2.0x, 192.0.2.0",
          "https://192.0.2.001, 192.0.2.1"
  })
  void explainsRecoverableIpv4Formats(String input, String host) {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse(input, null, null, errors::add);

    assertThat(url.hostString()).isEqualTo(host);
    assertThat(errors).isNotEmpty().allMatch(error -> error.contains("IPv4"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://one.2/", "https://192..2.1/", "https://192.0.2.09/" })
  void reportsNonnumericPartsInAnIpv4Address(String input) {
    assertThatExceptionOfType(InvalidUrlException.class).isThrownBy(() -> WhatWgUrlParser.parse(input, null, null, null))
            .withMessageContaining("An IPv4 address part is not numeric");
  }

  @Test
  void preservesDotsInDomainNamesThatDoNotEndInAnIpv4Number() {
    assertThat(WhatWgUrlParser.parse("https://example.test./", null, null, null).hostString()).isEqualTo("example.test.");
  }

  @ParameterizedTest
  @CsvSource({
          "'custom:opaque<name', 'custom:opaque<name'",
          "'custom://ex{host}/', 'custom://ex{host}/'",
          "'https://example.test/?q=<value>', 'https://example.test/?q=%3Cvalue%3E'",
          "'https://example.test/#<top>', 'https://example.test/#%3Ctop%3E'"
  })
  void reportsInvalidUrlUnitsAndUsesTheComponentEncodingSet(String input, String expected) {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse(input, null, StandardCharsets.UTF_8, errors::add);

    assertThat(url.href()).isEqualTo(expected);
    assertThat(errors).isNotEmpty().allMatch(error -> error.contains("URL unit") || error.contains("URL Unit"));
  }

  @ParameterizedTest
  @CsvSource({
          "file:///C:/base/item, file:/D:/next, file:///D:/next",
          "file://files.test/base/item, file:/next, file://files.test/next"
  })
  void distinguishesDriveReplacementFromFileHostInheritance(String base, String reference, String expected) {
    var baseUrl = WhatWgUrlParser.parse(base, null, null, null);

    assertThat(WhatWgUrlParser.parse(reference, baseUrl, null, null).href()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
          "file:\\path, file:///path",
          "file:/\\files.test/share, file://files.test/share"
  })
  void reportsBackslashInFileUrl(String input, String expected) {
    List<String> errors = new ArrayList<>();

    assertThat(WhatWgUrlParser.parse(input, null, null, errors::add).href()).isEqualTo(expected);
    assertThat(errors).anyMatch(error -> error.contains("instead of /"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.test/path%x4", "https://example.test/path%4x",
          "custom:opaque%x4", "https://example.test/?a=%4x" })
  void validatesBothDigitsOfPercentEscapes(String input) {
    List<String> errors = new ArrayList<>();

    assertThat(WhatWgUrlParser.parse(input, null, StandardCharsets.UTF_8, errors::add).href()).isEqualTo(input);
    assertThat(errors).singleElement().asString().contains("Invalid URL Unit");
  }

  @Test
  void reportsUnicodeNoncharactersButAcceptsAccentedLettersInAPath() {
    List<String> errors = new ArrayList<>();
    var url = WhatWgUrlParser.parse("https://example.test/café/\uFDD0", null, StandardCharsets.UTF_8, errors::add);

    assertThat(url.pathname()).isEqualTo("/caf%C3%A9/%EF%B7%90");
    assertThat(errors).singleElement().asString().contains("Invalid URL Unit", "\uFDD0");
  }

  @Test
  void encodesQueryApostrophesOnlyForSpecialSchemes() {
    var https = WhatWgUrlParser.parse("https://example.test/?name=O'Neil", null, StandardCharsets.UTF_8, null);
    var custom = WhatWgUrlParser.parse("custom://example.test/?name=O'Neil", null, StandardCharsets.UTF_8, null);

    assertThat(https.search()).isEqualTo("?name=O%27Neil");
    assertThat(custom.search()).isEqualTo("?name=O'Neil");
  }

  @Test
  void serializesUsernameWithoutAddingAnEmptyPassword() {
    var url = WhatWgUrlParser.parse("https://reader:@example.test/", null, null, null);

    assertThat(url.includesCredentials()).isTrue();
    assertThat(url.username()).isEqualTo("reader");
    assertThat(url.password()).isEmpty();
    assertThat(url.userInfo()).isEqualTo("reader");
    assertThat(url.href()).isEqualTo("https://reader@example.test/");
  }

}
