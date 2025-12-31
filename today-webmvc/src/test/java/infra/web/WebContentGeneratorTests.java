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

package infra.web;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.TimeUnit;

import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.session.Session;
import infra.session.SessionRequiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 14:53
 */
class WebContentGeneratorTests {

  @Test
  void defaultConstructorSupportsGetHeadPostMethods() {
    WebContentGenerator generator = new TestWebContentGenerator();

    assertThat(generator.getSupportedMethods()).containsExactlyInAnyOrder("GET", "HEAD", "POST");
  }

  @Test
  void constructorWithRestrictFalseSupportsAllMethods() {
    WebContentGenerator generator = new TestWebContentGenerator(false);

    assertThat(generator.getSupportedMethods()).isNull();
  }

  @Test
  void constructorWithSpecificMethods() {
    WebContentGenerator generator = new TestWebContentGenerator("GET", "PUT");

    assertThat(generator.getSupportedMethods()).containsExactlyInAnyOrder("GET", "PUT");
  }

  @Test
  void setSupportedMethodsUpdatesAllowHeader() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setSupportedMethods("GET", "PUT");

    assertThat(generator.getSupportedMethods()).containsExactlyInAnyOrder("GET", "PUT");
    assertThat(generator.getAllowHeader()).contains("GET", "PUT", "OPTIONS");
  }

  @Test
  void setSupportedMethodsToNull() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setSupportedMethods((String[]) null);

    assertThat(generator.getSupportedMethods()).isNull();
  }

  @Test
  void getAllowHeaderIncludesOptionsAutomatically() {
    WebContentGenerator generator = new TestWebContentGenerator("GET", "POST");

    assertThat(generator.getAllowHeader()).contains("GET", "POST", "OPTIONS");
  }

  @Test
  void getAllowHeaderWithAllMethods() {
    WebContentGenerator generator = new TestWebContentGenerator(false);

    assertThat(generator.getAllowHeader()).contains("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(generator.getAllowHeader()).doesNotContain("TRACE");
  }

  @Test
  void setRequireSession() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setRequireSession(true);

    assertThat(generator.isRequireSession()).isTrue();
  }

  @Test
  void setCacheControl() {
    WebContentGenerator generator = new TestWebContentGenerator();
    CacheControl cacheControl = CacheControl.maxAge(3600, TimeUnit.SECONDS);
    generator.setCacheControl(cacheControl);

    assertThat(generator.getCacheControl()).isSameAs(cacheControl);
  }

  @Test
  void setCacheSeconds() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setCacheSeconds(3600);

    assertThat(generator.getCacheSeconds()).isEqualTo(3600);
  }

  @Test
  void setVaryByRequestHeaders() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders("Accept-Language", "User-Agent");

    assertThat(generator.getVaryByRequestHeaders()).containsExactly("Accept-Language", "User-Agent");
  }

  @Test
  void checkRequestWithUnsupportedMethod() {
    WebContentGenerator generator = new TestWebContentGenerator("GET");
    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("POST");

    assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class)
            .isThrownBy(() -> generator.checkRequest(request))
            .withMessageContaining("POST");
  }

  @Test
  void checkRequestWithSupportedMethod() {
    WebContentGenerator generator = new TestWebContentGenerator("GET", "POST");
    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("POST");

    assertThatCode(() -> generator.checkRequest(request)).doesNotThrowAnyException();
  }

  @Test
  void checkRequestWithRequiredSessionButNoneFound() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setRequireSession(true);

    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("GET");

    assertThatExceptionOfType(SessionRequiredException.class)
            .isThrownBy(() -> generator.checkRequest(request));
  }

  @Test
  void checkRequestWithRequiredSessionAndSessionFound() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setRequireSession(true);

    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("GET");

    Session session = mock(Session.class);
    try (MockedStatic<RequestContextUtils> mocked = mockStatic(RequestContextUtils.class)) {
      when(RequestContextUtils.getSession(request, false)).thenReturn(session);

      assertThatCode(() -> generator.checkRequest(request)).doesNotThrowAnyException();
    }
  }

  @Test
  void prepareResponseWithCacheControl() {
    WebContentGenerator generator = new TestWebContentGenerator();
    CacheControl cacheControl = CacheControl.maxAge(3600, TimeUnit.SECONDS);
    generator.setCacheControl(cacheControl);

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);

    generator.prepareResponse(response);

    verify(response).setHeader(eq("Cache-Control"), anyString());
  }

  @Test
  void prepareResponseWithCacheSecondsPositive() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setCacheSeconds(3600);

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);

    generator.prepareResponse(response);

    verify(response).setHeader(eq("Cache-Control"), anyString());
  }

  @Test
  void prepareResponseWithCacheSecondsZero() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setCacheSeconds(0);

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);

    generator.prepareResponse(response);

    verify(response).setHeader("Cache-Control", "no-store");
  }

  @Test
  void prepareResponseWithVaryHeaders() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders("Accept-Language");

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);
    when(response.containsResponseHeader("Vary")).thenReturn(false);

    generator.prepareResponse(response);

    verify(headers).setVary(List.of("Accept-Language"));
  }

  @Test
  void applyCacheControl() {
    WebContentGenerator generator = new TestWebContentGenerator();
    CacheControl cacheControl = CacheControl.maxAge(3600, TimeUnit.SECONDS);

    RequestContext response = mock(RequestContext.class);

    generator.applyCacheControl(response, cacheControl);

    verify(response).setHeader("Cache-Control", "max-age=3600");
  }

  @Test
  void applyCacheSecondsPositive() {
    WebContentGenerator generator = new TestWebContentGenerator();
    RequestContext response = mock(RequestContext.class);

    generator.applyCacheSeconds(response, 3600);

    verify(response).setHeader("Cache-Control", "max-age=3600");
  }

  @Test
  void applyCacheSecondsZero() {
    WebContentGenerator generator = new TestWebContentGenerator();
    RequestContext response = mock(RequestContext.class);

    generator.applyCacheSeconds(response, 0);

    verify(response).setHeader("Cache-Control", "no-store");
  }

  @Test
  void applyCacheSecondsNegative() {
    WebContentGenerator generator = new TestWebContentGenerator();
    RequestContext response = mock(RequestContext.class);

    generator.applyCacheSeconds(response, -1);

    verify(response, never()).setHeader(eq("Cache-Control"), anyString());
  }

  @Test
  void defaultConstructorWithTrueRestrictSupportsDefaultMethods() {
    WebContentGenerator generator = new TestWebContentGenerator(true);

    assertThat(generator.getSupportedMethods()).containsExactlyInAnyOrder("GET", "HEAD", "POST");
  }

  @Test
  void getAllowHeaderWithExplicitOptionsSupport() {
    WebContentGenerator generator = new TestWebContentGenerator("GET", "POST", "OPTIONS");

    assertThat(generator.getAllowHeader()).contains("GET", "POST", "OPTIONS");
  }

  @Test
  void getAllowHeaderWithoutOptionsSupport() {
    WebContentGenerator generator = new TestWebContentGenerator("GET", "POST");

    assertThat(generator.getAllowHeader()).contains("GET", "POST", "OPTIONS");
    assertThat(generator.getAllowHeader().split(",")).hasSize(3);
  }

  @Test
  void setRequireSessionToFalse() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setRequireSession(false);

    assertThat(generator.isRequireSession()).isFalse();
  }

  @Test
  void setCacheControlToNull() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setCacheControl(null);

    assertThat(generator.getCacheControl()).isNull();
  }

  @Test
  void setCacheSecondsDefaultValue() {
    WebContentGenerator generator = new TestWebContentGenerator();

    assertThat(generator.getCacheSeconds()).isEqualTo(-1);
  }

  @Test
  void setVaryByRequestHeadersToNull() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders((String[]) null);

    assertThat(generator.getVaryByRequestHeaders()).isNull();
  }

  @Test
  void setVaryByRequestHeadersEmpty() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders();

    assertThat(generator.getVaryByRequestHeaders()).isEmpty();
  }

  @Test
  void checkRequestWithNullSupportedMethodsAllowsAll() {
    WebContentGenerator generator = new TestWebContentGenerator(false);
    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("DELETE");

    assertThatCode(() -> generator.checkRequest(request)).doesNotThrowAnyException();
  }

  @Test
  void checkRequestWithRequiredSessionAndNullSessionThrowsException() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setRequireSession(true);

    RequestContext request = mock(RequestContext.class);
    when(request.getMethodAsString()).thenReturn("GET");
    try (MockedStatic<RequestContextUtils> mocked = mockStatic(RequestContextUtils.class)) {
      when(RequestContextUtils.getSession(request, false)).thenReturn(null);

      assertThatExceptionOfType(SessionRequiredException.class)
              .isThrownBy(() -> generator.checkRequest(request))
              .withMessage("Pre-existing session required but none found");
    }
  }

  @Test
  void prepareResponseWithNullCacheControlAndNegativeCacheSeconds() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setCacheSeconds(-1);

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);

    generator.prepareResponse(response);

    verify(response, never()).setHeader(eq("Cache-Control"), anyString());
  }

  @Test
  void prepareResponseWithExistingVaryHeader() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders("Accept-Language", "User-Agent");

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);
    when(response.containsResponseHeader("Vary")).thenReturn(true);
    when(headers.getVary()).thenReturn(List.of("Accept-Encoding"));

    generator.prepareResponse(response);

    verify(headers).setVary(List.of("Accept-Language", "User-Agent"));
  }

  @Test
  void prepareResponseWithExistingVaryHeaderContainingWildcard() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders("Accept-Language");

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);
    when(response.containsResponseHeader("Vary")).thenReturn(true);
    when(headers.getVary()).thenReturn(List.of("*"));

    generator.prepareResponse(response);

    verify(headers).setVary(List.of());
  }

  @Test
  void prepareResponseWithDuplicateVaryHeaders() {
    WebContentGenerator generator = new TestWebContentGenerator();
    generator.setVaryByRequestHeaders("Accept-Language", "User-Agent");

    RequestContext response = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.responseHeaders()).thenReturn(headers);
    when(response.containsResponseHeader("Vary")).thenReturn(true);
    when(headers.getVary()).thenReturn(List.of("Accept-Language"));

    generator.prepareResponse(response);

    verify(headers).setVary(List.of("User-Agent"));
  }

  @Test
  void applyCacheControlWithNullHeaderValue() {
    WebContentGenerator generator = new TestWebContentGenerator();
    CacheControl cacheControl = CacheControl.empty();

    RequestContext response = mock(RequestContext.class);

    generator.applyCacheControl(response, cacheControl);

    verify(response, never()).setHeader(anyString(), anyString());
  }

  @Test
  void applyCacheSecondsWithMaxAge() {
    WebContentGenerator generator = new TestWebContentGenerator();
    RequestContext response = mock(RequestContext.class);

    generator.applyCacheSeconds(response, 1800);

    verify(response).setHeader("Cache-Control", "max-age=1800");
  }

  @Test
  void initAllowHeaderWithNullSupportedMethods() {
    WebContentGenerator generator = new TestWebContentGenerator(false);

    assertThat(generator.getAllowHeader()).isNotNull();
    assertThat(generator.getAllowHeader()).contains("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS");
    assertThat(generator.getAllowHeader()).doesNotContain("TRACE");
  }

  static class TestWebContentGenerator extends WebContentGenerator {
    public TestWebContentGenerator() {
    }

    public TestWebContentGenerator(boolean restrictDefaultSupportedMethods) {
      super(restrictDefaultSupportedMethods);
    }

    public TestWebContentGenerator(String... supportedMethods) {
      super(supportedMethods);
    }
  }

}