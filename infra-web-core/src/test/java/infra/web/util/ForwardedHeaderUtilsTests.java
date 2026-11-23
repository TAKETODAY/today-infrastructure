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

import java.net.InetSocketAddress;
import java.net.URI;

import infra.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ForwardedHeaderUtilsTests {

  private static final URI REQUEST = URI.create("http://backend.test:8080/api?name=one%20two");

  @Test
  void parsesFirstStandardElementWithoutResolvingAddresses() {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "\t; Proto = https; ext=\"semi; comma, quote\\\"\"; host=\"[2001:db8::1]:443\"; "
            + "for=\"[2001:db8::2]:1234\"; by=edge.test; proto=http, host=another.test");

    var result = ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null,
            InetSocketAddress.createUnresolved("backend.test", 8080));

    assertThat(result.uri()).hasToString("https://[2001:db8::1]/api?name=one%20two");
    assertThat(result.forAddress()).isEqualTo(InetSocketAddress.createUnresolved("[2001:db8::2]", 1234));
    assertThat(result.byAddress()).isEqualTo(InetSocketAddress.createUnresolved("edge.test", 8080));
  }

  @Test
  void usesRequestSchemeForDefaultPortsWhenSocketAddressesAreUnavailable() {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "unrecognized; host=edge.test; for=client.test; by=proxy.test");

    var result = ForwardedHeaderUtils.parseStandardHeader(URI.create("https://backend.test:8443"), headers, null, null);

    assertThat(result.uri()).hasToString("https://edge.test");
    assertThat(result.forAddress()).isEqualTo(InetSocketAddress.createUnresolved("client.test", 443));
    assertThat(result.byAddress()).isEqualTo(InetSocketAddress.createUnresolved("proxy.test", 443));
    assertThat(ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null).forAddress())
            .isEqualTo(InetSocketAddress.createUnresolved("client.test", 80));
  }

  @Test
  void keepsOriginalUriWhenNoForwardingHeaderIsPresent() {
    var headers = HttpHeaders.forWritable();
    var standard = ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null);
    var legacy = ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers, null, null);

    assertThat(standard.uri()).isEqualTo(REQUEST);
    assertThat(standard.forAddress()).isNull();
    assertThat(standard.byAddress()).isNull();
    assertThat(legacy.uri()).isEqualTo(REQUEST);
    assertThat(legacy.forAddress()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
          "host=2001:db8::1, Invalid IPv4 address",
          "host=edge.test:port, Failed to parse port in forwarded host",
          "for=2001:db8::1, Invalid IPv4 address",
          "by=edge.test:port, Failed to parse port in forwarded address"
  })
  void rejectsMalformedStandardAddresses(String value, String reason) {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", value);

    assertThatIllegalArgumentException().isThrownBy(() -> ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null))
            .withMessageContaining(reason);
  }

  @ParameterizedTest
  @CsvSource({ "2001:db8::2, [2001:db8::2]", "[2001:db8::2], [2001:db8::2]", "client.test, client.test" })
  void takesFirstLegacyValuesAndLetsExplicitPortOverrideTheHostPort(String client, String expectedHost) {
    var headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Proto", " https , http");
    headers.add("X-Forwarded-Host", "edge.test:8443, proxy.test");
    headers.add("X-Forwarded-Port", "443, 8080");
    headers.add("X-Forwarded-For", client + ", proxy.test");

    var result = ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers,
            InetSocketAddress.createUnresolved("proxy.test", 1234), null);

    assertThat(result.uri()).hasToString("https://edge.test/api?name=one%20two");
    assertThat(result.forAddress()).isEqualTo(InetSocketAddress.createUnresolved(expectedHost, 1234));
    assertThat(result.byAddress()).isNull();
  }

  @Test
  void recognizesLegacySslFlagOnlyWhenEnabledAndNoProtocolIsSupplied() {
    var headers = HttpHeaders.forWritable();
    headers.set("X-Forwarded-Ssl", "off");
    assertThat(ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers, null, null).uri()).isEqualTo(REQUEST);
    headers.set("X-Forwarded-Ssl", "ON");
    assertThat(ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers, null, null).uri())
            .hasToString("https://backend.test/api?name=one%20two");
    headers.set("X-Forwarded-Proto", "http");
    assertThat(ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers, null, null).uri())
            .hasToString("http://backend.test/api?name=one%20two");
  }

  @Test
  void reportsInvalidLegacyPort() {
    var headers = HttpHeaders.forWritable();
    headers.add("X-Forwarded-Port", "not-a-port");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> ForwardedHeaderUtils.parseXForwardedHeaders(REQUEST, headers, null, null))
            .withMessageContaining("X-Forwarded-Port", "not-a-port");
  }

  @Test
  void reportsWhenForwardedComponentsCannotBeConvertedToAUri() {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "host=\"not a host\"");
    var result = ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null);

    assertThatIllegalStateException().isThrownBy(result::uri).withMessageContaining("Could not create URI object");
  }

  @Test
  void ignoresOtherElementsWhenTheFirstForwardingElementIsEmpty() {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", " \t,proto=https;host=edge.test");

    assertThat(ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null).uri()).isEqualTo(REQUEST);
  }

  @Test
  void ignoresExtensionPairsAndTrailingWhitespaceAfterARecognizedPair() {
    var headers = HttpHeaders.forWritable();
    headers.add("Forwarded", "!#$%&'*+-.^_`|~=extension; @ignored=value; host=edge.test; \t");

    assertThat(ForwardedHeaderUtils.parseStandardHeader(REQUEST, headers, null, null).uri())
            .hasToString("http://edge.test/api?name=one%20two");
  }

}
