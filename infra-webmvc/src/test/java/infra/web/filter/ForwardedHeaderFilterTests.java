package infra.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.util.Enumeration;

import infra.http.HttpStatus;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.mock.MockFilterChain;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.api.DispatcherType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/7/25 10:01
 */
class ForwardedHeaderFilterTests {

  private static final String FORWARDED = "forwarded";

  private static final String X_FORWARDED_PROTO = "x-forwarded-proto";

  private static final String X_FORWARDED_HOST = "x-forwarded-host";

  private static final String X_FORWARDED_PORT = "x-forwarded-port";

  private static final String X_FORWARDED_SSL = "x-forwarded-ssl";

  private static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";

  private static final String X_FORWARDED_FOR = "x-forwarded-for";

  private final MockFilterChain filterChain = new MockFilterChain();

  private MockRequest request;

  @BeforeEach
  void setup() {
    this.request = new MockRequest();
    this.request.setScheme("http");
    this.request.setServerName("localhost");
    this.request.setServerPort(80);
  }

  @Test
  void shouldFilter() {
    testShouldFilter(FORWARDED);
    testShouldFilter(X_FORWARDED_HOST);
    testShouldFilter(X_FORWARDED_PORT);
    testShouldFilter(X_FORWARDED_PROTO);
    testShouldFilter(X_FORWARDED_SSL);
    testShouldFilter(X_FORWARDED_PREFIX);
    testShouldFilter(X_FORWARDED_FOR);
  }

  private void testShouldFilter(String headerName) {
    MockRequest request = new MockRequest();
    request.addHeader(headerName, "1");
    assertThat(new ForwardedHeaderFilter(false).shouldNotFilter(new MockHttpContext(request))).isFalse();
  }

