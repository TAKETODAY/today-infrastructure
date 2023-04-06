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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matcher;

import java.time.Duration;
import java.util.ArrayList;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.web.RequestContext;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertNotNull;
import static cn.taketoday.test.util.AssertionErrors.assertNull;
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
    ArrayList<HttpCookie> httpCookies = context.responseCookies();
    for (HttpCookie httpCookie : httpCookies) {
      if (cookieName.equals(httpCookie.getName())) {
        if (httpCookie instanceof ResponseCookie responseCookie) {
          return responseCookie;
        }
        else {
          return ResponseCookie.from(cookieName, httpCookie.getValue())
                  .build();
        }
      }
    }
    return null;
  }

}
