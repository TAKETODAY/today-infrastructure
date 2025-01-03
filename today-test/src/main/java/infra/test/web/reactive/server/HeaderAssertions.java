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

package infra.test.web.reactive.server;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import infra.http.CacheControl;
import infra.http.ContentDisposition;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.test.util.AssertionErrors;
import infra.util.CollectionUtils;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.assertNotNull;
import static infra.test.util.AssertionErrors.assertTrue;

/**
 * Assertions on headers of the response.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @see WebTestClient.ResponseSpec#expectHeader()
 * @since 4.0
 */
public class HeaderAssertions {

  private final ExchangeResult exchangeResult;

  private final WebTestClient.ResponseSpec responseSpec;

  HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
    this.exchangeResult = result;
    this.responseSpec = spec;
  }

  /**
   * Expect a header with the given name to match the specified values.
   */
  public WebTestClient.ResponseSpec valueEquals(String headerName, String... values) {
    return assertHeader(headerName, Arrays.asList(values), getHeaders().getOrEmpty(headerName));
  }

  /**
   * Expect a header with the given name to match the given long value.
   */
  public WebTestClient.ResponseSpec valueEquals(String headerName, long value) {
    String actual = getHeaders().getFirst(headerName);
    this.exchangeResult.assertWithDiagnostics(() ->
            assertTrue("Response does not contain header '" + headerName + "'", actual != null));
    return assertHeader(headerName, value, Long.parseLong(Objects.requireNonNull(actual)));
  }

  /**
   * Expect a header with the given name to match the specified long value
   * parsed into a date using the preferred date format described in RFC 7231.
   * <p>An {@link AssertionError} is thrown if the response does not contain
   * the specified header, or if the supplied {@code value} does not match the
   * primary header value.
   *
   * @since 4.0
   */
  public WebTestClient.ResponseSpec valueEqualsDate(String headerName, long value) {
    this.exchangeResult.assertWithDiagnostics(() -> {
      String headerValue = getHeaders().getFirst(headerName);
      assertNotNull("Response does not contain header '" + headerName + "'", headerValue);

      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setDate("expected", value);
      headers.setOrRemove("actual", headerValue);

      assertEquals("Response header '" + headerName + "'='" + headerValue + "' " +
                      "does not match expected value '" + headers.getFirst("expected") + "'",
              headers.getFirstDate("expected"), headers.getFirstDate("actual"));
    });
    return this.responseSpec;
  }

  /**
   * Match the first value of the response header with a regex.
   *
   * @param name the header name
   * @param pattern the regex pattern
   */
  public WebTestClient.ResponseSpec valueMatches(String name, String pattern) {
    String value = getRequiredValue(name);
    String message = getMessage(name) + "=[" + value + "] does not match [" + pattern + "]";
    this.exchangeResult.assertWithDiagnostics(() -> assertTrue(message, value.matches(pattern)));
    return this.responseSpec;
  }

  /**
   * Match all values of the response header with the given regex
   * patterns which are applied to the values of the header in the
   * same order. Note that the number of patterns must match the
   * number of actual values.
   *
   * @param name the header name
   * @param patterns one or more regex patterns, one per expected value
   * @since 4.0
   */
  public WebTestClient.ResponseSpec valuesMatch(String name, String... patterns) {
    this.exchangeResult.assertWithDiagnostics(() -> {
      List<String> values = getRequiredValues(name);
      assertTrue(
              getMessage(name) + " has fewer or more values " + values +
                      " than number of patterns to match with " + Arrays.toString(patterns),
              values.size() == patterns.length);
      for (int i = 0; i < values.size(); i++) {
        String value = values.get(i);
        String pattern = patterns[i];
        assertTrue(
                getMessage(name) + "[" + i + "]='" + value + "' does not match '" + pattern + "'",
                value.matches(pattern));
      }
    });
    return this.responseSpec;
  }

  /**
   * Assert the first value of the response header with a Hamcrest {@link Matcher}.
   *
   * @param name the header name
   * @param matcher the matcher to use
   */
  public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
    String value = getHeaders().getFirst(name);
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name);
      MatcherAssert.assertThat(message, value, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Assert all values of the response header with a Hamcrest {@link Matcher}.
   *
   * @param name the header name
   * @param matcher the matcher to use
   * @since 4.0
   */
  public WebTestClient.ResponseSpec values(String name, Matcher<? super Iterable<String>> matcher) {
    List<String> values = getHeaders().get(name);
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name);
      MatcherAssert.assertThat(message, values, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Consume the first value of the named response header.
   *
   * @param name the header name
   * @param consumer the consumer to use
   */
  public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
    String value = getRequiredValue(name);
    this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
    return this.responseSpec;
  }

  /**
   * Consume all values of the named response header.
   *
   * @param name the header name
   * @param consumer the consumer to use
   * @since 4.0
   */
  public WebTestClient.ResponseSpec values(String name, Consumer<List<String>> consumer) {
    List<String> values = getRequiredValues(name);
    this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(values));
    return this.responseSpec;
  }

  private String getRequiredValue(String name) {
    return getRequiredValues(name).get(0);
  }

  private List<String> getRequiredValues(String name) {
    List<String> values = getHeaders().get(name);
    if (CollectionUtils.isEmpty(values)) {
      this.exchangeResult.assertWithDiagnostics(() ->
              AssertionErrors.fail(getMessage(name) + " not found"));
    }
    return Objects.requireNonNull(values);
  }

  /**
   * Expect that the header with the given name is present.
   */
  public WebTestClient.ResponseSpec exists(String name) {
    if (!getHeaders().containsKey(name)) {
      String message = getMessage(name) + " does not exist";
      this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
    }
    return this.responseSpec;
  }

  /**
   * Expect that the header with the given name is not present.
   */
  public WebTestClient.ResponseSpec doesNotExist(String name) {
    if (getHeaders().containsKey(name)) {
      String message = getMessage(name) + " exists with value=[" + getHeaders().getFirst(name) + "]";
      this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
    }
    return this.responseSpec;
  }

  /**
   * Expect a "Cache-Control" header with the given value.
   */
  public WebTestClient.ResponseSpec cacheControl(CacheControl cacheControl) {
    return assertHeader("Cache-Control", cacheControl.getHeaderValue(), getHeaders().getCacheControl());
  }

  /**
   * Expect a "Content-Disposition" header with the given value.
   */
  public WebTestClient.ResponseSpec contentDisposition(ContentDisposition contentDisposition) {
    return assertHeader("Content-Disposition", contentDisposition, getHeaders().getContentDisposition());
  }

  /**
   * Expect a "Content-Length" header with the given value.
   */
  public WebTestClient.ResponseSpec contentLength(long contentLength) {
    return assertHeader("Content-Length", contentLength, getHeaders().getContentLength());
  }

  /**
   * Expect a "Content-Type" header with the given value.
   */
  public WebTestClient.ResponseSpec contentType(MediaType mediaType) {
    return assertHeader("Content-Type", mediaType, getHeaders().getContentType());
  }

  /**
   * Expect a "Content-Type" header with the given value.
   */
  public WebTestClient.ResponseSpec contentType(String mediaType) {
    return contentType(MediaType.parseMediaType(mediaType));
  }

  /**
   * Expect a "Content-Type" header compatible with the given value.
   */
  public WebTestClient.ResponseSpec contentTypeCompatibleWith(MediaType mediaType) {
    MediaType actual = getHeaders().getContentType();
    String message = getMessage("Content-Type") + "=[" + actual + "] is not compatible with [" + mediaType + "]";
    this.exchangeResult.assertWithDiagnostics(() ->
            assertTrue(message, (actual != null && actual.isCompatibleWith(mediaType))));
    return this.responseSpec;
  }

  /**
   * Expect a "Content-Type" header compatible with the given value.
   */
  public WebTestClient.ResponseSpec contentTypeCompatibleWith(String mediaType) {
    return contentTypeCompatibleWith(MediaType.parseMediaType(mediaType));
  }

  /**
   * Expect an "Expires" header with the given value.
   */
  public WebTestClient.ResponseSpec expires(long expires) {
    return assertHeader("Expires", expires, getHeaders().getExpires());
  }

  /**
   * Expect a "Last-Modified" header with the given value.
   */
  public WebTestClient.ResponseSpec lastModified(long lastModified) {
    return assertHeader("Last-Modified", lastModified, getHeaders().getLastModified());
  }

  /**
   * Expect a "Location" header with the given value.
   *
   * @since 4.0
   */
  public WebTestClient.ResponseSpec location(String location) {
    return assertHeader("Location", URI.create(location), getHeaders().getLocation());
  }

  private HttpHeaders getHeaders() {
    return this.exchangeResult.getResponseHeaders();
  }

  private String getMessage(String headerName) {
    return "Response header '" + headerName + "'";
  }

  private WebTestClient.ResponseSpec assertHeader(String name, @Nullable Object expected, @Nullable Object actual) {
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name);
      assertEquals(message, expected, actual);
    });
    return this.responseSpec;
  }

}
