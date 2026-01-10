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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.core.MethodParameter;
import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerMatchingMetadata;
import infra.web.annotation.PathVariable;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

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

  @Test
  void resolveArgumentWithCustomMapImplementation() throws Throwable {
    Method method = TestController.class.getMethod("handleCustomMap", CustomMap.class);
    ResolvableMethodParameter paramCustomMap = new ResolvableMethodParameter(new MethodParameter(method, 0));

    applyTemplateVars();

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramCustomMap);

    assertThat(result).isInstanceOf(CustomMap.class);
    assertThat(((Map) result)).containsEntry("name", "value");
  }

  @Test
  void resolveArgumentWithLinkedHashMap() throws Throwable {
    Method method = TestController.class.getMethod("handleLinkedHashMap", LinkedHashMap.class);
    ResolvableMethodParameter paramLinkedHashMap = new ResolvableMethodParameter(new MethodParameter(method, 0));

    applyTemplateVars();

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramLinkedHashMap);

    assertThat(result).isInstanceOf(LinkedHashMap.class);
    assertThat(((Map) result)).containsEntry("name", "value");
  }

  @Test
  void resolveArgumentWithEmptyUriVariables() throws Throwable {
    RequestPath requestPath = RequestPath.parse("/mock", null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("/mock");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);
    webRequest.setMatchingMetadata(matchingMetadata);

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramMap);

    assertThat(result).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) result)).isEmpty();
  }

  @Test
  void resolveArgumentWithoutMatchingMetadata() throws Throwable {
    MockRequestContext context = new MockRequestContext(null, new HttpMockRequestImpl(), new MockHttpResponseImpl());
    // No matching metadata

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    Object result = resolver.resolveArgument(context, paramMap);

    assertThat(result).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) result)).isEmpty();
  }

  @Test
  void supportsParameterWithNonMapType() throws Exception {
    Method method = TestController.class.getMethod("handleString", String.class);
    ResolvableMethodParameter paramString = new ResolvableMethodParameter(new MethodParameter(method, 0));

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    assertThat(resolver.supportsParameter(paramString)).isFalse();
  }

  @Test
  void supportsParameterWithoutPathVariableAnnotation() throws Exception {
    Method method = TestController.class.getMethod("handleMapWithoutAnnotation", Map.class);
    ResolvableMethodParameter paramMapNoAnnot = new ResolvableMethodParameter(new MethodParameter(method, 0));

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    assertThat(resolver.supportsParameter(paramMapNoAnnot)).isFalse();
  }

  @Test
  void supportsParameterWithNamedPathVariable() throws Exception {
    Method method = TestController.class.getMethod("handleNamedMap", Map.class);
    ResolvableMethodParameter paramNamedMap = new ResolvableMethodParameter(new MethodParameter(method, 0));

    PathVariableMapMethodArgumentResolver resolver = new PathVariableMapMethodArgumentResolver();
    assertThat(resolver.supportsParameter(paramNamedMap)).isFalse();
  }

  static class TestController {
    public void handleCustomMap(@PathVariable CustomMap customMap) { }

    public void handleLinkedHashMap(@PathVariable LinkedHashMap map) { }

    public void handleString(@PathVariable String string) { }

    public void handleMapWithoutAnnotation(Map<String, String> map) { }

    public void handleNamedMap(@PathVariable("name") Map<String, String> namedMap) { }
  }

  static class CustomMap extends LinkedHashMap<String, String> {
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
