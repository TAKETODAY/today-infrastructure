/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.http.HttpCookie;
import infra.mock.api.http.Cookie;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.annotation.CookieValue;
import infra.web.bind.resolver.CookieParameterResolver.AllCookieParameterResolver;
import infra.web.bind.resolver.CookieParameterResolver.CookieCollectionParameterResolver;
import infra.web.bind.resolver.CookieParameterResolver.CookieValueAnnotationParameterResolver;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 22:13
 */
class CookieParameterResolverTests {

  @Test
  public void supportsParameterWithHttpCookie() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieParameterResolver resolver = new CookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void supportsParameterWithString() throws Exception {
    Method method = getClass().getDeclaredMethod("handleString", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieParameterResolver resolver = new CookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void resolveArgumentWithExistingCookie() throws Throwable {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("testCookie", "testValue"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    MethodParameter parameter1 = new MethodParameter(method, 0);
    parameter1.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(parameter1);
    CookieParameterResolver resolver = new CookieParameterResolver();

    Object result = resolver.resolveArgument(context, parameter);

    assertThat(result).isInstanceOf(HttpCookie.class);
    HttpCookie cookie = (HttpCookie) result;
    assertThat(cookie.getName()).isEqualTo("testCookie");
    assertThat(cookie.getValue()).isEqualTo("testValue");
  }

  @Test
  public void resolveArgumentWithMissingCookie() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    MethodParameter parameter1 = new MethodParameter(method, 0);
    parameter1.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(parameter1);
    CookieParameterResolver resolver = new CookieParameterResolver();

    assertThatThrownBy(() -> resolver.resolveArgument(context, parameter))
            .isInstanceOf(MissingRequestCookieException.class);
  }

  @Test
  public void supportsParameterWithCookieValueAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void supportsParameterWithoutCookieValueAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void resolveNameWithHttpCookieType() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("testCookie", "testValue"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("testCookie", parameter, context);

    assertThat(result).isInstanceOf(HttpCookie.class);
    assertThat(((HttpCookie) result).getName()).isEqualTo("testCookie");
    assertThat(((HttpCookie) result).getValue()).isEqualTo("testValue");
  }

  @Test
  public void resolveNameWithStringType() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("testCookie", "testValue"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("testCookie", parameter, context);

    assertThat(result).isInstanceOf(String.class);
    assertThat(result).isEqualTo("testValue");
  }

  @Test
  public void resolveNameWithMissingCookie() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("missingCookie", parameter, context);

    assertThat(result).isNull();
  }

  @Test
  public void handleMissingValue() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThatThrownBy(() -> resolver.handleMissingValue("missingCookie", parameter))
            .isInstanceOf(MissingRequestCookieException.class);
  }

