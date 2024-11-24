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

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import infra.http.ResponseCookie;
import infra.test.util.AssertionErrors;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Assertions on cookies of the response.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CookieAssertions {

  private final ExchangeResult exchangeResult;
  private final WebTestClient.ResponseSpec responseSpec;

  public CookieAssertions(ExchangeResult exchangeResult, WebTestClient.ResponseSpec responseSpec) {
    this.exchangeResult = exchangeResult;
    this.responseSpec = responseSpec;
  }

  /**
   * Expect a header with the given name to match the specified values.
   */
  public WebTestClient.ResponseSpec valueEquals(String name, String value) {
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name);
      AssertionErrors.assertEquals(message, value, getCookie(name).getValue());
    });
    return this.responseSpec;
  }

  /**
   * Assert the first value of the response cookie with a Hamcrest {@link Matcher}.
   */
  public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
    String value = getCookie(name).getValue();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name);
      MatcherAssert.assertThat(message, value, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Consume the value of the response cookie.
   */
  public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
    String value = getCookie(name).getValue();
    this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
    return this.responseSpec;
  }

  /**
   * Expect that the cookie with the given name is present.
   */
  public WebTestClient.ResponseSpec exists(String name) {
    getCookie(name);
    return this.responseSpec;
  }

  /**
   * Expect that the cookie with the given name is not present.
   */
  public WebTestClient.ResponseSpec doesNotExist(String name) {
    ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
    if (cookie != null) {
      String message = getMessage(name) + " exists with value=[" + cookie.getValue() + "]";
      this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
    }
    return this.responseSpec;
  }

  /**
   * Assert a cookie's maxAge attribute.
   */
  public WebTestClient.ResponseSpec maxAge(String name, Duration expected) {
    Duration maxAge = getCookie(name).getMaxAge();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " maxAge";
      AssertionErrors.assertEquals(message, expected, maxAge);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's maxAge attribute with a Hamcrest {@link Matcher}.
   */
  public WebTestClient.ResponseSpec maxAge(String name, Matcher<? super Long> matcher) {
    long maxAge = getCookie(name).getMaxAge().getSeconds();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " maxAge";
      assertThat(message, maxAge, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's path attribute.
   */
  public WebTestClient.ResponseSpec path(String name, String expected) {
    String path = getCookie(name).getPath();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " path";
      AssertionErrors.assertEquals(message, expected, path);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's path attribute with a Hamcrest {@link Matcher}.
   */
  public WebTestClient.ResponseSpec path(String name, Matcher<? super String> matcher) {
    String path = getCookie(name).getPath();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " path";
      assertThat(message, path, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's domain attribute.
   */
  public WebTestClient.ResponseSpec domain(String name, String expected) {
    String path = getCookie(name).getDomain();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " domain";
      AssertionErrors.assertEquals(message, expected, path);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's domain attribute with a Hamcrest {@link Matcher}.
   */
  public WebTestClient.ResponseSpec domain(String name, Matcher<? super String> matcher) {
    String domain = getCookie(name).getDomain();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " domain";
      assertThat(message, domain, matcher);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's secure attribute.
   */
  public WebTestClient.ResponseSpec secure(String name, boolean expected) {
    boolean isSecure = getCookie(name).isSecure();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " secure";
      AssertionErrors.assertEquals(message, expected, isSecure);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's httpOnly attribute.
   */
  public WebTestClient.ResponseSpec httpOnly(String name, boolean expected) {
    boolean isHttpOnly = getCookie(name).isHttpOnly();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " httpOnly";
      AssertionErrors.assertEquals(message, expected, isHttpOnly);
    });
    return this.responseSpec;
  }

  /**
   * Assert a cookie's sameSite attribute.
   */
  public WebTestClient.ResponseSpec sameSite(String name, String expected) {
    String sameSite = getCookie(name).getSameSite();
    this.exchangeResult.assertWithDiagnostics(() -> {
      String message = getMessage(name) + " sameSite";
      AssertionErrors.assertEquals(message, expected, sameSite);
    });
    return this.responseSpec;
  }

  private ResponseCookie getCookie(String name) {
    ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
    if (cookie == null) {
      this.exchangeResult.assertWithDiagnostics(() ->
              AssertionErrors.fail("No cookie with name '" + name + "'"));
    }
    return Objects.requireNonNull(cookie);
  }

  private String getMessage(String cookie) {
    return "Response cookie '" + cookie + "'";
  }

}
