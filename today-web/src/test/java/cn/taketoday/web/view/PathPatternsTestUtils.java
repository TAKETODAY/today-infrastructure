/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.view;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.MockServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

public abstract class PathPatternsTestUtils {

  public static Stream<Function<String, MockServletRequestContext>> requestArguments() {
    return requestArguments(null);
  }

  public static Stream<Function<String, MockServletRequestContext>> requestArguments(@Nullable String contextPath) {
    return Stream.of(
            path -> createRequest("GET", contextPath, path)
    );
  }

  public static MockServletRequestContext createRequest(String method, @Nullable String contextPath, String path) {
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.refresh();
    if (contextPath != null) {
      String requestUri = contextPath + (path.startsWith("/") ? "" : "/") + path;

      MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, requestUri);
      servletRequest.setContextPath(contextPath);
      return new MockServletRequestContext(context, servletRequest, new MockHttpServletResponse());
    }
    else {
      MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, path);
      return new MockServletRequestContext(context, servletRequest, new MockHttpServletResponse());
    }
  }

  public static MockHttpServletRequest initRequest(String method, String requestUri) {
    return initRequest(method, null, requestUri, true);
  }

  public static MockHttpServletRequest initRequest(String method, String requestUri, boolean parsedPatterns) {
    return initRequest(method, null, requestUri, parsedPatterns);
  }

  /**
   * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath.
   */
  public static MockHttpServletRequest initRequest(
          String method, @Nullable String contextPath, String path) {

    return initRequest(method, contextPath, path, true, null);
  }

  /**
   * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath.
   */
  public static MockHttpServletRequest initRequest(
          String method, @Nullable String contextPath, String path, boolean parsedPatterns) {

    return initRequest(method, contextPath, path, parsedPatterns, null);
  }

  /**
   * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath
   * and a post-construct initializer to apply further changes before the
   * lookupPath is resolved.
   */
  public static MockHttpServletRequest initRequest(
          String method, @Nullable String contextPath, String path,
          boolean parsedPatterns, @Nullable Consumer<MockHttpServletRequest> postConstructInitializer) {

    MockHttpServletRequest request = createRequest0(method, contextPath, path);
    if (postConstructInitializer != null) {
      postConstructInitializer.accept(request);
    }

    return request;
  }

  private static MockHttpServletRequest createRequest0(String method, @Nullable String contextPath, String path) {
    if (contextPath != null) {
      String requestUri = contextPath + (path.startsWith("/") ? "" : "/") + path;
      MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
      request.setContextPath(contextPath);
      return request;
    }
    else {
      return new MockHttpServletRequest(method, path);
    }
  }

}

