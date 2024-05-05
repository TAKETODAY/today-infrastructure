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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 22:06
 */
class PathVariableMapMethodArgumentResolverTests {

  private PathVariableMapMethodArgumentResolver resolver;

  private MockRequestContext webRequest;

  private HttpMockRequestImpl request;

  private ResolvableMethodParameter paramMap;
  private ResolvableMethodParameter paramNamedMap;
  private ResolvableMethodParameter paramMapNoAnnot;

  @BeforeEach
  public void setup() throws Exception {
    resolver = new PathVariableMapMethodArgumentResolver();
    request = new HttpMockRequestImpl();
    webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    Method method = getClass().getMethod("handle", Map.class, Map.class, Map.class);
    paramMap = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramNamedMap = new ResolvableMethodParameter(new MethodParameter(method, 1));
    paramMapNoAnnot = new ResolvableMethodParameter(new MethodParameter(method, 2));
  }

  @Test
  public void supportsParameter() {
    assertThat(resolver.supportsParameter(paramMap)).isTrue();
    assertThat(resolver.supportsParameter(paramNamedMap)).isFalse();
    assertThat(resolver.supportsParameter(paramMapNoAnnot)).isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    applyTemplateVars();

    Map<String, String> uriTemplateVars = new HashMap<>();
    uriTemplateVars.put("name1", "value1");
    uriTemplateVars.put("name2", "value2");

    RequestPath requestPath = RequestPath.parse("/mock/value1/value2", null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("/mock/{name1}/{name2}");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);
    webRequest.setMatchingMetadata(matchingMetadata);

    Object result = resolver.resolveArgument(webRequest, paramMap);

    assertThat(result).isEqualTo(uriTemplateVars);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resolveArgumentNoUriVars() throws Throwable {
    Map<String, String> map = (Map<String, String>) resolver.resolveArgument(webRequest, paramMap);

    assertThat(map).isEqualTo(Collections.emptyMap());
  }

  private void applyTemplateVars() {
    RequestPath requestPath = RequestPath.parse("/mock/value", null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("/mock/{name}");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);
    webRequest.setMatchingMetadata(matchingMetadata);
  }

  public void handle(
          @PathVariable Map<String, String> map,
          @PathVariable(value = "name") Map<String, String> namedMap,
          Map<String, String> mapWithoutAnnotat) {
  }

}
