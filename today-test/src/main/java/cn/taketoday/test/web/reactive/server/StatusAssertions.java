/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.reactive.server;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.util.function.Consumer;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.test.util.AssertionErrors;

/**
 * Assertions on the response status.
 *
 * @author Rossen Stoyanchev
 * @see WebTestClient.ResponseSpec#expectStatus()
 * @since 4.0
 */
public class StatusAssertions {

  private final ExchangeResult exchangeResult;

  private final WebTestClient.ResponseSpec responseSpec;

  StatusAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
    this.exchangeResult = result;
    this.responseSpec = spec;
  }

  /**
   * Assert the response status as an {@link HttpStatusCode}.
   */
  public WebTestClient.ResponseSpec isEqualTo(HttpStatusCode status) {
    HttpStatusCode actual = this.exchangeResult.getStatus();
    this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", status, actual));
    return this.responseSpec;
  }

  /**
   * Assert the response status as an integer.
   */
  public WebTestClient.ResponseSpec isEqualTo(int status) {
    return isEqualTo(HttpStatusCode.valueOf(status));
  }

  /**
   * Assert the response status code is {@code HttpStatus.OK} (200).
   */
  public WebTestClient.ResponseSpec isOk() {
    return assertStatusAndReturn(HttpStatus.OK);
  }

  /**
   * Assert the response status code is {@code HttpStatus.CREATED} (201).
   */
  public WebTestClient.ResponseSpec isCreated() {
    return assertStatusAndReturn(HttpStatus.CREATED);
  }

  /**
   * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
   */
  public WebTestClient.ResponseSpec isAccepted() {
    return assertStatusAndReturn(HttpStatus.ACCEPTED);
  }

  /**
   * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
   */
  public WebTestClient.ResponseSpec isNoContent() {
    return assertStatusAndReturn(HttpStatus.NO_CONTENT);
  }

  /**
   * Assert the response status code is {@code HttpStatus.FOUND} (302).
   */
  public WebTestClient.ResponseSpec isFound() {
    return assertStatusAndReturn(HttpStatus.FOUND);
  }

  /**
   * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
   */
  public WebTestClient.ResponseSpec isSeeOther() {
    return assertStatusAndReturn(HttpStatus.SEE_OTHER);
  }

  /**
   * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
   */
  public WebTestClient.ResponseSpec isNotModified() {
    return assertStatusAndReturn(HttpStatus.NOT_MODIFIED);
  }

  /**
   * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
   */
  public WebTestClient.ResponseSpec isTemporaryRedirect() {
    return assertStatusAndReturn(HttpStatus.TEMPORARY_REDIRECT);
  }

  /**
   * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
   */
  public WebTestClient.ResponseSpec isPermanentRedirect() {
    return assertStatusAndReturn(HttpStatus.PERMANENT_REDIRECT);
  }

  /**
   * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
   */
  public WebTestClient.ResponseSpec isBadRequest() {
    return assertStatusAndReturn(HttpStatus.BAD_REQUEST);
  }

  /**
   * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
   */
  public WebTestClient.ResponseSpec isUnauthorized() {
    return assertStatusAndReturn(HttpStatus.UNAUTHORIZED);
  }

  /**
   * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
   */
  public WebTestClient.ResponseSpec isForbidden() {
    return assertStatusAndReturn(HttpStatus.FORBIDDEN);
  }

  /**
   * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
   */
  public WebTestClient.ResponseSpec isNotFound() {
    return assertStatusAndReturn(HttpStatus.NOT_FOUND);
  }

  /**
   * Assert the response error message.
   */
  public WebTestClient.ResponseSpec reasonEquals(String reason) {
    String actual = getReasonPhrase(this.exchangeResult.getStatus());
    this.exchangeResult.assertWithDiagnostics(() ->
            AssertionErrors.assertEquals("Response status reason", reason, actual));
    return this.responseSpec;
  }

  private static String getReasonPhrase(HttpStatusCode statusCode) {
    if (statusCode instanceof HttpStatus status) {
      return status.getReasonPhrase();
    }
    else {
      return "";
    }
  }

  /**
   * Assert the response status code is in the 1xx range.
   */
  public WebTestClient.ResponseSpec is1xxInformational() {
    return assertSeriesAndReturn(HttpStatus.Series.INFORMATIONAL);
  }

  /**
   * Assert the response status code is in the 2xx range.
   */
  public WebTestClient.ResponseSpec is2xxSuccessful() {
    return assertSeriesAndReturn(HttpStatus.Series.SUCCESSFUL);
  }

  /**
   * Assert the response status code is in the 3xx range.
   */
  public WebTestClient.ResponseSpec is3xxRedirection() {
    return assertSeriesAndReturn(HttpStatus.Series.REDIRECTION);
  }

  /**
   * Assert the response status code is in the 4xx range.
   */
  public WebTestClient.ResponseSpec is4xxClientError() {
    return assertSeriesAndReturn(HttpStatus.Series.CLIENT_ERROR);
  }

  /**
   * Assert the response status code is in the 5xx range.
   */
  public WebTestClient.ResponseSpec is5xxServerError() {
    return assertSeriesAndReturn(HttpStatus.Series.SERVER_ERROR);
  }

  /**
   * Match the response status value with a Hamcrest matcher.
   *
   * @param matcher the matcher to use
   */
  public WebTestClient.ResponseSpec value(Matcher<? super Integer> matcher) {
    int actual = this.exchangeResult.getStatus().value();
    this.exchangeResult.assertWithDiagnostics(() -> MatcherAssert.assertThat("Response status", actual, matcher));
    return this.responseSpec;
  }

  /**
   * Consume the response status value as an integer.
   *
   * @param consumer the consumer to use
   * @since 4.0
   */
  public WebTestClient.ResponseSpec value(Consumer<Integer> consumer) {
    int actual = this.exchangeResult.getStatus().value();
    this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(actual));
    return this.responseSpec;
  }

  private WebTestClient.ResponseSpec assertStatusAndReturn(HttpStatusCode expected) {
    HttpStatusCode actual = this.exchangeResult.getStatus();
    this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", expected, actual));
    return this.responseSpec;
  }

  private WebTestClient.ResponseSpec assertSeriesAndReturn(HttpStatus.Series expected) {
    HttpStatusCode status = this.exchangeResult.getStatus();
    HttpStatus.Series series = HttpStatus.Series.resolve(status.value());
    this.exchangeResult.assertWithDiagnostics(() ->
            AssertionErrors.assertEquals("Range for response status value " + status, expected, series));
    return this.responseSpec;
  }

}
