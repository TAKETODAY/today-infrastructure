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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import infra.mock.api.DispatcherType;
import infra.mock.api.Filter;
import infra.mock.api.FilterChain;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockFilterChain;
import infra.mock.web.MockFilterConfig;
import infra.mock.web.MockHttpResponseImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 17:49
 */
class MockMvcFilterDecoratorTests {

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  private MockFilterChain filterChain;

  private MockFilter delegate;

  private MockMvcFilterDecorator filter;

  @BeforeEach
  public void setup() {
    request = new HttpMockRequestImpl();
    response = new MockHttpResponseImpl();
    filterChain = new MockFilterChain();
    delegate = new MockFilter();
  }

  @Test
  public void init() throws Exception {
    FilterConfig config = new MockFilterConfig();
    filter = new MockMvcFilterDecorator(delegate, new String[] { "/" });
    filter.init(config);
    assertThat(delegate.filterConfig).isEqualTo(config);
  }

  @Test
  public void destroy() {
    filter = new MockMvcFilterDecorator(delegate, new String[] { "/" });
    filter.destroy();
    assertThat(delegate.destroy).isTrue();
  }

  @Test
  public void matchExact() throws Exception {
    assertFilterInvoked("/test", "/test");
  }

  @Test
  public void matchPathMappingAllFolder() throws Exception {
    assertFilterInvoked("/test/this", "*");
    assertFilterInvoked("/test/this", "/*");
  }

  @Test
  public void matchPathMappingAll() throws Exception {
    assertFilterInvoked("/test", "*");
    assertFilterInvoked("/test", "/*");
  }

  @Test
  public void matchPathMappingContextRootAndSlash() throws Exception {
    assertFilterInvoked("/", "*");
    assertFilterInvoked("/", "/*");
  }

  @Test
  public void matchPathMappingFolderPatternWithMultiFolderPath() throws Exception {
    assertFilterInvoked("/test/this/here", "/test/*");
  }

  @Test
  public void matchPathMappingFolderPattern() throws Exception {
    assertFilterInvoked("/test/this", "/test/*");
  }

  @Test
  public void matchPathMappingNoSuffix() throws Exception {
    assertFilterInvoked("/test/", "/test/*");
  }

  @Test
  public void matchPathMappingMissingSlash() throws Exception {
    assertFilterInvoked("/test", "/test/*");
  }

  @Test
  public void noMatchPathMappingMulti() throws Exception {
    assertFilterNotInvoked("/this/test/here", "/test/*");
  }

  @Test
  public void noMatchPathMappingEnd() throws Exception {
    assertFilterNotInvoked("/this/test", "/test/*");
  }

  @Test
  public void noMatchPathMappingEndSuffix() throws Exception {
    assertFilterNotInvoked("/test2/", "/test/*");
  }

  @Test
  public void noMatchPathMappingMissingSlash() throws Exception {
    assertFilterNotInvoked("/test2", "/test/*");
  }

  @Test
  public void noMatchDispatcherType() throws Exception {
    assertFilterNotInvoked(DispatcherType.FORWARD, DispatcherType.REQUEST, "/test", "/test");
  }

  @Test
  public void matchExtensionMulti() throws Exception {
    assertFilterInvoked("/test/this/here.html", "*.html");
  }

  @Test
  public void matchExtension() throws Exception {
    assertFilterInvoked("/test/this.html", "*.html");
  }

  @Test
  public void matchExtensionNoPrefix() throws Exception {
    assertFilterInvoked("/.html", "*.html");
  }

  @Test
  public void matchExtensionNoFolder() throws Exception {
    assertFilterInvoked("/test.html", "*.html");
  }

  @Test
  public void noMatchExtensionNoSlash() throws Exception {
    assertFilterNotInvoked(".html", "*.html");
  }

  @Test
  public void noMatchExtensionSlashEnd() throws Exception {
    assertFilterNotInvoked("/index.html/", "*.html");
  }

  @Test
  public void noMatchExtensionPeriodEnd() throws Exception {
    assertFilterNotInvoked("/index.html.", "*.html");
  }

  @Test
  public void noMatchExtensionLarger() throws Exception {
    assertFilterNotInvoked("/index.htm", "*.html");
  }

  @Test
  public void noMatchInvalidPattern() throws Exception {
    // pattern uses extension mapping but starts with / (treated as exact match)
    assertFilterNotInvoked("/index.html", "/*.html");
  }

  @Test
  public void specPathMappingMultiFolderPattern() throws Exception {
    assertFilterInvoked("/foo/bar/index.html", "/foo/bar/*");
  }

  @Test
  public void specPathMappingMultiFolderPatternAlternate() throws Exception {
    assertFilterInvoked("/foo/bar/index.bop", "/foo/bar/*");
  }

  @Test
  public void specPathMappingNoSlash() throws Exception {
    assertFilterInvoked("/baz", "/baz/*");
  }

  @Test
  public void specPathMapping() throws Exception {
    assertFilterInvoked("/baz/index.html", "/baz/*");
  }

  @Test
  public void specExactMatch() throws Exception {
    assertFilterInvoked("/catalog", "/catalog");
  }

  @Test
  public void specExtensionMappingSingleFolder() throws Exception {
    assertFilterInvoked("/catalog/racecar.bop", "*.bop");
  }

  @Test
  public void specExtensionMapping() throws Exception {
    assertFilterInvoked("/index.bop", "*.bop");
  }

  private void assertFilterNotInvoked(String requestUri, String pattern) throws Exception {
    assertFilterNotInvoked(DispatcherType.REQUEST, DispatcherType.REQUEST, requestUri, pattern);
  }

  private void assertFilterNotInvoked(
          DispatcherType requestDispatcherType, DispatcherType filterDispatcherType,
          String requestUri, String pattern) throws Exception {

    request.setDispatcherType(requestDispatcherType);
    request.setRequestURI(requestUri);
    filter = new MockMvcFilterDecorator(delegate, null, null, EnumSet.of(filterDispatcherType), pattern);
    filter.doFilter(request, response, filterChain);

    assertThat(delegate.request).isNull();
    assertThat(delegate.response).isNull();
    assertThat(delegate.chain).isNull();

    assertThat(filterChain.getRequest()).isEqualTo(request);
    assertThat(filterChain.getResponse()).isEqualTo(response);
    filterChain = new MockFilterChain();
  }

  private void assertFilterInvoked(String requestUri, String pattern) throws Exception {
    request.setRequestURI(requestUri);
    filter = new MockMvcFilterDecorator(delegate, new String[] { pattern });
    filter.doFilter(request, response, filterChain);

    assertThat(delegate.request).isEqualTo(request);
    assertThat(delegate.response).isEqualTo(response);
    assertThat(delegate.chain).isEqualTo(filterChain);
    delegate = new MockFilter();
  }

  private static class MockFilter implements Filter {

    private FilterConfig filterConfig;

    private MockRequest request;

    private MockResponse response;

    private FilterChain chain;

    private boolean destroy;

    @Override
    public void init(FilterConfig filterConfig) throws MockException {
      this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) {
      this.request = request;
      this.response = response;
      this.chain = chain;
    }

    @Override
    public void destroy() {
      this.destroy = true;
    }
  }

}