/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;

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
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(name, value);

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

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
    MockHttpServletRequest request = new MockHttpServletRequest();

    request.addHeader(name, value1);
    request.addHeader(name, value2);

    MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
    expected.add(name, value1);
    expected.add(name, value2);

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    Object result = resolver.resolveArgument(webRequest, paramMultiValueMap);

    assertThat(result instanceof MultiValueMap).isTrue();
    assertThat(expected).as("Invalid result").isEqualTo(result);
  }

  @Test
  public void resolveHttpHeadersArgument() throws Throwable {
    String name = "foo";
    String value1 = "bar";
    String value2 = "baz";
    MockHttpServletRequest request = new MockHttpServletRequest();

    request.addHeader(name, value1);
    request.addHeader(name, value2);

    HttpHeaders expected = HttpHeaders.forWritable();
    expected.add(name, value1);
    expected.add(name, value2);

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());
    Object result = resolver.resolveArgument(webRequest, paramHttpHeaders);

    boolean condition = result instanceof HttpHeaders;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  public void params(@RequestHeader Map<?, ?> param1,
          @RequestHeader MultiValueMap<?, ?> param2,
          @RequestHeader HttpHeaders param3, Map<?, ?> unsupported) {
  }

}
