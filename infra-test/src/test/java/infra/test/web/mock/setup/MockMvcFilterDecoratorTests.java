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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import infra.web.mock.api.DispatcherType;
import infra.web.mock.MockResponse;
import infra.web.mock.MockRequest;
import infra.web.mock.MockFilterChain;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 17:49
 */
class MockMvcFilterDecoratorTests {

  private MockRequest request;

  private MockResponse response;

  private MockFilterChain filterChain;

  private MockFilter delegate;

  private MockMvcFilterDecorator filter;

  @BeforeEach
  public void setup() {
    request = new MockRequest();
    response = new MockResponse();
    filterChain = new MockFilterChain();
    delegate = new MockFilter();
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
    filter = new MockMvcFilterDecorator(delegate, EnumSet.of(filterDispatcherType), pattern);
    filter.doFilter(new MockRequestContext(request, response), filterChain);

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
    filter.doFilter(new MockRequestContext(request, response), filterChain);

    assertThat(delegate.request).isEqualTo(request);
    assertThat(delegate.response).isEqualTo(response);
    assertThat(delegate.chain).isEqualTo(filterChain);
    delegate = new MockFilter();
  }

  private static class MockFilter implements Filter {

    private MockRequest request;

    private MockResponse response;

    private FilterChain chain;

    @Override
    public void doFilter(RequestContext request, FilterChain chain) throws Exception {
      this.request = MockUtils.getMockRequest(request);
      this.response = MockUtils.getMockResponse(request);
      this.chain = chain;
    }

  }

}