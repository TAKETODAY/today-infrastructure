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

package infra.mock.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.mock.api.http.Cookie;
import infra.util.FileCopyUtils;
import infra.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link HttpMockRequestImpl}.
 *
 * @author Rick Evans
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @author Jakub Narloch
 * @author Av Pinzur
 */
class MockHttpRequestTests {

  private static final String HOST = "Host";

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  @Test
  void protocolAndScheme() {
    assertThat(request.getProtocol()).isEqualTo(HttpMockRequestImpl.DEFAULT_PROTOCOL);
    assertThat(request.getScheme()).isEqualTo(HttpMockRequestImpl.DEFAULT_SCHEME);
    request.setProtocol("HTTP/2.0");
    request.setScheme("https");
    assertThat(request.getProtocol()).isEqualTo("HTTP/2.0");
    assertThat(request.getScheme()).isEqualTo("https");
  }

  @Test
  void setContentAndGetInputStream() throws IOException {
    byte[] bytes = "body".getBytes(Charset.defaultCharset());
    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset())).isEqualTo("body");

    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset())).isEqualTo("body");
  }

  @Test
  void readEmptyInputStreamWorksAcrossRequests() throws IOException {
    HttpMockRequestImpl firstRequest = new HttpMockRequestImpl();
    firstRequest.getInputStream().readAllBytes();
    firstRequest.getInputStream().close();

    HttpMockRequestImpl secondRequest = new HttpMockRequestImpl();
    secondRequest.getInputStream().readAllBytes();
    secondRequest.getInputStream().close();
  }

  @Test
  void setContentAndGetReader() throws IOException {
    byte[] bytes = "body".getBytes(Charset.defaultCharset());
    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(FileCopyUtils.copyToString(request.getReader())).isEqualTo("body");

    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(FileCopyUtils.copyToString(request.getReader())).isEqualTo("body");
  }

  @Test
  void setContentAndGetContentAsByteArray() {
    byte[] bytes = "request body".getBytes();
    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(request.getContentAsByteArray()).isEqualTo(bytes);
  }

  @Test
  void getContentAsStringWithoutSettingCharacterEncoding() throws IOException {
    assertThatIllegalStateException().isThrownBy(
                    request::getContentAsString)
            .withMessageContaining("Cannot get content as a String for a null character encoding");
  }

  @Test
  void setContentAndGetContentAsStringWithExplicitCharacterEncoding() throws IOException {
    String palindrome = "ablE was I ere I saw Elba";
    byte[] bytes = palindrome.getBytes(StandardCharsets.UTF_16);
    request.setCharacterEncoding("UTF-16");
    request.setContent(bytes);
    assertThat(request.getContentLength()).isEqualTo(bytes.length);
    assertThat(request.getContentAsString()).isEqualTo(palindrome);
  }

  @Test
  void noContent() throws IOException {
    assertThat(request.getContentLength()).isEqualTo(-1);
    assertThat(request.getInputStream().read()).isEqualTo(-1);
    assertThat(request.getContentAsByteArray()).isNull();
  }

  @Test

  void getReaderTwice() throws IOException {
    byte[] bytes = "body".getBytes(Charset.defaultCharset());
    request.setContent(bytes);
    assertThat(request.getReader()).isSameAs(request.getReader());
  }

  @Test

  void getInputStreamTwice() throws IOException {
    byte[] bytes = "body".getBytes(Charset.defaultCharset());
    request.setContent(bytes);
    assertThat(request.getInputStream()).isSameAs(request.getInputStream());
  }

  @Test

  void getReaderAfterGettingInputStream() throws IOException {
    request.getInputStream();
    assertThatIllegalStateException().isThrownBy(
                    request::getReader)
            .withMessageContaining("Cannot call getReader() after getInputStream() has already been called for the current request");
  }

  @Test

  void getInputStreamAfterGettingReader() throws IOException {
    request.getReader();
    assertThatIllegalStateException().isThrownBy(
                    request::getInputStream)
            .withMessageContaining("Cannot call getInputStream() after getReader() has already been called for the current request");
  }

  @Test
  void setContentType() {
    String contentType = "test/plain";
    request.setContentType(contentType);
    assertThat(request.getContentType()).isEqualTo(contentType);
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertThat(request.getCharacterEncoding()).isNull();
  }

  @Test
  void setContentTypeUTF8() {
    String contentType = "test/plain;charset=UTF-8";
    request.setContentType(contentType);
    assertThat(request.getContentType()).isEqualTo(contentType);
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void contentTypeHeader() {
    String contentType = "test/plain";
    request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
    assertThat(request.getContentType()).isEqualTo(contentType);
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertThat(request.getCharacterEncoding()).isNull();
  }

  @Test
  void contentTypeHeaderUTF8() {
    String contentType = "test/plain;charset=UTF-8";
    request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
    assertThat(request.getContentType()).isEqualTo(contentType);
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test

  void setContentTypeHeaderWithMoreComplexCharsetSyntax() {
    String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
    request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
    assertThat(request.getContentType()).isEqualTo(contentType);
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void setContentTypeThenCharacterEncoding() {
    request.setContentType("test/plain");
    request.setCharacterEncoding("UTF-8");
    assertThat(request.getContentType()).isEqualTo("test/plain");
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void setCharacterEncodingThenContentType() {
    request.setCharacterEncoding("UTF-8");
    request.setContentType("test/plain");
    assertThat(request.getContentType()).isEqualTo("test/plain");
    assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("test/plain;charset=UTF-8");
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void httpHeaderNameCasingIsPreserved() {
    String headerName = "Header1";
    request.addHeader(headerName, "value1");
    Enumeration<String> requestHeaders = request.getHeaderNames();
    assertThat(requestHeaders.nextElement()).as("HTTP header casing not being preserved").isEqualTo(headerName);
  }

  @Test
  void setMultipleParameters() {
    request.setParameter("key1", "value1");
    request.setParameter("key2", "value2");
    Map<String, Object> params = new HashMap<>(2);
    params.put("key1", "newValue1");
    params.put("key3", new String[] { "value3A", "value3B" });
    request.setParameters(params);
    String[] values1 = request.getParameterValues("key1");
    assertThat(values1.length).isEqualTo(1);
    assertThat(request.getParameter("key1")).isEqualTo("newValue1");
    assertThat(request.getParameter("key2")).isEqualTo("value2");
    String[] values3 = request.getParameterValues("key3");
    assertThat(values3.length).isEqualTo(2);
    assertThat(values3[0]).isEqualTo("value3A");
    assertThat(values3[1]).isEqualTo("value3B");
  }

  @Test
  void addMultipleParameters() {
    request.setParameter("key1", "value1");
    request.setParameter("key2", "value2");
    Map<String, Object> params = new HashMap<>(2);
    params.put("key1", "newValue1");
    params.put("key3", new String[] { "value3A", "value3B" });
    request.addParameters(params);
    String[] values1 = request.getParameterValues("key1");
    assertThat(values1.length).isEqualTo(2);
    assertThat(values1[0]).isEqualTo("value1");
    assertThat(values1[1]).isEqualTo("newValue1");
    assertThat(request.getParameter("key2")).isEqualTo("value2");
    String[] values3 = request.getParameterValues("key3");
    assertThat(values3.length).isEqualTo(2);
    assertThat(values3[0]).isEqualTo("value3A");
    assertThat(values3[1]).isEqualTo("value3B");
  }

  @Test
  void removeAllParameters() {
    request.setParameter("key1", "value1");
    Map<String, Object> params = new HashMap<>(2);
    params.put("key2", "value2");
    params.put("key3", new String[] { "value3A", "value3B" });
    request.addParameters(params);
    assertThat(request.getParameterMap().size()).isEqualTo(3);
    request.removeAllParameters();
    assertThat(request.getParameterMap().size()).isEqualTo(0);
  }

  @Test
  void cookies() {
    Cookie cookie1 = new Cookie("foo", "bar");
    Cookie cookie2 = new Cookie("baz", "qux");
    request.setCookies(cookie1, cookie2);

    Cookie[] cookies = request.getCookies();
    List<String> cookieHeaders = Collections.list(request.getHeaders(HttpHeaders.COOKIE));

    assertThat(cookies)
            .describedAs("Raw cookies stored as is")
            .hasSize(2)
            .satisfies(subject -> {
              assertThat(subject[0].getName()).isEqualTo("foo");
              assertThat(subject[0].getValue()).isEqualTo("bar");
              assertThat(subject[1].getName()).isEqualTo("baz");
              assertThat(subject[1].getValue()).isEqualTo("qux");
            });

    assertThat(cookieHeaders)
            .describedAs("Cookies -> Header conversion works as expected per RFC6265")
            .singleElement().isEqualTo("foo=bar; baz=qux");
  }

  @Test
  void noCookies() {
    assertThat(request.getCookies()).isNull();
  }

  @Test
  void defaultLocale() {
    Locale originalDefaultLocale = Locale.getDefault();
    try {
      Locale newDefaultLocale = originalDefaultLocale.equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
      Locale.setDefault(newDefaultLocale);
      // Create the request after changing the default locale.
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      assertThat(newDefaultLocale.equals(request.getLocale())).isFalse();
      assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
    }
    finally {
      Locale.setDefault(originalDefaultLocale);
    }
  }

  @Test
  void setPreferredLocalesWithNullList() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            request.setPreferredLocales(null));
  }

  @Test
  void setPreferredLocalesWithEmptyList() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            request.setPreferredLocales(new ArrayList<>()));
  }

  @Test
  void setPreferredLocales() {
    List<Locale> preferredLocales = Arrays.asList(Locale.ITALY, Locale.CHINA);
    request.setPreferredLocales(preferredLocales);
    assertEqualEnumerations(Collections.enumeration(preferredLocales), request.getLocales());
    assertThat(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).isEqualTo("it-it, zh-cn");
  }

  @Test
  void preferredLocalesFromAcceptLanguageHeader() {
    String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
    request.addHeader("Accept-Language", headerValue);
    List<Locale> actual = Collections.list(request.getLocales());
    assertThat(actual).isEqualTo(Arrays.asList(Locale.forLanguageTag("fr-ch"), Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("en"), Locale.forLanguageTag("de")));
    assertThat(request.getHeader("Accept-Language")).isEqualTo(headerValue);
  }

  @Test
  void invalidAcceptLanguageHeader() {
    request.addHeader("Accept-Language", "en_US");
    assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(request.getHeader("Accept-Language")).isEqualTo("en_US");
  }

  @Test
  void emptyAcceptLanguageHeader() {
    request.addHeader("Accept-Language", "");
    assertThat(request.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(request.getHeader("Accept-Language")).isEqualTo("");
  }

  @Test
  void getServerNameWithDefaultName() {
    assertThat(request.getServerName()).isEqualTo("localhost");
  }

  @Test
  void getServerNameWithCustomName() {
    request.setServerName("example.com");
    assertThat(request.getServerName()).isEqualTo("example.com");
  }

  @Test
  void getServerNameViaHostHeaderWithoutPort() {
    String testServer = "test.server";
    request.addHeader(HOST, testServer);
    assertThat(request.getServerName()).isEqualTo(testServer);
  }

  @Test
  void getServerNameViaHostHeaderWithPort() {
    String testServer = "test.server";
    request.addHeader(HOST, testServer + ":8080");
    assertThat(request.getServerName()).isEqualTo(testServer);
  }

  @Test
  void getServerNameWithInvalidIpv6AddressViaHostHeader() {
    request.addHeader(HOST, "[::ffff:abcd:abcd"); // missing closing bracket
    assertThatIllegalStateException()
            .isThrownBy(request::getServerName)
            .withMessageStartingWith("Invalid Host header: ");
  }

  @Test
  void getServerNameViaHostHeaderAsIpv6AddressWithoutPort() {
    String host = "[2001:db8:0:1]";
    request.addHeader(HOST, host);
    assertThat(request.getServerName()).isEqualTo(host);
  }

  @Test
  void getServerNameViaHostHeaderAsIpv6AddressWithPort() {
    request.addHeader(HOST, "[2001:db8:0:1]:8081");
    assertThat(request.getServerName()).isEqualTo("[2001:db8:0:1]");
  }

  @Test
  void getServerPortWithDefaultPort() {
    assertThat(request.getServerPort()).isEqualTo(80);
  }

  @Test
  void getServerPortWithCustomPort() {
    request.setServerPort(8080);
    assertThat(request.getServerPort()).isEqualTo(8080);
  }

  @Test
  void getServerPortWithInvalidIpv6AddressViaHostHeader() {
    request.addHeader(HOST, "[::ffff:abcd:abcd:8080"); // missing closing bracket
    assertThatIllegalStateException()
            .isThrownBy(request::getServerPort)
            .withMessageStartingWith("Invalid Host header: ");
  }

  @Test
  void getServerPortWithIpv6AddressAndInvalidPortViaHostHeader() {
    request.addHeader(HOST, "[::ffff:abcd:abcd]:bogus"); // "bogus" is not a port number
    assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(request::getServerPort)
            .withMessageContaining("bogus");
  }

  @Test
  void getServerPortViaHostHeaderAsIpv6AddressWithoutPort() {
    String testServer = "[2001:db8:0:1]";
    request.addHeader(HOST, testServer);
    assertThat(request.getServerPort()).isEqualTo(80);
  }

  @Test
  void getServerPortViaHostHeaderAsIpv6AddressWithPort() {
    String testServer = "[2001:db8:0:1]";
    int testPort = 9999;
    request.addHeader(HOST, testServer + ":" + testPort);
    assertThat(request.getServerPort()).isEqualTo(testPort);
  }

  @Test
  void getServerPortViaHostHeaderWithoutPort() {
    String testServer = "test.server";
    request.addHeader(HOST, testServer);
    assertThat(request.getServerPort()).isEqualTo(80);
  }

  @Test
  void getServerPortViaHostHeaderWithPort() {
    String testServer = "test.server";
    int testPort = 9999;
    request.addHeader(HOST, testServer + ":" + testPort);
    assertThat(request.getServerPort()).isEqualTo(testPort);
  }

  @Test
  void getRequestURL() {
    request.setServerPort(8080);
    request.setRequestURI("/path");
    assertThat(request.getRequestURL().toString()).isEqualTo("http://localhost:8080/path");

    request.setScheme("https");
    request.setServerName("example.com");
    request.setServerPort(8443);
    assertThat(request.getRequestURL().toString()).isEqualTo("https://example.com:8443/path");
  }

  @Test
  void getRequestURLWithDefaults() {
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo("http://localhost");
  }

  @Test

  void getRequestURLWithHostHeader() {
    String testServer = "test.server";
    request.addHeader(HOST, testServer);
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo(("http://" + testServer));
  }

  @Test

  void getRequestURLWithHostHeaderAndPort() {
    String testServer = "test.server:9999";
    request.addHeader(HOST, testServer);
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo(("http://" + testServer));
  }

  @Test
  void getRequestURLWithIpv6AddressViaServerNameWithoutPort() throws Exception {
    request.setServerName("[::ffff:abcd:abcd]");
    URL url = new URL(request.getRequestURL().toString());
    assertThat(url).asString().isEqualTo("http://[::ffff:abcd:abcd]");
  }

  @Test
  void getRequestURLWithIpv6AddressViaServerNameWithPort() throws Exception {
    request.setServerName("[::ffff:abcd:abcd]");
    request.setServerPort(9999);
    URL url = new URL(request.getRequestURL().toString());
    assertThat(url).asString().isEqualTo("http://[::ffff:abcd:abcd]:9999");
  }

  @Test
  void getRequestURLWithInvalidIpv6AddressViaHostHeader() {
    request.addHeader(HOST, "[::ffff:abcd:abcd"); // missing closing bracket
    assertThatIllegalStateException()
            .isThrownBy(request::getRequestURL)
            .withMessageStartingWith("Invalid Host header: ");
  }

  @Test
  void getRequestURLWithIpv6AddressViaHostHeaderWithoutPort() throws Exception {
    request.addHeader(HOST, "[::ffff:abcd:abcd]");
    URL url = new URL(request.getRequestURL().toString());
    assertThat(url).asString().isEqualTo("http://[::ffff:abcd:abcd]");
  }

  @Test
  void getRequestURLWithIpv6AddressViaHostHeaderWithPort() throws Exception {
    request.addHeader(HOST, "[::ffff:abcd:abcd]:9999");
    URL url = new URL(request.getRequestURL().toString());
    assertThat(url).asString().isEqualTo("http://[::ffff:abcd:abcd]:9999");
  }

  @Test
  void getRequestURLWithNullRequestUri() {
    request.setRequestURI(null);
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo("http://localhost");
  }

  @Test
  void getRequestURLWithDefaultsAndHttps() {
    request.setScheme("https");
    request.setServerPort(443);
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo("https://localhost");
  }

  @Test
  void getRequestURLWithNegativePort() {
    request.setServerPort(-99);
    StringBuffer requestURL = request.getRequestURL();
    assertThat(requestURL.toString()).isEqualTo("http://localhost");
  }

  @Test
  void isSecureWithHttpSchemeAndSecureFlagIsFalse() {
    assertThat(request.isSecure()).isFalse();
    request.setScheme("http");
    request.setSecure(false);
    assertThat(request.isSecure()).isFalse();
  }

  @Test
  void isSecureWithHttpSchemeAndSecureFlagIsTrue() {
    assertThat(request.isSecure()).isFalse();
    request.setScheme("http");
    request.setSecure(true);
    assertThat(request.isSecure()).isTrue();
  }

  @Test
  void isSecureWithHttpsSchemeAndSecureFlagIsFalse() {
    assertThat(request.isSecure()).isFalse();
    request.setScheme("https");
    request.setSecure(false);
    assertThat(request.isSecure()).isTrue();
  }

  @Test
  void isSecureWithHttpsSchemeAndSecureFlagIsTrue() {
    assertThat(request.isSecure()).isFalse();
    request.setScheme("https");
    request.setSecure(true);
    assertThat(request.isSecure()).isTrue();
  }

  @Test
  void httpHeaderDate() {
    Date date = new Date();
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, date);
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(date.getTime());
  }

  @Test
  void httpHeaderTimestamp() {
    long timestamp = new Date().getTime();
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, timestamp);
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(timestamp);
  }

  @Test
  void httpHeaderRfcFormattedDate() {
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21 Jul 2015 10:00:00 GMT");
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
  }

  @Test
  void httpHeaderFirstVariantFormattedDate() {
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21-Jul-15 10:00:00 GMT");
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
  }

  @Test
  void httpHeaderSecondVariantFormattedDate() {
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue Jul 21 10:00:00 2015");
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(1437472800000L);
  }

  @Test
  void httpHeaderFormattedDateError() {
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "This is not a date");
    assertThatIllegalArgumentException().isThrownBy(() ->
            request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
  }

  private void assertEqualEnumerations(Enumeration<?> enum1, Enumeration<?> enum2) {
    int count = 0;
    while (enum1.hasMoreElements()) {
      assertThat(enum2.hasMoreElements()).as("enumerations must be equal in length").isTrue();
      String message = "enumeration element #" + ++count;
      assertThat(enum2.nextElement()).as(message).isEqualTo(enum1.nextElement());
    }
  }

}
