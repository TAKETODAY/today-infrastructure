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

package cn.taketoday.web.handler;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.i18n.LocaleChangeInterceptor;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.PathPatternsParameterizedTest;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 23:27
 */
class MappedInterceptorTests {

  private static final LocaleChangeInterceptor delegate = new LocaleChangeInterceptor();

  public static Stream<Named<Function<String, ServletRequestContext>>> requestArguments(@Nullable String contextPath) {
    return Stream.of(
            Named.named("ServletRequestPathUtils",
                    path -> PathPatternsTestUtils.createRequest("GET", contextPath, path)
            )
    );
  }

  @SuppressWarnings("unused")
  private static Stream<Named<Function<String, ServletRequestContext>>> pathPatternsArguments() {
    return requestArguments(null);
  }

  @PathPatternsParameterizedTest
  void noPatterns(Function<String, ServletRequestContext> requestFactory) {
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
  void excludePattern(Function<String, ServletRequestContext> requestFactory) {
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
  void customPathMatcher(Function<String, ServletRequestContext> requestFactory) {
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

    then(delegate).should().beforeProcess(ArgumentMatchers.any(RequestContext.class), ArgumentMatchers.any());
  }

  @Test
  void postHandle() throws Throwable {
    HandlerInterceptor delegate = mock(HandlerInterceptor.class);

    new MappedInterceptor(null, delegate).afterProcess(
            mock(RequestContext.class), null, mock(ModelAndView.class));

    then(delegate).should().afterProcess(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
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
    public List<String> extractVariableNames(String pattern) {
      return null;
    }
  }
}
