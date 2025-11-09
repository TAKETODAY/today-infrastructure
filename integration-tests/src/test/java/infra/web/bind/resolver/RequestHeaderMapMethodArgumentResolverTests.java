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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import infra.core.annotation.SynthesizingMethodParameter;
import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestHeader;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/15 14:32
 */
class RequestHeaderMapMethodArgumentResolverTests {

  private RequestHeaderMapMethodArgumentResolver resolver;

  private ResolvableMethodParameter paramMap;
  private ResolvableMethodParameter paramMultiValueMap;
  private ResolvableMethodParameter paramHttpHeaders;
  private ResolvableMethodParameter paramUnsupported;

  @BeforeEach
  public void setup() throws Throwable {
    resolver = new RequestHeaderMapMethodArgumentResolver();

    Method method = getClass().getMethod("params", Map.class, MultiValueMap.class, HttpHeaders.class, Map.class);
    paramMap = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));
    paramMultiValueMap = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 1));
    paramHttpHeaders = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 2));
    paramUnsupported = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 3));

  }

  @Test
  public void supportsParameter() {
    assertThat(resolver.supportsParameter(paramMap)).as("Map parameter not supported").isTrue();
    assertThat(resolver.supportsParameter(paramMultiValueMap)).as("MultiValueMap parameter not supported").isTrue();
    assertThat(resolver.supportsParameter(paramHttpHeaders)).as("HttpHeaders parameter not supported").isTrue();
    assertThat(resolver.supportsParameter(paramUnsupported)).as("non-@RequestParam map supported").isFalse();
  }

  @Test
  public void resolveMapArgument() throws Throwable {
    String name = "foo";
    String value = "bar";
    Map<String, String> expected = Collections.singletonMap(name, value);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader(name, value);

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramMap);

    boolean condition = result instanceof Map;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultiValueMapArgument() throws Throwable {
    String name = "foo";
    String value1 = "bar";
    String value2 = "baz";
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    request.addHeader(name, value1);
    request.addHeader(name, value2);

    MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
    expected.add(name, value1);
    expected.add(name, value2);

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramMultiValueMap);

    assertThat(result instanceof MultiValueMap).isTrue();
    assertThat(expected).as("Invalid result").isEqualTo(result);
  }

  @Test
  public void resolveHttpHeadersArgument() throws Throwable {
    String name = "foo";
    String value1 = "bar";
    String value2 = "baz";
    HttpMockRequestImpl request = new HttpMockRequestImpl();

    request.addHeader(name, value1);
    request.addHeader(name, value2);

    HttpHeaders expected = HttpHeaders.forWritable();
    expected.add(name, value1);
    expected.add(name, value2);

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());
    Object result = resolver.resolveArgument(webRequest, paramHttpHeaders);

    boolean condition = result instanceof HttpHeaders;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  void resolveMapArgumentWithNoHeaders() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramMap);

    assertThat(result instanceof Map).isTrue();
    assertThat(((Map<?, ?>) result)).isEmpty();
  }

  @Test
  void resolveMultiValueMapArgumentWithNoHeaders() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramMultiValueMap);

    assertThat(result instanceof MultiValueMap).isTrue();
    assertThat(((MultiValueMap<?, ?>) result)).isEmpty();
  }

  @Test
  void resolveHttpHeadersArgumentWithNoHeaders() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramHttpHeaders);

    assertThat(result instanceof HttpHeaders).isTrue();
    assertThat(((HttpHeaders) result)).isEmpty();
  }

  @Test
  void resolveMapArgumentWithMultipleHeaders() throws Throwable {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("header1", "value1");
    request.addHeader("header2", "value2");
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, paramMap);

    assertThat(result instanceof Map).isTrue();
    Map resultMap = (Map<?, ?>) result;
    assertThat(resultMap).containsEntry("header1", "value1");
    assertThat(resultMap).containsEntry("header2", "value2");
  }

  @Test
  void resolveCustomMapImplementation() throws Throwable {
    Method method = getClass().getMethod("customMapParam", CustomMap.class);
    ResolvableMethodParameter customMapParam = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("test", "value");
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, customMapParam);

    assertThat(result instanceof CustomMap).isTrue();
    CustomMap resultMap = (CustomMap<?, ?>) result;
    assertThat(resultMap).containsEntry("test", "value");
  }

  @Test
  void resolveCustomMultiValueMapImplementation() throws Throwable {
    Method method = getClass().getMethod("customMultiValueMapParam", CustomMultiValueMap.class);
    ResolvableMethodParameter customMultiValueMapParam = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addHeader("test", "value1");
    request.addHeader("test", "value2");
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Object result = resolver.resolveArgument(webRequest, customMultiValueMapParam);

    assertThat(result instanceof CustomMultiValueMap).isTrue();
    CustomMultiValueMap resultMap = (CustomMultiValueMap<?, ?>) result;
    assertThat(resultMap.get("test")).containsExactly("value1", "value2");
  }

  public void customMapParam(@RequestHeader CustomMap<?, ?> param) {
  }

  public void customMultiValueMapParam(@RequestHeader CustomMultiValueMap<?, ?> param) {
  }

  static class CustomMap<K, V> extends java.util.HashMap<K, V> {
  }

  static class CustomMultiValueMap<K, V> extends LinkedMultiValueMap<K, V> {
  }

  public void params(@RequestHeader Map<?, ?> param1,
          @RequestHeader MultiValueMap<?, ?> param2,
          @RequestHeader HttpHeaders param3, Map<?, ?> unsupported) {
  }

}
