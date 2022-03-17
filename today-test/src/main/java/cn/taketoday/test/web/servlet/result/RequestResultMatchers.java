/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.concurrent.Callable;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.web.context.request.async.DeferredResult;
import cn.taketoday.web.context.request.async.WebAsyncTask;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertFalse;
import static cn.taketoday.test.util.AssertionErrors.assertNull;
import static cn.taketoday.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for assertions on the request.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#request}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class RequestResultMatchers {

  /**
   * Protected constructor.
   * <p>Use {@link MockMvcResultMatchers#request()}.
   */
  protected RequestResultMatchers() {
  }

  /**
   * Assert whether asynchronous processing started, usually as a result of a
   * controller method returning {@link Callable} or {@link DeferredResult}.
   * <p>The test will await the completion of a {@code Callable} so that
   * {@link #asyncResult(Matcher)} or {@link #asyncResult(Object)} can be used
   * to assert the resulting value.
   * <p>Neither a {@code Callable} nor a {@code DeferredResult} will complete
   * processing all the way since a {@link MockHttpServletRequest} does not
   * perform asynchronous dispatches.
   *
   * @see #asyncNotStarted()
   */
  public ResultMatcher asyncStarted() {
    return result -> assertAsyncStarted(result.getRequest());
  }

  /**
   * Assert that asynchronous processing was not started.
   *
   * @see #asyncStarted()
   */
  public ResultMatcher asyncNotStarted() {
    return result -> assertFalse("Async started", result.getRequest().isAsyncStarted());
  }

  /**
   * Assert the result from asynchronous processing with the given matcher.
   * <p>This method can be used when a controller method returns {@link Callable}
   * or {@link WebAsyncTask}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher asyncResult(Matcher<? super T> matcher) {
    return result -> {
      HttpServletRequest request = result.getRequest();
      assertAsyncStarted(request);
      assertThat("Async result", (T) result.getAsyncResult(), matcher);
    };
  }

  /**
   * Assert the result from asynchronous processing.
   * <p>This method can be used when a controller method returns {@link Callable}
   * or {@link WebAsyncTask}. The value matched is the value returned from the
   * {@code Callable} or the exception raised.
   */
  public ResultMatcher asyncResult(@Nullable Object expectedResult) {
    return result -> {
      HttpServletRequest request = result.getRequest();
      assertAsyncStarted(request);
      assertEquals("Async result", expectedResult, result.getAsyncResult());
    };
  }

  /**
   * Assert a request attribute value with the given Hamcrest {@link Matcher}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
    return result -> {
      T value = (T) result.getRequest().getAttribute(name);
      assertThat("Request attribute '" + name + "'", value, matcher);
    };
  }

  /**
   * Assert a request attribute value.
   */
  public ResultMatcher attribute(String name, @Nullable Object expectedValue) {
    return result ->
            assertEquals("Request attribute '" + name + "'", expectedValue, result.getRequest().getAttribute(name));
  }

  /**
   * Assert a session attribute value with the given Hamcrest {@link Matcher}.
   */
  @SuppressWarnings("unchecked")
  public <T> ResultMatcher sessionAttribute(String name, Matcher<? super T> matcher) {
    return result -> {
      HttpSession session = result.getRequest().getSession();
      Assert.state(session != null, "No HttpSession");
      T value = (T) session.getAttribute(name);
      assertThat("Session attribute '" + name + "'", value, matcher);
    };
  }

  /**
   * Assert a session attribute value.
   */
  public ResultMatcher sessionAttribute(String name, @Nullable Object value) {
    return result -> {
      HttpSession session = result.getRequest().getSession();
      Assert.state(session != null, "No HttpSession");
      assertEquals("Session attribute '" + name + "'", value, session.getAttribute(name));
    };
  }

  /**
   * Assert the given session attributes do not exist.
   *
   * @since 4.0
   */
  public ResultMatcher sessionAttributeDoesNotExist(String... names) {
    return result -> {
      HttpSession session = result.getRequest().getSession();
      Assert.state(session != null, "No HttpSession");
      for (String name : names) {
        assertNull("Session attribute '" + name + "' exists", session.getAttribute(name));
      }
    };
  }

  private static void assertAsyncStarted(HttpServletRequest request) {
    assertTrue("Async not started", request.isAsyncStarted());
  }

}