  @Test
  void shouldNotFilter() {
    assertThat(new ForwardedHeaderFilter(false).shouldNotFilter(new MockHttpContext(request))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "https", "wss" })
  void forwardedRequest(String protocol) throws Exception {
    this.request.setRequestURI("/mvc-showcase");
    this.request.addHeader(X_FORWARDED_PROTO, protocol);
    this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
    this.request.addHeader(X_FORWARDED_PORT, "443");
    this.request.addHeader("foo", "bar");
    this.request.addHeader(X_FORWARDED_FOR, "[203.0.113.195]");

    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(false);
    filter.doFilter(new MockHttpContext(request), this.filterChain);
    HttpContext actual = this.filterChain.getContext();

    assertThat(actual).isNotNull();
    assertThat(actual.getRequestURL().toString()).isEqualTo(protocol + "://84.198.58.199/mvc-showcase");
    assertThat(actual.getScheme()).isEqualTo(protocol);
    assertThat(actual.getServerName()).isEqualTo("84.198.58.199");
    assertThat(actual.getServerPort()).isEqualTo(443);
    assertThat(actual.isSecure()).isTrue();

    assertThat(actual.getHeader(X_FORWARDED_PROTO)).isNull();
    assertThat(actual.getHeader(X_FORWARDED_HOST)).isNull();
    assertThat(actual.getHeader(X_FORWARDED_PORT)).isNull();
    assertThat(actual.getHeader(X_FORWARDED_FOR)).isNull();
    assertThat(actual.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  void forwardedRequestInRemoveOnlyMode() throws Exception {
    this.request.setRequestURI("/mvc-showcase");
    this.request.addHeader(X_FORWARDED_PROTO, "https");
    this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
    this.request.addHeader(X_FORWARDED_PORT, "443");
    this.request.addHeader(X_FORWARDED_SSL, "on");
    this.request.addHeader("foo", "bar");
    this.request.addHeader(X_FORWARDED_FOR, "203.0.113.195");

    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(false);
    filter.setRemoveOnly(true);
    filter.doFilter(new MockHttpContext(request), this.filterChain);
    MockRequest actual = this.filterChain.getRequest();

    HttpContext context = filterChain.getContext();

    assertThat(actual).isNotNull();
    assertThat(context.getRequestURL()).isEqualTo("http://localhost/mvc-showcase");
    assertThat(context.getScheme()).isEqualTo("http");
    assertThat(context.getServerName()).isEqualTo("localhost");
    assertThat(context.getServerPort()).isEqualTo(80);
    assertThat(context.isSecure()).isFalse();
    assertThat(context.getRemoteAddress()).isEqualTo(MockRequest.DEFAULT_REMOTE_ADDR);

    assertThat(context.getHeader(X_FORWARDED_PROTO)).isNull();
    assertThat(context.getHeader(X_FORWARDED_HOST)).isNull();
    assertThat(context.getHeader(X_FORWARDED_PORT)).isNull();
    assertThat(context.getHeader(X_FORWARDED_SSL)).isNull();
    assertThat(context.getHeader(X_FORWARDED_FOR)).isNull();
    assertThat(context.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  void forwardedRequestWithSsl() throws Exception {
    this.request.setRequestURI("/mvc-showcase");
    this.request.addHeader(X_FORWARDED_SSL, "on");
    this.request.addHeader(X_FORWARDED_HOST, "84.198.58.199");
    this.request.addHeader(X_FORWARDED_PORT, "443");
    this.request.addHeader("foo", "bar");

    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(false);
    filter.doFilter(new MockHttpContext(request), this.filterChain);
    MockRequest actual = this.filterChain.getRequest();

    HttpContext context = filterChain.getContext();
    assertThat(actual).isNotNull();
    assertThat(context.getRequestURL()).isEqualTo("https://84.198.58.199/mvc-showcase");
    assertThat(context.getScheme()).isEqualTo("https");
    assertThat(context.getServerName()).isEqualTo("84.198.58.199");
    assertThat(context.getServerPort()).isEqualTo(443);
    assertThat(context.isSecure()).isTrue();

    assertThat(context.getHeader(X_FORWARDED_SSL)).isNull();
    assertThat(context.getHeader(X_FORWARDED_HOST)).isNull();
    assertThat(context.getHeader(X_FORWARDED_PORT)).isNull();
    assertThat(context.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  void forwardedRequestWithForwardDispatch() throws Exception {
    this.request.setRequestURI("/foo");
    this.request.addHeader(X_FORWARDED_PROTO, "https");
    this.request.addHeader(X_FORWARDED_HOST, "www.mycompany.example");
    this.request.addHeader(X_FORWARDED_PORT, "443");

    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(false);
    filter.doFilter(new MockHttpContext(request), this.filterChain);
    MockRequest wrappedRequest = this.filterChain.getRequest();

    this.request.setDispatcherType(DispatcherType.FORWARD);
    this.request.setRequestURI("/bar");
    this.filterChain.reset();

    filter.doFilter(new MockHttpContext(wrappedRequest), this.filterChain);
    MockRequest actual = this.filterChain.getRequest();

    HttpContext context = filterChain.getContext();

    assertThat(actual).isNotNull();
    assertThat(context.getRequestURI()).isEqualTo("/bar");
    assertThat(context.getRequestURL()).isEqualTo("https://www.mycompany.example/bar");
  }

  @Nested
  class InvalidRequests {

    @Test
    void shouldRejectInvalidForwardedForIpv4() throws Exception {
      request.addHeader(FORWARDED, "for=127.0.0.1:");

      MockResponse response = new MockResponse();
      new ForwardedHeaderFilter(true).doFilter(new MockHttpContext(request, response), filterChain);
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldRejectInvalidForwardedForIpv6() throws Exception {
      request.addHeader(FORWARDED, "for=\"2a02:918:175:ab60:45ee:c12c:dac1:808b\"");

      MockResponse response = new MockResponse();
      new ForwardedHeaderFilter(true).doFilter(new MockHttpContext(request, response), filterChain);
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldRejectInvalidForwardedPort() throws Exception {
      request.addHeader(X_FORWARDED_PORT, "invalid");

      MockResponse response = new MockResponse();
      new ForwardedHeaderFilter(false).doFilter(new MockHttpContext(request, response), filterChain);
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

  }

  @Nested
  class ForwardedPrefix {

    @Test
    void contextPathPreserveEncoding() throws Exception {
      request.setRequestURI("/app%20/path/");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRequestURI()).isEqualTo("/app%20/path/");
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/app%20/path/");
    }

    @Test
    void requestUri() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/");
      request.setRequestURI("/app/path");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRequestURI()).isEqualTo("/app/path");
    }

    @Test
    void requestUriWithTrailingSlash() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/");
      request.setRequestURI("/app/path/");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRequestURI()).isEqualTo("/app/path/");
    }

    @Test
    void requestUriPreserveEncoding() throws Exception {
      request.setRequestURI("/app/path%20with%20spaces/");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRequestURI()).isEqualTo("/app/path%20with%20spaces/");
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/app/path%20with%20spaces/");
    }

    @Test
    void requestUriRootUrl() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/");
      request.setRequestURI("/app");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRequestURI()).isEqualTo("/app");
    }

    @Test
    void requestUriPreserveSemicolonContent() throws Exception {
      request.setRequestURI("/path;a=b/with/semicolon");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRequestURI()).isEqualTo("/path;a=b/with/semicolon");
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/path;a=b/with/semicolon");
    }

    @Test
    void caseInsensitiveForwardedPrefix() throws Exception {
      request = new MockRequest() {

        @Override
        public String getHeader(String header) {
          Enumeration<String> names = getHeaderNames();
          while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.equals(header)) {
              return super.getHeader(header);
            }
          }
          return null;
        }
      };
      request.addHeader(X_FORWARDED_PREFIX, "/prefix");
      request.setRequestURI("/path");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRequestURI()).isEqualTo("/prefix/path");
    }

    @Test
    void requestUriWithForwardedPrefix() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/prefix");
      request.setRequestURI("/mvc-showcase");

      HttpContext actual = filterAndGetWrappedRequest(false);
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/prefix/mvc-showcase");
    }

    @Test
    void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/prefix/");
      request.setRequestURI("/mvc-showcase");

      HttpContext actual = filterAndGetWrappedRequest(false);
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/prefix/mvc-showcase");
    }

    @Test
    void shouldConcatenatePrefixes() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/first,/second");
      request.setRequestURI("/mvc-showcase");

      HttpContext actual = filterAndGetWrappedRequest(false);
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/first/second/mvc-showcase");
    }

    @Test
    void shouldConcatenatePrefixesWithTrailingSlashes() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/first/,/second//");
      request.setRequestURI("/mvc-showcase");

      HttpContext actual = filterAndGetWrappedRequest(false);
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/first/second/mvc-showcase");
    }

    @Test
    void shouldRemoveSingleTrailingSlash() throws Exception {
      request.addHeader(X_FORWARDED_PREFIX, "/prefix,/");
      request.setRequestURI("/mvc-showcase");

      HttpContext actual = filterAndGetWrappedRequest(false);
      assertThat(actual.getRequestURL()).isEqualTo("http://localhost/prefix/mvc-showcase");
    }

  }

  @Nested
  class ForwardedFor {

    @Test
    void xForwardedForEmpty() throws Exception {
      request.addHeader(X_FORWARDED_FOR, "");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRemoteAddress()).isEqualTo(MockRequest.DEFAULT_REMOTE_ADDR);
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void xForwardedForSingleIdentifier() throws Exception {
      request.addHeader(X_FORWARDED_FOR, "203.0.113.195");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRemoteAddress()).isEqualTo("203.0.113.195");
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void xForwardedForMultipleIdentifiers() throws Exception {
      request.addHeader(X_FORWARDED_FOR, "203.0.113.195, 70.41.3.18, 150.172.238.178");
      HttpContext actual = filterAndGetWrappedRequest(false);

      assertThat(actual.getRemoteAddress()).isEqualTo("203.0.113.195");
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void forwardedForIpV4Identifier() throws Exception {
      request.addHeader(FORWARDED, "for=203.0.113.195");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRemoteAddress()).isEqualTo("203.0.113.195");
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void forwardedForIpV6Identifier() throws Exception {
      request.addHeader(FORWARDED, "for=\"[2001:db8:cafe::17]\"");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRemoteAddress()).isEqualTo("[2001:db8:cafe::17]");
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void forwardedForIpV4IdentifierWithPort() throws Exception {
      request.addHeader(FORWARDED, "for=\"203.0.113.195:47011\"");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRemoteAddress()).isEqualTo("203.0.113.195");
      assertThat(actual.getRemotePort()).isEqualTo(47011);
    }

    @Test
    void forwardedForIpV6IdentifierWithPort() throws Exception {
      request.addHeader(FORWARDED, "For=\"[2001:db8:cafe::17]:47011\"");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRemoteAddress()).isEqualTo("[2001:db8:cafe::17]");
      assertThat(actual.getRemotePort()).isEqualTo(47011);
    }

    @Test
    void forwardedForMultipleIdentifiers() throws Exception {
      request.addHeader(FORWARDED, "for=203.0.113.195;proto=http, for=\"[2001:db8:cafe::17]\", for=unknown");
      HttpContext actual = filterAndGetWrappedRequest(true);

      assertThat(actual.getRemoteAddress()).isEqualTo("203.0.113.195");
      assertThat(actual.getRemotePort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

  }

  @Nested
  class ForwardedBy {

    @Test
    void forwardedByIpV4Identifier() throws Exception {
      request.addHeader(FORWARDED, "By=203.0.113.195");
      HttpContext actual = filterAndGetWrappedRequest(true);
      InetSocketAddress localAddress = (InetSocketAddress) actual.localAddress();

      assertThat(localAddress.getHostString()).isEqualTo("203.0.113.195");
      assertThat(localAddress.getPort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void forwardedByIpV6Identifier() throws Exception {
      request.addHeader(FORWARDED, "By=\"[2001:db8:cafe::17]\"");
      HttpContext actual = filterAndGetWrappedRequest(true);
      InetSocketAddress localAddress = (InetSocketAddress) actual.localAddress();

      assertThat(localAddress.getHostString()).isEqualTo("[2001:db8:cafe::17]");
      assertThat(localAddress.getPort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

    @Test
    void forwardedByIpV4IdentifierWithPort() throws Exception {
      request.addHeader(FORWARDED, "By=\"203.0.113.195:47011\"");
      HttpContext actual = filterAndGetWrappedRequest(true);
      InetSocketAddress localAddress = (InetSocketAddress) actual.localAddress();
      assertThat(localAddress.getHostString()).isEqualTo("203.0.113.195");
      assertThat(localAddress.getPort()).isEqualTo(47011);
    }

    @Test
    void forwardedByIpV6IdentifierWithPort() throws Exception {
      request.addHeader(FORWARDED, "By=\"[2001:db8:cafe::17]:47011\"");
      HttpContext actual = filterAndGetWrappedRequest(true);
      InetSocketAddress localAddress = (InetSocketAddress) actual.localAddress();
      assertThat(localAddress.getHostString()).isEqualTo("[2001:db8:cafe::17]");
      assertThat(localAddress.getPort()).isEqualTo(47011);
    }

    @Test
    void forwardedByMultipleIdentifiers() throws Exception {
      request.addHeader(FORWARDED, "by=203.0.113.195;proto=http, by=\"[2001:db8:cafe::17]\", by=unknown");
      HttpContext actual = filterAndGetWrappedRequest(true);
      InetSocketAddress localAddress = (InetSocketAddress) actual.localAddress();

      assertThat(localAddress.getHostString()).isEqualTo("203.0.113.195");
      assertThat(localAddress.getPort()).isEqualTo(MockRequest.DEFAULT_SERVER_PORT);
    }

  }

  @Nested
  class SendRedirect {

    private ForwardedHeaderFilter filter;

    @Test
    void sendRedirectWithAbsolutePath() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String redirectedUrl = sendRedirect("/foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
    }

    @Test
    void sendRedirectWithAbsolutePathQueryParamAndFragment() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");
      request.setQueryString("oldqp=1");

      String redirectedUrl = sendRedirect("/foo/bar?newqp=2#fragment");
      assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar?newqp=2#fragment");
    }

    @Test
    void sendRedirectWithContextPath() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String redirectedUrl = sendRedirect("/context/foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
    }

    @Test
    void sendRedirectWithRelativePath() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");
      request.setRequestURI("/parent/");

      String redirectedUrl = sendRedirect("foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/parent/foo/bar");
    }

    @Test
    void sendRedirectWithFileInPathAndRelativeRedirect() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");
      request.setRequestURI("/context/a");

      String redirectedUrl = sendRedirect("foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/context/foo/bar");
    }

    @Test
    void sendRedirectWithRelativePathIgnoresFile() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");
      request.setRequestURI("/parent");

      String redirectedUrl = sendRedirect("foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
    }

    @Test
    void sendRedirectWithLocationDotDotPath() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String redirectedUrl = sendRedirect("parent/../foo/bar");
      assertThat(redirectedUrl).isEqualTo("https://example.com/foo/bar");
    }

    @Test
    void sendRedirectWithLocationHasScheme() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String location = "http://company.example/foo/bar";
      String redirectedUrl = sendRedirect(location);
      assertThat(redirectedUrl).isEqualTo(location);
    }

    @Test
    void sendRedirectWithLocationSlashSlash() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String location = "//other.info/foo/bar";
      String redirectedUrl = sendRedirect(location);
      assertThat(redirectedUrl).isEqualTo(("https:" + location));
    }

    @Test
    void sendRedirectWithLocationSlashSlashParentDotDot() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      String location = "//other.info/parent/../foo/bar";
      String redirectedUrl = sendRedirect(location);
      assertThat(redirectedUrl).isEqualTo(("https:" + location));
    }

    @Test
    void sendRedirectWithNoXForwardedAndAbsolutePath() throws Exception {
      String redirectedUrl = sendRedirect("/foo/bar");
      assertThat(redirectedUrl).isEqualTo("/foo/bar");
    }

    @Test
    void sendRedirectWithNoXForwardedAndDotDotPath() throws Exception {
      String redirectedUrl = sendRedirect("../foo/bar");
      assertThat(redirectedUrl).isEqualTo("../foo/bar");
    }

    @Test
    void sendRedirectWhenRequestOnlyAndXForwardedThenUsesRelativeRedirects() throws Exception {
      request.addHeader(X_FORWARDED_PROTO, "https");
      request.addHeader(X_FORWARDED_HOST, "example.com");
      request.addHeader(X_FORWARDED_PORT, "443");

      this.filter = new ForwardedHeaderFilter(false);
      this.filter.setRelativeRedirects(true);
      String location = sendRedirect("/a");

      assertThat(location).isEqualTo("/a");
    }

    @Test
    void sendRedirectWhenRequestOnlyAndNoXForwardedThenUsesRelativeRedirects() throws Exception {
      this.filter = new ForwardedHeaderFilter(true);
      this.filter.setRelativeRedirects(true);
      String location = sendRedirect("/a");

      assertThat(location).isEqualTo("/a");
    }

    private String sendRedirect(final String location) throws Exception {
      Filter redirectFilter = new Filter() {
        @Override
        public void doFilter(HttpContext http, FilterChain chain) throws Exception {
          http.sendRedirect(location);
        }
      };
      this.filter = this.filter == null ? new ForwardedHeaderFilter(false) : this.filter;
      MockResponse response = new MockResponse();
      FilterChain filterChain = new MockFilterChain(mock(), this.filter, redirectFilter);
      filterChain.doFilter(new MockHttpContext(request, response));
      return response.getRedirectedUrl();
    }
  }

  private HttpContext filterAndGetWrappedRequest(boolean useStandardHeader) throws Exception {
    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(useStandardHeader);
    filter.setUseForwardedPrefix(true);
    filter.doFilter(new MockHttpContext(request), this.filterChain);
    return this.filterChain.getContext();
  }

}