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

package cn.taketoday.test.web.servlet.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.web.servlet.UrlPathHelper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

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

  private static final String EXTENSION_MAPPING_PATTERN = "*.";

  private static final String PATH_MAPPING_PATTERN = "/*";

  private final Filter delegate;

  @Nullable
  private final Map<String, String> initParams;

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
    this(delegate, null, null, urlPatterns);
  }

  /**
   * Create instance with init parameters to initialize the filter with,
   * as well as dispatcher types and URL patterns to match.
   */
  public MockMvcFilterDecorator(
          Filter delegate, @Nullable Map<String, String> initParams,
          @Nullable EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns) {

    Assert.notNull(delegate, "filter cannot be null");
    Assert.notNull(urlPatterns, "urlPatterns cannot be null");
    this.delegate = delegate;
    this.initParams = initParams;
    this.dispatcherTypes = dispatcherTypes;
    this.hasPatterns = (urlPatterns.length != 0);
    for (String urlPattern : urlPatterns) {
      addUrlPattern(urlPattern);
    }
  }

  private void addUrlPattern(String urlPattern) {
    Assert.notNull(urlPattern, "Found null URL Pattern");
    if (urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
      this.endsWithMatches.add(urlPattern.substring(1));
    }
    else if (urlPattern.equals(PATH_MAPPING_PATTERN)) {
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

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
          throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String requestPath = UrlPathHelper.defaultInstance.getPathWithinApplication(httpRequest);

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

  public void initIfRequired(@Nullable ServletContext servletContext) throws ServletException {
    if (this.initParams != null) {
      MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
      this.initParams.forEach(filterConfig::addInitParameter);
      this.delegate.init(filterConfig);
    }
  }

}
