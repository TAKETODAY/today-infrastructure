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

package cn.taketoday.web.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.MockHttpServletResponse;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for {@link }.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 * @author Sam Brannen
 */
class ServletWebRequestHttpMethodsTests {

  private static final String CURRENT_TIME = "Wed, 9 Apr 2014 09:57:42 GMT";

  private static final Instant NOW = Instant.now();

  private final HttpMockRequestImpl servletRequest = new HttpMockRequestImpl();

  private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

  private final ServletRequestContext request = new ServletRequestContext(null, servletRequest, servletResponse);

  @Test
  void ifMatchWildcardShouldMatchWhenETagPresent() {
    setUpRequest("PUT");
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
    assertThat(request.checkNotModified("\"SomeETag\"")).isFalse();
  }

  @Test
  void ifMatchWildcardShouldMatchETagMissing() {
    setUpRequest("PUT");
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
    assertThat(request.checkNotModified("")).isTrue();
    assertPreconditionFailed();
  }

  @Test
  void ifMatchValueShouldMatchWhenETagMatches() {
    setUpRequest("PUT");
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"first\"");
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"second\"");
    assertThat(request.checkNotModified("\"second\"")).isFalse();
  }

  @Test
  void ifMatchValueShouldRejectWhenETagDoesNotMatch() {
    setUpRequest("PUT");
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"first\"");
    assertThat(request.checkNotModified("\"second\"")).isTrue();
    assertPreconditionFailed();
  }

  @Test
  void ifMatchValueShouldUseStrongComparison() {
    setUpRequest("PUT");
    String eTag = "\"spring\"";
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "W/" + eTag);
    assertThat(request.checkNotModified(eTag)).isTrue();
    assertPreconditionFailed();
  }

  @SafeHttpMethodsTest
  void ifMatchShouldOnlyBeConsideredForUnsafeMethods(String method) {
    setUpRequest(method);
    servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
    assertThat(request.checkNotModified("\"spring\"")).isFalse();
  }

  @Test
  void ifUnModifiedSinceShouldMatchValueWhenLater() {
    setUpRequest("PUT");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, now.toEpochMilli());
    assertThat(request.checkNotModified(oneMinuteAgo.toEpochMilli())).isFalse();
    assertThat(servletResponse.getStatus()).isEqualTo(200);
    assertThat(servletResponse.getHeader(HttpHeaders.LAST_MODIFIED)).isNull();
  }

  @Test
  void ifUnModifiedSinceShouldNotMatchValueWhenEarlier() {
    setUpRequest("PUT");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, oneMinuteAgo.toEpochMilli());
    assertThat(request.checkNotModified(now.toEpochMilli())).isTrue();
    assertPreconditionFailed();
  }

  @SafeHttpMethodsTest
  void ifNoneMatchShouldMatchIdenticalETagValue(String method) {
    setUpRequest(method);
    String etag = "\"spring\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    assertThat(request.checkNotModified(etag)).isTrue();
    assertNotModified(etag, null);
  }

  @SafeHttpMethodsTest
  void ifNoneMatchShouldMatchETagWithSeparatorChar(String method) {
    setUpRequest(method);
    String etag = "\"spring,framework\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    assertThat(request.checkNotModified(etag)).isTrue();
    assertNotModified(etag, null);
  }

  @SafeHttpMethodsTest
  void ifNoneMatchShouldNotMatchDifferentETag(String method) {
    setUpRequest(method);
    String etag = "\"framework\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "\"spring\"");
    assertThat(request.checkNotModified(etag)).isFalse();
    assertOkWithETag(etag);
  }

  @SafeHttpMethodsTest
    // SPR-14559
  void ifNoneMatchShouldNotFailForUnquotedETag(String method) {
    setUpRequest(method);
    String etag = "\"etagvalue\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "missingquotes");
    assertThat(request.checkNotModified(etag)).isFalse();
    assertOkWithETag(etag);
  }

  @SafeHttpMethodsTest
  void ifNoneMatchShouldMatchPaddedETag(String method) {
    setUpRequest(method);
    String etag = "spring";
    String paddedEtag = String.format("\"%s\"", etag);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, paddedEtag);
    assertThat(request.checkNotModified(etag)).isTrue();
    assertNotModified(paddedEtag, null);
  }

  @SafeHttpMethodsTest
  void ifNoneMatchShouldIgnoreWildcard(String method) {
    setUpRequest(method);
    String etag = "\"spring\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
    assertThat(request.checkNotModified(etag)).isFalse();
    assertOkWithETag(etag);
  }

  @Test
  void ifNoneMatchShouldRejectWildcardForUnsafeMethods() {
    setUpRequest("PUT");
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
    assertThat(request.checkNotModified("\"spring\"")).isTrue();
    assertPreconditionFailed();
  }

  @SafeHttpMethodsTest
  void ifNoneMatchValueShouldUseWeakComparison(String method) {
    setUpRequest(method);
    String etag = "\"spring\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "W/" + etag);
    assertThat(request.checkNotModified(etag)).isTrue();
    assertNotModified(etag, null);
  }

  @SafeHttpMethodsTest
  void ifModifiedSinceShouldMatchIfDatesEqual(String method) {
    setUpRequest(method);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
    assertThat(request.checkNotModified(NOW.toEpochMilli())).isTrue();
    assertNotModified(null, NOW);
  }

  @SafeHttpMethodsTest
  void ifModifiedSinceShouldNotMatchIfDateAfter(String method) {
    setUpRequest(method);
    Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
    assertThat(request.checkNotModified(oneMinuteLater.toEpochMilli())).isFalse();
    assertOkWithLastModified(oneMinuteLater);
  }

  @SafeHttpMethodsTest
  void ifModifiedSinceShouldNotOverrideResponseStatus(String method) {
    setUpRequest(method);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
    servletResponse.setStatus(304);
    assertThat(request.checkNotModified(NOW.toEpochMilli())).isFalse();
    assertNotModified(null, null);
  }

  @SafeHttpMethodsTest
    // SPR-13516
  void ifModifiedSinceShouldNotFailForInvalidResponseStatus(String method) {
    setUpRequest(method);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
    servletResponse.setStatus(0);
    assertThat(request.checkNotModified(NOW.toEpochMilli())).isFalse();
  }

  @SafeHttpMethodsTest
  void ifModifiedSinceShouldNotFailForTimestampWithLengthPart(String method) {
    setUpRequest(method);
    long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

    assertThat(request.checkNotModified(epochTime)).isTrue();
    assertNotModified(null, Instant.ofEpochMilli(epochTime));
  }

  @SafeHttpMethodsTest
  void IfNoneMatchAndIfNotModifiedSinceShouldMatchWhenSameETagAndDate(String method) {
    setUpRequest(method);
    String etag = "\"spring\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
    assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isTrue();
    assertNotModified(etag, NOW);
  }

  @SafeHttpMethodsTest
  void IfNoneMatchAndIfNotModifiedSinceShouldMatchWhenSameETagAndLaterDate(String method) {
    setUpRequest(method);
    String etag = "\"spring\"";
    Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteLater.toEpochMilli());
    assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isTrue();
    assertNotModified(etag, NOW);
  }

  @SafeHttpMethodsTest
  void IfNoneMatchAndIfNotModifiedSinceShouldNotMatchWhenDifferentETag(String method) {
    setUpRequest(method);
    String etag = "\"framework\"";
    Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "\"spring\"");
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteLater.toEpochMilli());
    assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isFalse();
    assertOkWithETag(etag);
    assertOkWithLastModified(NOW);
  }

  private void setUpRequest(String method) {
    this.servletRequest.setMethod(method);
    this.servletRequest.setRequestURI("https://example.org");
  }

  private void assertPreconditionFailed() {
    assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
  }

  private void assertNotModified(@Nullable String eTag, @Nullable Instant lastModified) {
    flush();
    assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
    if (eTag != null) {
      assertThat(servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(eTag);
    }
    if (lastModified != null) {
      assertThat(servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000)
              .isEqualTo(lastModified.toEpochMilli() / 1000);
    }
  }

  private void assertOkWithETag(String eTag) {
    flush();
    assertThat(servletResponse.getStatus()).isEqualTo(200);
    assertThat(servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(eTag);
  }

  private void flush() {
    try {
      request.flush();
    }
    catch (Exception e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  private void assertOkWithLastModified(Instant lastModified) {
    flush();
    assertThat(servletResponse.getStatus()).isEqualTo(200);
    assertThat(servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000)
            .isEqualTo(lastModified.toEpochMilli() / 1000);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @ValueSource(strings = { "GET", "HEAD" })
  @interface SafeHttpMethodsTest {
  }

}
