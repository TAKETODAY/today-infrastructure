/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RfcUriParserTests {

  @ParameterizedTest
  @ValueSource(strings = { "", "/", "*", ".", ";name", "%41", "/%41", "file%20name", "/letter%u004A" })
  void preservesValidRequestPaths(String input) {
    assertThat(RfcUriParser.parse(input).path()).isEqualTo(input);
  }

  @Test
  void handlesQueryAndFragmentWithoutAPath() {
    assertThat(RfcUriParser.parse("?name=value#top").query()).isEqualTo("name=value");
    assertThat(RfcUriParser.parse("#top").fragment()).isEqualTo("top");
    assertThat(RfcUriParser.parse("https://example.test#top").fragment()).isEqualTo("top");
    assertThat(RfcUriParser.parse("https://example.test:8080#top").port()).isEqualTo("8080");
    assertThat(RfcUriParser.parse("https://example.test:8080?name=value").query()).isEqualTo("name=value");
  }

  @Test
  void separatesRelativePathAndQueryButRetainsQueryInOpaquePart() {
    var relative = RfcUriParser.parse("items?name=value#top");
    assertThat(relative.path()).isEqualTo("items");
    assertThat(relative.query()).isEqualTo("name=value");
    assertThat(relative.fragment()).isEqualTo("top");

    var opaque = RfcUriParser.parse("mailto:reader@example.test?subject=Hello#top");
    assertThat(opaque.isOpaque()).isTrue();
    assertThat(opaque.path()).isEqualTo("reader@example.test?subject=Hello");
    assertThat(opaque.query()).isNull();
    assertThat(opaque.fragment()).isEqualTo("top");
  }

  @Test
  void preservesEncodedHostAndParsesIpv6WithOrWithoutPort() {
    assertThat(RfcUriParser.parse("https://caf%C3%A9.test/").host()).isEqualTo("caf%C3%A9.test");
    assertThat(RfcUriParser.parse("https://[2001:db8::1]/path").path()).isEqualTo("/path");
    var ipv6 = RfcUriParser.parse("https://[2001:db8::1]:8443/path");
    assertThat(ipv6.host()).isEqualTo("[2001:db8::1]");
    assertThat(ipv6.port()).isEqualTo("8443");
  }

  @Test
  void distinguishesNumericCredentialsFromPorts() {
    var uri = RfcUriParser.parse("https://reader:1234@example.test:8443");
    assertThat(uri.user()).isEqualTo("reader:1234");
    assertThat(uri.host()).isEqualTo("example.test");
    assertThat(uri.port()).isEqualTo("8443");
    assertThat(RfcUriParser.parse("https://reader:p%40ss@example.test").user()).isEqualTo("reader:p%40ss");
  }

  @ParameterizedTest
  @CsvSource({
          "*suffix, Bad character",
          "https://[2001:db8::1]extra, Bad authority",
          "https://[2001:db8::z], Bad authority",
          "https://example.test:!, Bad authority",
          "https://example.test:{port, Bad authority",
          "https://example.test}, Bad authority",
          "https://example.test[::1]/, Bad authority",
          "https://reader@second:1234@example.test, Bad authority",
          "https://caf%zx.test/, Bad authority",
          "https://caf%/, Bad authority",
          "https://caf%%41.test/, Bad encoding",
          "/bad%u1x00, Bad path"
  })
  void rejectsSyntaxErrors(String input, String reason) {
    assertThatExceptionOfType(InvalidUrlException.class).isThrownBy(() -> RfcUriParser.parse(input))
            .withMessageContaining(reason);
  }

}
