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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.http.server.RequestPath;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.ResolvableMethod;
import cn.taketoday.web.annotation.MatrixParam;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.ReflectionTestUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

import static cn.taketoday.web.bind.resolver.MvcAnnotationPredicates.matrixAttribute;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/28 21:05
 */
class MatrixParamMapMethodArgumentResolverTests {

  private MatrixParamMapMethodArgumentResolver resolver;

  private ServletRequestContext webRequest;

  private final ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new MatrixParamMapMethodArgumentResolver();

    BindingContext binding = new BindingContext();
    this.webRequest = new ServletRequestContext(null, new MockHttpServletRequest(), null);
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
          @MatrixParam Map<String, String> map,
          @MatrixParam MultiValueMap<String, String> multivalueMap,
          @MatrixParam(pathVar = "cars") MultiValueMap<String, String> mapForPathVar,
          @MatrixParam("name") Map<String, String> mapWithName) {
  }

}