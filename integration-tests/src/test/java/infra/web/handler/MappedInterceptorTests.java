/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.http.server.PathContainer;
import infra.http.server.RequestPath;
import infra.util.PathMatcher;
import infra.web.HandlerInterceptor;
import infra.web.InterceptorChain;
import infra.web.RequestContext;
import infra.web.i18n.LocaleChangeInterceptor;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;
import infra.web.view.PathPatternsParameterizedTest;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 23:27
 */
class MappedInterceptorTests {

  private static final LocaleChangeInterceptor delegate = new LocaleChangeInterceptor();

  public static Stream<Named<Function<String, MockRequestContext>>> requestArguments(@Nullable String contextPath) {
    return Stream.of(
            Named.named("ServletRequestPathUtils",
                    path -> PathPatternsTestUtils.createRequest("GET", contextPath, path)
            )
    );
  }

  @SuppressWarnings("unused")
  private static Stream<Named<Function<String, MockRequestContext>>> pathPatternsArguments() {
    return requestArguments(null);
  }

  @PathPatternsParameterizedTest
  void noPatterns(Function<String, MockRequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(null, null, delegate);
    Assertions.assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
  }

  @PathPatternsParameterizedTest
  void includePattern(Function<String, RequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo/*" }, null, delegate);

    assertThat(interceptor.matches(requestFactory.apply("/foo/bar"))).isTrue();
    assertThat(interceptor.matches(requestFactory.apply("/bar/foo"))).isFalse();
  }

  @PathPatternsParameterizedTest
  void includePatternWithMatrixVariables(Function<String, RequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo*/*" }, null, delegate);
    assertThat(interceptor.matches(requestFactory.apply("/foo;q=1/bar;s=2"))).isTrue();
  }

  @PathPatternsParameterizedTest
  void excludePattern(Function<String, MockRequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(null, new String[] { "/admin/**" }, delegate);

    Assertions.assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
    Assertions.assertThat(interceptor.matches(requestFactory.apply("/admin/foo"))).isFalse();
  }

  @PathPatternsParameterizedTest
  void includeAndExcludePatterns(Function<String, RequestContext> requestFactory) {
    MappedInterceptor interceptor =
            new MappedInterceptor(new String[] { "/**" }, new String[] { "/admin/**" }, delegate);

    assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
    assertThat(interceptor.matches(requestFactory.apply("/admin/foo"))).isFalse();
  }

  @PathPatternsParameterizedTest
  void includePatternWithFallbackOnPathMatcher(Function<String, RequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/path1/**/path2" }, null, delegate);

    assertThat(interceptor.matches(requestFactory.apply("/path1/foo/bar/path2"))).isTrue();
    assertThat(interceptor.matches(requestFactory.apply("/path1/foo/bar/path3"))).isFalse();
    assertThat(interceptor.matches(requestFactory.apply("/path3/foo/bar/path2"))).isFalse();
  }

  @Disabled
  @PathPatternsParameterizedTest
  void customPathMatcher(Function<String, MockRequestContext> requestFactory) {
    MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo/[0-9]*" }, null, delegate);
    interceptor.setPathMatcher(new TestPathMatcher());

    Assertions.assertThat(interceptor.matches(requestFactory.apply("/foo/123"))).isTrue();
    Assertions.assertThat(interceptor.matches(requestFactory.apply("/foo/bar"))).isFalse();
  }

  @Test
  void preHandle() throws Throwable {
    HandlerInterceptor delegate = mock(HandlerInterceptor.class);

    new MappedInterceptor(null, delegate).beforeProcess(
            mock(RequestContext.class), null);

    then(delegate).should().beforeProcess(any(RequestContext.class), any());
  }

  @Test
  void postHandle() throws Throwable {
    HandlerInterceptor delegate = mock(HandlerInterceptor.class);

    new MappedInterceptor(null, delegate).afterProcess(
            mock(RequestContext.class), null, mock(ModelAndView.class));

    then(delegate).should().afterProcess(any(), any(), any());
  }

  @Test
  void constructorWithIncludeAndExcludePatterns() {
    String[] includePatterns = { "/api/**", "/admin/**" };
    String[] excludePatterns = { "/admin/exclude/**" };
    HandlerInterceptor interceptor = mock(HandlerInterceptor.class);

    MappedInterceptor mappedInterceptor = new MappedInterceptor(includePatterns, excludePatterns, interceptor);

    assertThat(mappedInterceptor.getInterceptor()).isSameAs(interceptor);
    assertThat(mappedInterceptor.getPathPatterns()).containsExactlyInAnyOrder("/api/**", "/admin/**");
  }

  @Test
  void constructorWithIncludePatternsOnly() {
    String[] includePatterns = { "/api/**" };
    HandlerInterceptor interceptor = mock(HandlerInterceptor.class);

    MappedInterceptor mappedInterceptor = new MappedInterceptor(includePatterns, interceptor);

    assertThat(mappedInterceptor.getInterceptor()).isSameAs(interceptor);
    assertThat(mappedInterceptor.getPathPatterns()).containsExactly("/api/**");
  }