  @Test
  public void handleMissingValueAfterConversion() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThatThrownBy(() -> resolver.handleMissingValueAfterConversion("missingCookie", parameter, null))
            .isInstanceOf(MissingRequestCookieException.class)
            .hasFieldOrPropertyWithValue("missingAfterConversion", true);
  }

  @Test
  public void allCookieParameterResolverSupportsParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookieArray", HttpCookie[].class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void allCookieParameterResolverSupportsParameterWithNonArrayType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void allCookieParameterResolverSupportsParameterWithWrongComponentType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleStringArray", String[].class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void allCookieParameterResolverResolveArgument() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("cookie1", "value1"), new Cookie("cookie2", "value2"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleCookieArray", HttpCookie[].class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    Object result = resolver.resolveArgument(context, parameter);

    assertThat(result).isInstanceOf(HttpCookie[].class);
    HttpCookie[] resultCookies = (HttpCookie[]) result;
    assertThat(resultCookies).hasSize(2);
    assertThat(resultCookies[0].getName()).isEqualTo("cookie1");
    assertThat(resultCookies[1].getName()).isEqualTo("cookie2");
  }

  @Test
  public void cookieCollectionParameterResolverSupportsParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookieCollection", java.util.List.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieCollectionParameterResolver resolver =
            new CookieCollectionParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void cookieCollectionParameterResolverResolveName() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("cookie1", "value1"), new Cookie("cookie2", "value2"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleCookieCollection", java.util.List.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieCollectionParameterResolver resolver =
            new CookieCollectionParameterResolver(null);

    Object result = resolver.resolveName("anyName", parameter, context);

    assertThat(result).isInstanceOf(HttpCookie[].class);
    HttpCookie[] resultCookies = (HttpCookie[]) result;
    assertThat(resultCookies).hasSize(2);
  }

  @Test
  public void cookieValueAnnotationParameterResolverSupportsParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  public void cookieValueAnnotationParameterResolverDoesNotSupportParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleString", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void cookieValueAnnotationParameterResolverResolveNameWithCookieValue() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("testCookie", "testValue"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("testCookie", parameter, context);

    assertThat(result).isEqualTo("testValue");
  }

  @Test
  public void cookieValueAnnotationParameterResolverResolveNameWithHttpCookie() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    mockRequest.setCookies(new Cookie("testCookie", "testValue"));
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleAnnotatedHttpCookie", HttpCookie.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("testCookie", parameter, context);

    assertThat(result).isInstanceOf(HttpCookie.class);
    HttpCookie resultCookie = (HttpCookie) result;
    assertThat(resultCookie.getName()).isEqualTo("testCookie");
    assertThat(resultCookie.getValue()).isEqualTo("testValue");
  }

  @Test
  public void cookieValueAnnotationParameterResolverResolveNameWithMissingCookie() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter methodParam = new MethodParameter(method, 0);
    methodParam.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParam);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    Object result = resolver.resolveName("missingCookie", parameter, context);

    assertThat(result).isNull();
  }

  @Test
  public void cookieValueAnnotationParameterResolverHandleMissingValue() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThatThrownBy(() -> resolver.handleMissingValue("missingCookie", parameter))
            .isInstanceOf(MissingRequestCookieException.class);
  }

  @Test
  public void cookieValueAnnotationParameterResolverHandleMissingValueAfterConversion() throws Exception {
    Method method = getClass().getDeclaredMethod("handleAnnotatedCookie", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    CookieValueAnnotationParameterResolver resolver =
            new CookieValueAnnotationParameterResolver(null);

    assertThatThrownBy(() -> resolver.handleMissingValueAfterConversion("missingCookie", parameter, null))
            .isInstanceOf(MissingRequestCookieException.class)
            .hasFieldOrPropertyWithValue("missingAfterConversion", true);
  }

  @Test
  public void allCookieParameterResolverDoesNotSupportNonArrayParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void allCookieParameterResolverDoesNotSupportWrongArrayType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleStringArray", String[].class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    AllCookieParameterResolver resolver = new AllCookieParameterResolver();

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  public void cookieCollectionParameterResolverDoesNotSupportNonCollectionParameter() throws Exception {
    Method method = getClass().getDeclaredMethod("handleCookie", HttpCookie.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new MethodParameter(method, 0));
    CookieCollectionParameterResolver resolver =
            new CookieCollectionParameterResolver(null);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @SuppressWarnings("unused")
  private void handleAnnotatedHttpCookie(@CookieValue("testCookie") HttpCookie cookie) { }

  @SuppressWarnings("unused")
  private void handleAnnotatedCookie(@CookieValue("testCookie") String cookieValue) { }

  @SuppressWarnings("unused")
  private void handleCookieArray(HttpCookie[] cookies) { }

  @SuppressWarnings("unused")
  private void handleStringArray(String[] strings) { }

  @SuppressWarnings("unused")
  private void handleCookieCollection(java.util.List<HttpCookie> cookies) { }

  @SuppressWarnings("unused")
  private void handleCookie(HttpCookie testCookie) {
  }

  @SuppressWarnings("unused")
  private void handleString(String value) { }

}