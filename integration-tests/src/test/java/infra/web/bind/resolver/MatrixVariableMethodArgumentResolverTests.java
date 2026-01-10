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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.ResolvableMethod;
import infra.web.annotation.MatrixVariable;
import infra.web.bind.RequestBindingException;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.testfixture.ReflectionTestUtils;
import infra.web.util.pattern.PathMatchInfo;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/28 22:08
 */
class MatrixVariableMethodArgumentResolverTests {

  private MatrixVariableMethodArgumentResolver resolver;

  private MockRequestContext webRequest;

  private final ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new MatrixVariableMethodArgumentResolver();

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

    assertThat(this.resolver.supportsParameter(
            this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName()).arg(List.class, String.class))).isTrue();

    assertThat(this.resolver.supportsParameter(
            this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().name("year")).arg(int.class))).isTrue();
  }

  @Test
  public void resolveArgument() throws Throwable {
    MultiValueMap<String, String> params = getVariablesFor("cars");
    params.add("colors", "red");
    params.add("colors", "green");
    params.add("colors", "blue");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName()).arg(List.class, String.class);

    assertThat(this.resolver.resolveArgument(this.webRequest, param)).isEqualTo(Arrays.asList("red", "green", "blue"));
  }

  @Test
  public void resolveArgumentPathVariable() throws Throwable {
    getVariablesFor("cars").add("year", "2006");
    getVariablesFor("bikes").add("year", "2005");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().name("year")).arg(int.class);

    assertThat(this.resolver.resolveArgument(this.webRequest, param)).isEqualTo(2006);
  }

  @Test
  public void resolveArgumentDefaultValue() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().name("year")).arg(int.class);
    assertThat(resolver.resolveArgument(this.webRequest, param)).isEqualTo(2013);
  }

  @Test
  public void resolveArgumentMultipleMatches() throws Throwable {
    getVariablesFor("var1").add("colors", "red");
    getVariablesFor("var2").add("colors", "green");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName()).arg(List.class, String.class);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            this.resolver.resolveArgument(this.webRequest, param));
  }

  @Test
  public void resolveArgumentRequired() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().noName()).arg(List.class, String.class);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            this.resolver.resolveArgument(this.webRequest, param));
  }

  @Test
  public void resolveArgumentNoMatch() throws Throwable {
    MultiValueMap<String, String> params = getVariablesFor("cars");
    params.add("anotherYear", "2012");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.matrixAttribute().name("year")).arg(int.class);

    assertThat(this.resolver.resolveArgument(this.webRequest, param)).isEqualTo(2013);
  }

  private MultiValueMap<String, String> getVariablesFor(String pathVarName) {
    HandlerMatchingMetadata metadata = webRequest.matchingMetadata();

    Map<String, MultiValueMap<String, String>> matrixVariables = metadata.getMatrixVariables();

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    matrixVariables.put(pathVarName, params);
    return params;
  }

  public void handle(String stringArg, @MatrixVariable List<String> colors,
          @MatrixVariable(name = "year", pathVar = "cars", required = false, defaultValue = "2013") int preferredYear) {

  }

}