  @Test
  void constructorWithNullPatterns() {
    HandlerInterceptor interceptor = mock(HandlerInterceptor.class);

    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, interceptor);

    assertThat(mappedInterceptor.getInterceptor()).isSameAs(interceptor);
    assertThat(mappedInterceptor.getPathPatterns()).isNull();
  }

  @Test
  void setPathMatcherWithValidMatcher() {
    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, mock(HandlerInterceptor.class));
    PathMatcher pathMatcher = mock(PathMatcher.class);

    mappedInterceptor.setPathMatcher(pathMatcher);

    assertThat(mappedInterceptor.getPathMatcher()).isSameAs(pathMatcher);
  }

  @Test
  void setPathMatcherWithNullThrowsException() {
    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, mock(HandlerInterceptor.class));

    assertThatThrownBy(() -> mappedInterceptor.setPathMatcher(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("pathMatcher is required");
  }

  @Test
  void matchesWithRequestContext() {
    RequestContext request = mock(RequestContext.class);
    given(request.getRequestPath()).willReturn(RequestPath.parse("/test", null));

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/test" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(request);

    assertThat(result).isTrue();
  }

  @Test
  void matchesWithIncludePattern() {
    PathContainer pathContainer = PathContainer.parsePath("/api/users");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/api/**" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isTrue();
  }

  @Test
  void matchesWithExcludePattern() {
    PathContainer pathContainer = PathContainer.parsePath("/admin/users");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, new String[] { "/admin/**" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isFalse();
  }

  @Test
  void matchesWithIncludeAndExcludePatternsMatchingExclude() {
    PathContainer pathContainer = PathContainer.parsePath("/admin/users");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/**" }, new String[] { "/admin/**" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isFalse();
  }

  @Test
  void matchesWithIncludeAndExcludePatternsNotMatchingEither() {
    PathContainer pathContainer = PathContainer.parsePath("/public/info");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/api/**" }, new String[] { "/admin/**" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isFalse();
  }

  @Test
  void matchesWithNoPatterns() {
    PathContainer pathContainer = PathContainer.parsePath("/any/path");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isTrue();
  }

  @Test
  void matchesWithMatrixVariablesInPath() {
    PathContainer pathContainer = PathContainer.parsePath("/api/users;jsessionid=12345");

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/api/**" }, mock(HandlerInterceptor.class));

    boolean result = mappedInterceptor.matches(pathContainer);

    assertThat(result).isTrue();
  }

  @Test
  void beforeProcessDelegatesToInterceptor() throws Throwable {
    HandlerInterceptor delegate = mock(HandlerInterceptor.class);
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    given(delegate.beforeProcess(request, handler)).willReturn(true);

    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, delegate);

    boolean result = mappedInterceptor.beforeProcess(request, handler);

    assertThat(result).isTrue();
    verify(delegate).beforeProcess(request, handler);
  }

  @Test
  void afterProcessDelegatesToInterceptor() throws Throwable {
    HandlerInterceptor delegate = mock(HandlerInterceptor.class);
    RequestContext request = mock(RequestContext.class);
    Object handler = new Object();
    Object result = new Object();

    MappedInterceptor mappedInterceptor = new MappedInterceptor(null, delegate);

    mappedInterceptor.afterProcess(request, handler, result);

    verify(delegate).afterProcess(request, handler, result);
  }

  @Test
  void interceptWhenNotMatchingProceedsWithChain() throws Throwable {
    RequestContext request = mock(RequestContext.class);
    given(request.getRequestPath()).willReturn(RequestPath.parse("/other", null));

    HandlerInterceptor delegate = mock(HandlerInterceptor.class);
    InterceptorChain chain = mock(InterceptorChain.class);
    Object expectedResult = new Object();
    given(chain.proceed(request)).willReturn(expectedResult);

    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/test" }, delegate);

    Object actualResult = mappedInterceptor.intercept(request, chain);

    assertThat(actualResult).isSameAs(expectedResult);
    verify(delegate, never()).intercept(any(), any());
    verify(chain).proceed(request);
  }

  public static class TestPathMatcher implements PathMatcher {

    @Override
    public boolean isPattern(String path) {
      return false;
    }

    @Override
    public boolean match(String pattern, String path) {
      return path.matches(pattern);
    }

    @Override
    public boolean matchStart(String pattern, String path) {
      return false;
    }

    @Override
    public String extractPathWithinPattern(String pattern, String path) {
      return null;
    }

    @Override
    public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
      return null;
    }

    @Override
    public Comparator<String> getPatternComparator(String path) {
      return null;
    }

    @Override
    public String combine(String pattern1, String pattern2) {
      return null;
    }

    @Override
    public String[] extractVariables(String pattern, String path) {
      return new String[0];
    }

    @Override
    public String[] extractVariableNames(String pattern) {
      return null;
    }

  }
}
