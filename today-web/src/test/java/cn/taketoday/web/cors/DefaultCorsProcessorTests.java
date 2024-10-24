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

package cn.taketoday.web.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/24 16:42
 */
class DefaultCorsProcessorTests {

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  private DefaultCorsProcessor processor;

  private CorsConfiguration conf;

  private MockRequestContext request;

  @BeforeEach
  void setup() {
    this.mockRequest = new HttpMockRequestImpl();
    this.mockRequest.setRequestURI("/test.html");
    this.mockRequest.setServerName("domain1.example");
    this.conf = new CorsConfiguration();
    this.mockResponse = new MockHttpResponseImpl();
    this.mockResponse.setStatus(MockHttpResponseImpl.SC_OK);
    this.processor = new DefaultCorsProcessor();

    request = new MockRequestContext(null, mockRequest, mockResponse);
  }

  @Test
  void requestWithoutOriginHeader() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());

    this.processor.process(this.conf, request);
    request.requestCompleted();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void sameOriginRequest() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://domain1.example");

    this.processor.process(this.conf, request);
    request.requestCompleted();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void actualRequestWithOriginHeader() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void actualRequestWithOriginHeaderAndNullConfig() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

    this.processor.process(null, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void actualRequestWithOriginHeaderAndAllowedOrigin() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.conf.addAllowedOrigin("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void actualRequestCredentials() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.conf.addAllowedOrigin("https://domain1.com");
    this.conf.addAllowedOrigin("https://domain2.com");
    this.conf.addAllowedOrigin("http://domain3.example");
    this.conf.setAllowCredentials(true);

    this.processor.process(this.conf, request);
    request.requestCompleted();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    List<String> headers = this.mockResponse.getHeaders(HttpHeaders.VARY);
    assertThat(headers).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void actualRequestCredentialsWithWildcardOrigin() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

    this.conf.addAllowedOrigin("*");
    this.conf.setAllowCredentials(true);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.processor.process(this.conf, request));

    this.conf.setAllowedOrigins(null);
    this.conf.addAllowedOriginPattern("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void actualRequestCaseInsensitiveOriginMatch() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.conf.addAllowedOrigin("https://DOMAIN2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
  }

  @Test // gh-26892
  public void actualRequestTrailingSlashOriginMatch() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com/");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
  }

  @Test //gh-33682
  public void actualRequestMalformedOriginRejected() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://*@:;");
    this.conf.addAllowedOrigin("https://domain2.com");

    boolean result = this.processor.process(this.conf, request);
    assertThat(result).isFalse();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void actualRequestExposedHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.conf.addExposedHeader("header1");
    this.conf.addExposedHeader("header2");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("header1");
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("header2");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestAllOriginsAllowed() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.conf.addAllowedOrigin("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestWrongAllowedMethod() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE");
    this.conf.addAllowedOrigin("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void preflightRequestMatchedAllowedMethod() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.conf.addAllowedOrigin("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET, HEAD");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
  }

  @Test
  void preflightRequestTestWithOriginButWithoutOtherHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void preflightRequestWithoutRequestMethod() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void preflightRequestWithRequestAndMethodHeaderButNoConfig() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void preflightRequestValidRequestAndConfig() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
    this.conf.addAllowedOrigin("*");
    this.conf.addAllowedMethod("GET");
    this.conf.addAllowedMethod("PUT");
    this.conf.addAllowedHeader("header1");
    this.conf.addAllowedHeader("header2");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET, PUT");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestCredentials() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
    this.conf.addAllowedOrigin("https://domain1.com");
    this.conf.addAllowedOrigin("https://domain2.com");
    this.conf.addAllowedOrigin("http://domain3.example");
    this.conf.addAllowedHeader("Header1");
    this.conf.setAllowCredentials(true);

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestCredentialsWithWildcardOrigin() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
    this.conf.setAllowedOrigins(Arrays.asList("https://domain1.com", "*", "http://domain3.example"));
    this.conf.addAllowedHeader("Header1");
    this.conf.setAllowCredentials(true);

    assertThatIllegalArgumentException().isThrownBy(() ->
            this.processor.process(this.conf, request));

    this.conf.setAllowedOrigins(null);
    this.conf.addAllowedOriginPattern("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestPrivateNetworkWithWildcardOrigin() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
    this.mockRequest.addHeader(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");
    this.conf.setAllowedOrigins(Arrays.asList("https://domain1.com", "*", "http://domain3.example"));
    this.conf.addAllowedHeader("Header1");
    this.conf.setAllowPrivateNetwork(true);

    assertThatIllegalArgumentException().isThrownBy(() ->
            this.processor.process(this.conf, request));

    this.conf.setAllowedOrigins(null);
    this.conf.addAllowedOriginPattern("*");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestAllowedHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
    this.conf.addAllowedHeader("Header1");
    this.conf.addAllowedHeader("Header2");
    this.conf.addAllowedHeader("Header3");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).contains("Header1");
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).contains("Header2");
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).doesNotContain("Header3");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestAllowsAllHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2");
    this.conf.addAllowedHeader("*");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).contains("Header1");
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).contains("Header2");
    assertThat(this.mockResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).doesNotContain("*");
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestWithEmptyHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "");
    this.conf.addAllowedHeader("*");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isFalse();
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestWithNullConfig() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.conf.addAllowedOrigin("*");

    this.processor.process(null, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_FORBIDDEN);
  }

  @Test
  void preventDuplicatedVaryHeaders() throws Exception {
    this.mockRequest.setMethod(HttpMethod.GET.name());
    this.mockResponse.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    this.mockResponse.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    this.mockResponse.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.getHeaders(HttpHeaders.VARY)).containsOnlyOnce(HttpHeaders.ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
  }

  @Test
  void preflightRequestWithoutAccessControlRequestPrivateNetwork() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.conf.addAllowedHeader("*");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isFalse();
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestWithAccessControlRequestPrivateNetworkNotAllowed() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");
    this.conf.addAllowedHeader("*");
    this.conf.addAllowedOrigin("https://domain2.com");

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isFalse();
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

  @Test
  void preflightRequestWithAccessControlRequestPrivateNetworkAllowed() throws Exception {
    this.mockRequest.setMethod(HttpMethod.OPTIONS.name());
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    this.mockRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.mockRequest.addHeader(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");
    this.conf.addAllowedHeader("*");
    this.conf.addAllowedOrigin("https://domain2.com");
    this.conf.setAllowPrivateNetwork(true);

    this.processor.process(this.conf, request);
    assertThat(this.mockResponse.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
    assertThat(this.mockResponse.containsHeader(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isTrue();
    assertThat(this.mockResponse.getStatus()).isEqualTo(MockHttpResponseImpl.SC_OK);
  }

}