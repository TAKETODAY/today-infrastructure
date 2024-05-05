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

package cn.taketoday.test.web.mock.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.mock.api.DispatcherType;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.FilterConfig;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpMockRequest;

/**
 * A Filter that invokes a delegate {@link Filter} only if the request URL
 * matches the pattern it is mapped to using pattern matching as defined in the
 * Servlet spec.
 *
 * @author Rob Winch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class MockMvcFilterDecorator implements Filter {

  private static final String ALL_MAPPING_PATTERN = "*";

  private static final String EXTENSION_MAPPING_PATTERN = "*.";

  private static final String PATH_MAPPING_PATTERN = "/*";

  private final Filter delegate;

  @Nullable
  private final Function<MockContext, FilterConfig> filterConfigInitializer;

  @Nullable
  private final EnumSet<DispatcherType> dispatcherTypes;

  private final boolean hasPatterns;

  /** Patterns that require an exact match, e.g. "/test" */
  private final List<String> exactMatches = new ArrayList<>();

  /** Patterns that require the URL to have a specific prefix, e.g. "/test/*" */
  private final List<String> startsWithMatches = new ArrayList<>();

  /** Patterns that require the request URL to have a specific suffix, e.g. "*.html" */
  private final List<String> endsWithMatches = new ArrayList<>();

  /**
   * Create instance with URL patterns only.
   * <p>Note: when this constructor is used, the Filter is not initialized.
   */
  public MockMvcFilterDecorator(Filter delegate, String[] urlPatterns) {
    Assert.notNull(delegate, "filter is required");
    Assert.notNull(urlPatterns, "urlPatterns is required");
    this.delegate = delegate;
    this.filterConfigInitializer = null;
    this.dispatcherTypes = null;
    this.hasPatterns = initPatterns(urlPatterns);
  }

  /**
   * Create instance with init parameters to initialize the filter with,
   * as well as dispatcher types and URL patterns to match.
   */
  public MockMvcFilterDecorator(Filter delegate, @Nullable String filterName,
          @Nullable Map<String, String> initParams, @Nullable EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns) {
    Assert.notNull(delegate, "filter is required");
    Assert.notNull(urlPatterns, "urlPatterns is required");
    this.delegate = delegate;
    this.filterConfigInitializer = getFilterConfigInitializer(filterName, initParams);
    this.dispatcherTypes = dispatcherTypes;
    this.hasPatterns = initPatterns(urlPatterns);
  }

  private static Function<MockContext, FilterConfig> getFilterConfigInitializer(
          @Nullable String filterName, @Nullable Map<String, String> initParams) {

    return mockContext -> {
      MockFilterConfig filterConfig = (filterName != null ? new MockFilterConfig(mockContext, filterName) : new MockFilterConfig(mockContext));
      if (initParams != null) {
        initParams.forEach(filterConfig::addInitParameter);
      }
      return filterConfig;
    };
  }

  private boolean initPatterns(String... urlPatterns) {
    for (String urlPattern : urlPatterns) {
      Assert.notNull(urlPattern, "Found null URL Pattern");
      if (urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
        this.endsWithMatches.add(urlPattern.substring(1));
      }
      else if (urlPattern.equals(PATH_MAPPING_PATTERN) || urlPattern.equals(ALL_MAPPING_PATTERN)) {
        this.startsWithMatches.add("");
      }
      else if (urlPattern.endsWith(PATH_MAPPING_PATTERN)) {
        this.startsWithMatches.add(urlPattern.substring(0, urlPattern.length() - 1));
        this.exactMatches.add(urlPattern.substring(0, urlPattern.length() - 2));
      }
      else {
        if (urlPattern.isEmpty()) {
          urlPattern = "/";
        }
        this.exactMatches.add(urlPattern);
      }
    }
    return (urlPatterns.length != 0);
  }

  @Override
  public void doFilter(MockRequest request, MockResponse response, FilterChain filterChain)
          throws IOException, ServletException {

    HttpMockRequest httpRequest = (HttpMockRequest) request;
    String requestPath = httpRequest.getRequestURI();

    if (matchDispatcherType(httpRequest.getDispatcherType()) && matchRequestPath(requestPath)) {
      this.delegate.doFilter(request, response, filterChain);
    }
    else {
      filterChain.doFilter(request, response);
    }
  }

  private boolean matchDispatcherType(DispatcherType dispatcherType) {
    return (this.dispatcherTypes == null ||
            this.dispatcherTypes.stream().anyMatch(type -> type == dispatcherType));
  }

  private boolean matchRequestPath(String requestPath) {
    if (!this.hasPatterns) {
      return true;
    }
    for (String pattern : this.exactMatches) {
      if (pattern.equals(requestPath)) {
        return true;
      }
    }
    if (!requestPath.startsWith("/")) {
      return false;
    }
    for (String pattern : this.endsWithMatches) {
      if (requestPath.endsWith(pattern)) {
        return true;
      }
    }
    for (String pattern : this.startsWithMatches) {
      if (requestPath.startsWith(pattern)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.delegate.init(filterConfig);
  }

  @Override
  public void destroy() {
    this.delegate.destroy();
  }

  public void initIfRequired(@Nullable MockContext mockContext) throws ServletException {
    if (this.filterConfigInitializer != null) {
      FilterConfig filterConfig = this.filterConfigInitializer.apply(mockContext);
      this.delegate.init(filterConfig);
    }
  }

}
