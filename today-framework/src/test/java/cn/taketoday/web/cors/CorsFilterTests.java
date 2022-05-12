/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.filter.CorsFilter;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link CorsFilter}.
 *
 * @author Sebastien Deleuze
 * @author TODAY 2021/4/15 14:29
 */
public class CorsFilterTests {
  private CorsFilter filter;

  private final CorsConfiguration config = new CorsConfiguration();

  @BeforeEach
  public void setup() {
    config.setAllowedOrigins(Arrays.asList("https://domain1.com", "https://domain2.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST"));
    config.setAllowedHeaders(Arrays.asList("header1", "header2"));
    config.setExposedHeaders(Arrays.asList("header3", "header4"));
    config.setMaxAge(123L);
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
    configSource.registerCorsConfiguration("/**", config);

    filter = new CorsFilter(configSource);
  }

  @Test
  public void nonCorsRequest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test.html");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
    };
    filter.doFilter(request, response, filterChain);
  }

  @Test
  public void sameOriginRequest() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "https://domain1.com/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain1.com");
    request.setScheme("https");
    request.setServerName("domain1.com");
    request.setServerPort(443);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isNull();
    };
    filter.doFilter(request, response, filterChain);
  }

  @Test
  public void validActualRequest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader("header2", "foo");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      final RequestContext context = ServletUtils.getRequestContext(request, response);
      assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
      final String header = response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
      assertThat(header).isEqualTo("header3, header4");
    };
    filter.doFilter(request, response, filterChain);
  }

  @Test
  public void invalidActualRequest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.DELETE.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader("header2", "foo");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) ->
            fail("Invalid requests must not be forwarded to the filter chain");
    filter.doFilter(request, response, filterChain);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

  @Test
  public void validPreFlightRequest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name());
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) ->
            fail("Preflight requests must not be forwarded to the filter chain");
    filter.doFilter(request, response, filterChain);

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("header1, header2");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("header3, header4");
    assertThat(Long.parseLong(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))).isEqualTo(123L);
  }

  @Test
  public void invalidPreFlightRequest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/test.html");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name());
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) ->
            fail("Preflight requests must not be forwarded to the filter chain");
    filter.doFilter(request, response, filterChain);

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

}
