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

package infra.web.view;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

public abstract class PathPatternsTestUtils {

  public static Stream<Function<String, MockRequestContext>> requestArguments() {
    return requestArguments(null);
  }

  public static Stream<Function<String, MockRequestContext>> requestArguments(@Nullable String contextPath) {
    return Stream.of(
            path -> createRequest("GET", contextPath, path)
    );
  }

  public static MockRequestContext createRequest(String method, @Nullable String contextPath, String path) {
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.refresh();
    if (contextPath != null) {
      String requestUri = contextPath + (path.startsWith("/") ? "" : "/") + path;

      HttpMockRequestImpl servletRequest = new HttpMockRequestImpl(method, requestUri);
      return new MockRequestContext(context, servletRequest, new MockHttpResponseImpl());
    }
    else {
      HttpMockRequestImpl servletRequest = new HttpMockRequestImpl(method, path);
      return new MockRequestContext(context, servletRequest, new MockHttpResponseImpl());
    }
  }

  public static HttpMockRequestImpl initRequest(String method, String requestUri) {
    return initRequest(method, null, requestUri, true);
  }

  public static HttpMockRequestImpl initRequest(String method, String requestUri, boolean parsedPatterns) {
    return initRequest(method, null, requestUri, parsedPatterns);
  }

  /**
   * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath.
   */
  public static HttpMockRequestImpl initRequest(
          String method, @Nullable String contextPath, String path, boolean parsedPatterns) {

    return initRequest(method, contextPath, path, parsedPatterns, null);
  }

  /**
   * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath
   * and a post-construct initializer to apply further changes before the
   * lookupPath is resolved.
   */
  public static HttpMockRequestImpl initRequest(
          String method, @Nullable String contextPath, String path,
          boolean parsedPatterns, @Nullable Consumer<HttpMockRequestImpl> postConstructInitializer) {

    HttpMockRequestImpl request = createRequest0(method, contextPath, path);
    if (postConstructInitializer != null) {
      postConstructInitializer.accept(request);
    }

    return request;
  }

  private static HttpMockRequestImpl createRequest0(String method, @Nullable String contextPath, String path) {
    if (contextPath != null) {
      String requestUri = contextPath + (path.startsWith("/") ? "" : "/") + path;
      return new HttpMockRequestImpl(method, requestUri);
    }
    else {
      return new HttpMockRequestImpl(method, path);
    }
  }

}

