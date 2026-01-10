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

package infra.test.web.mock.result;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;

import infra.http.ResponseCookie;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.ResultMatcher;
import infra.web.RequestContext;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.assertNotNull;
import static infra.test.util.AssertionErrors.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for response cookie assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#cookie}.
 *
 * @author Rossen Stoyanchev
 * @author Thomas Bruyelle
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CookieResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#cookie()}.
   */
  protected CookieResultMatchers() { }

  /**
   * Assert a cookie value with the given Hamcrest {@link Matcher}.
   */
  public ResultMatcher value(String name, Matcher<? super String> matcher) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertThat("Response cookie '" + name + "'", cookie.getValue(), matcher);
    };
  }

  /**
   * Assert a cookie value.
   */
  public ResultMatcher value(String name, String expectedValue) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie", expectedValue, cookie.getValue());
    };
  }

  /**
   * Assert a cookie exists. The existence check is irrespective of whether
   * max age is 0 (i.e. expired).
   */
  public ResultMatcher exists(String name) {
    return result -> getCookie(result, name);
  }

  /**
   * Assert a cookie does not exist. Note that the existence check is
   * irrespective of whether max age is 0, i.e. expired.
   */
  public ResultMatcher doesNotExist(String name) {
    return result -> {
      ResponseCookie cookie = getCookie(result.getRequestContext(), name);
      assertNull("Unexpected cookie with name '" + name + "'", cookie);
    };
  }

  /**
   * Assert a cookie's maxAge with a Hamcrest {@link Matcher}.
   */
  public ResultMatcher maxAge(String name, Matcher<? super Duration> matcher) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertThat("Response cookie '" + name + "' maxAge", cookie.getMaxAge(), matcher);
    };
  }

  /**
   * Assert a cookie's maxAge.
   */
  public ResultMatcher maxAge(String name, int maxAge) {
    return maxAge(name, Duration.ofSeconds(maxAge));
  }

  /**
   * Assert a cookie's maxAge.
   */
  public ResultMatcher maxAge(String name, Duration maxAge) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' maxAge", maxAge, cookie.getMaxAge());
    };
  }

  /**
   * Assert a cookie's path with a Hamcrest {@link Matcher}.
   */
  public ResultMatcher path(String name, Matcher<? super String> matcher) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertThat("Response cookie '" + name + "' path", cookie.getPath(), matcher);
    };
  }

  /**
   * Assert a cookie's path.
   */
  public ResultMatcher path(String name, String path) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' path", path, cookie.getPath());
    };
  }

  /**
   * Assert a cookie's domain with a Hamcrest {@link Matcher}.
   */
  public ResultMatcher domain(String name, Matcher<? super String> matcher) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertThat("Response cookie '" + name + "' domain", cookie.getDomain(), matcher);
    };
  }

  /**
   * Assert a cookie's domain.
   */
  public ResultMatcher domain(String name, String domain) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' domain", domain, cookie.getDomain());
    };
  }

  /**
   * Assert a cookie's SameSite attribute with a Hamcrest {@link Matcher}.
   */
  public ResultMatcher sameSite(String name, Matcher<? super String> matcher) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertThat("Response cookie '" + name + "' SameSite", cookie.getSameSite(), matcher);
    };

  }

  /**
   * Assert a cookie's SameSite attribute.
   */
  public ResultMatcher sameSite(String name, String sameSite) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' SameSite", sameSite, cookie.getSameSite());
    };
  }

  /**
   * Assert whether the cookie must be sent over a secure protocol or not.
   */
  public ResultMatcher secure(String name, boolean secure) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' secure", secure, cookie.isSecure());
    };
  }

  /**
   * Assert whether the cookie must be HTTP only.
   */
  public ResultMatcher httpOnly(String name, boolean httpOnly) {
    return result -> {
      ResponseCookie cookie = getCookie(result, name);
      assertEquals("Response cookie '" + name + "' httpOnly", httpOnly, cookie.isHttpOnly());
    };
  }

  private static ResponseCookie getCookie(MvcResult result, String cookieName) {
    RequestContext context = result.getRequestContext();
    ResponseCookie cookie = getCookie(context, cookieName);
    assertNotNull("No cookie with name '" + cookieName + "'", cookie);
    return cookie;
  }

  @Nullable
  private static ResponseCookie getCookie(RequestContext context, String cookieName) {
    ArrayList<ResponseCookie> httpCookies = context.responseCookies();
    for (ResponseCookie httpCookie : httpCookies) {
      if (cookieName.equals(httpCookie.getName())) {
        return httpCookie;
      }
    }
    return null;
  }

}
