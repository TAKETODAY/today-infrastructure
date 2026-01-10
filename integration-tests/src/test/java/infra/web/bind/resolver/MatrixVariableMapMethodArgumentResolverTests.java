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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.ResolvableMethod;
import infra.web.annotation.MatrixVariable;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.testfixture.ReflectionTestUtils;
import infra.web.util.pattern.PathMatchInfo;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/28 21:05
 */
class MatrixVariableMapMethodArgumentResolverTests {

  private MatrixVariableMapMethodArgumentResolver resolver;

  private MockRequestContext webRequest;

  private final ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new MatrixVariableMapMethodArgumentResolver();

    BindingContext binding = new BindingContext();
    this.webRequest = new MockRequestContext(null, new HttpMockRequestImpl(), null);
    webRequest.setBinding(binding);

    PathMatchInfo info = new MockPathMatchInfo();
    RequestPath requestPath = RequestPath.parse("/mock", null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);

    ReflectionTestUtils.setField(matchingMetadata, "pathMatchInfo", info);
    webRequest.setMatchingMetadata(matchingMetadata);
  }

  @Test
  public void supportsParameter() {

    assertThat(this.resolver.supportsParameter(this.testMethod.arg(String.class))).isFalse();

    assertThat(this.resolver.supportsParameter(this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName())
            .arg(Map.class, String.class, String.class))).isTrue();

    assertThat(this.resolver.supportsParameter(this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noPathVar())
            .arg(MultiValueMap.class, String.class, String.class))).isTrue();

    assertThat(this.resolver.supportsParameter(this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().pathVar("cars"))
            .arg(MultiValueMap.class, String.class, String.class))).isTrue();

    assertThat(this.resolver.supportsParameter(this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().name("name"))
            .arg(Map.class, String.class, String.class))).isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    MultiValueMap<String, String> params = getVariablesFor("cars");
    params.add("colors", "red");
    params.add("colors", "green");
    params.add("colors", "blue");
    params.add("year", "2012");

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName())
            .arg(Map.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    Map<String, String> map = (Map<String, String>)
            this.resolver.resolveArgument(this.webRequest, param);

    assertThat(map.get("colors")).isEqualTo("red");

    param = this.testMethod
            .annot(MvcAnnotationPredicates.matrixAttribute().noPathVar())
            .arg(MultiValueMap.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    MultiValueMap<String, String> multivalueMap = (MultiValueMap<String, String>)
            this.resolver.resolveArgument(this.webRequest, param);

    assertThat(multivalueMap.get("colors")).isEqualTo(Arrays.asList("red", "green", "blue"));
  }

  @Test
  public void resolveArgumentPathVariable() throws Throwable {
    MultiValueMap<String, String> params1 = getVariablesFor("cars");
    params1.add("colors", "red");
    params1.add("colors", "purple");

    MultiValueMap<String, String> params2 = getVariablesFor("planes");
    params2.add("colors", "yellow");
    params2.add("colors", "orange");

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().pathVar("cars"))
            .arg(MultiValueMap.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    Map<String, ?> mapForPathVar = (Map<String, ?>) this.resolver.resolveArgument(
            this.webRequest, param);

    assertThat(mapForPathVar.get("colors")).isEqualTo(Arrays.asList("red", "purple"));

    param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName()).arg(Map.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    Map<String, String> mapAll = (Map<String, String>)
            this.resolver.resolveArgument(this.webRequest, param);

    assertThat(mapAll.get("colors")).isEqualTo("red");
  }

  @Test
  public void resolveArgumentNoParams() throws Throwable {

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName())
            .arg(Map.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    Map<String, String> map = (Map<String, String>)
            this.resolver.resolveArgument(this.webRequest, param);

    assertThat(map).isEqualTo(Collections.emptyMap());
  }

  @Test
  public void resolveMultiValueMapArgumentNoParams() throws Throwable {

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noPathVar())
            .arg(MultiValueMap.class, String.class, String.class);

    Object result = this.resolver.resolveArgument(this.webRequest, param);

    assertThat(result).isInstanceOf(MultiValueMap.class)
            .asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
  }

  @Test
  public void resolveArgumentNoMatch() throws Throwable {
    MultiValueMap<String, String> params2 = getVariablesFor("planes");
    params2.add("colors", "yellow");
    params2.add("colors", "orange");

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().pathVar("cars"))
            .arg(MultiValueMap.class, String.class, String.class);

    @SuppressWarnings("unchecked")
    Map<String, String> map = (Map<String, String>)
            this.resolver.resolveArgument(this.webRequest, param);

    assertThat(map).isEqualTo(Collections.emptyMap());
  }

  private MultiValueMap<String, String> getVariablesFor(String pathVarName) {
    HandlerMatchingMetadata metadata = webRequest.matchingMetadata();

    Map<String, MultiValueMap<String, String>> matrixVariables = metadata.getMatrixVariables();

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    matrixVariables.put(pathVarName, params);
    return params;
  }

  @SuppressWarnings("unused")
  public void handle(String stringArg,
          @MatrixVariable Map<String, String> map,
          @MatrixVariable MultiValueMap<String, String> multivalueMap,
          @MatrixVariable(pathVar = "cars") MultiValueMap<String, String> mapForPathVar,
          @MatrixVariable("name") Map<String, String> mapWithName) {
  }

}