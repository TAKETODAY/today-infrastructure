package infra.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.mock.web.MockRequest;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.FilterChain;
import infra.web.cors.CorsConfiguration;
import infra.web.cors.DefaultCorsProcessor;
import infra.web.cors.UrlBasedCorsConfigurationSource;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/6/1 12:40
 */
class CorsFilterTests {

  private CorsFilter filter;

  private final CorsConfiguration config = new CorsConfiguration();

  @BeforeEach
  void setup() {
    config.setAllowedOrigins(Arrays.asList("https://domain1.com", "https://domain2.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST"));
    config.setAllowedHeaders(Arrays.asList("header1", "header2"));
    config.setExposedHeaders(Arrays.asList("header3", "header4"));
    config.setMaxAge(123L);
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
    configSource.registerCorsConfiguration("/**", config);

    filter = new CorsFilter(configSource);
    filter.setCorsProcessor(new DefaultCorsProcessor());
  }

  @Test
  void nonCorsRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.GET.name(), "/test.html");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) -> {
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
    };
    filter.doFilter(new MockRequestContext(request), filterChain);
  }

  @Test
  void sameOriginRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.GET.name(), "https://domain1.com/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain1.com");
    request.setScheme("https");
    request.setServerName("domain1.com");
    request.setServerPort(443);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) -> {
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
    };
    filter.doFilter(new MockRequestContext(request), filterChain);
  }

  @Test
  void validActualRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.GET.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader("header2", "foo");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) -> {
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
    };
    filter.doFilter(new MockRequestContext(request, response), filterChain);
  }

  @Test
  void invalidActualRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.DELETE.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader("header2", "foo");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) ->
            fail("Invalid requests must not be forwarded to the filter chain");
    filter.doFilter(new MockRequestContext(request), filterChain);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

  @Test
  void validPreFlightRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.OPTIONS.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) ->
            fail("Preflight requests must not be forwarded to the filter chain");
    filter.doFilter(new MockRequestContext(request, response), filterChain);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("header1, header2");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
    assertThat(Long.parseLong(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))).isEqualTo(123L);
  }

  @Test
  void invalidPreFlightRequest() throws Throwable {

    MockRequest request = new MockRequest(HttpMethod.OPTIONS.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name());
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    FilterChain filterChain = (filterRequest) ->
            fail("Preflight requests must not be forwarded to the filter chain");
    filter.doFilter(new MockRequestContext(request), filterChain);

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

}