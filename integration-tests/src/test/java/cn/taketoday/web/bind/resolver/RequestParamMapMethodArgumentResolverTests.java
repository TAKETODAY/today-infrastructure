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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.ResolvableMethod;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.testfixture.MockMultipartFile;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockMultipartHttpServletRequest;
import cn.taketoday.mock.api.http.Part;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/18 16:38
 */
class RequestParamMapMethodArgumentResolverTests {

  private RequestParamMapMethodArgumentResolver resolver = new RequestParamMapMethodArgumentResolver();

  private MockHttpServletRequest request = new MockHttpServletRequest();

  private ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

  private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

  @Test
  public void supportsParameter() {
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class, String.class, String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class, String.class, String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().name("name")).arg(Map.class, String.class, String.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(Map.class, String.class, String.class);
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  public void resolveMapOfString() throws Throwable {
    String name = "foo";
    String value = "bar";
    request.addParameter(name, value);
    Map<String, String> expected = Collections.singletonMap(name, value);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class, String.class, String.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Map;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultiValueMapOfString() throws Throwable {
    String name = "foo";
    String value1 = "bar";
    String value2 = "baz";
    request.addParameter(name, value1, value2);

    MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
    expected.add(name, value1);
    expected.add(name, value2);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class, String.class, String.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof MultiValueMap;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resolveMapOfMultipartFile() throws Throwable {
    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    MultipartFile expected1 = new MockMultipartFile("mfile", "Hello World".getBytes());
    MultipartFile expected2 = new MockMultipartFile("other", "Hello World 3".getBytes());
    request.addFile(expected1);
    request.addFile(expected2);
    webRequest = new ServletRequestContext(null, request, null);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class, String.class, MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Map;
    assertThat(condition).isTrue();
    Map<String, MultipartFile> resultMap = (Map<String, MultipartFile>) result;
    assertThat(resultMap.size()).isEqualTo(2);
    assertThat(resultMap.get("mfile")).isEqualTo(expected1);
    assertThat(resultMap.get("other")).isEqualTo(expected2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resolveMultiValueMapOfMultipartFile() throws Throwable {
    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
    MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
    MultipartFile expected3 = new MockMultipartFile("other", "Hello World 3".getBytes());
    request.addFile(expected1);
    request.addFile(expected2);
    request.addFile(expected3);
    webRequest = new ServletRequestContext(null, request, null);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(MultiValueMap.class, String.class, MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof MultiValueMap;
    assertThat(condition).isTrue();
    MultiValueMap<String, MultipartFile> resultMap = (MultiValueMap<String, MultipartFile>) result;
    assertThat(resultMap.size()).isEqualTo(2);
    assertThat(resultMap.get("mfilelist").size()).isEqualTo(2);
    assertThat(resultMap.get("mfilelist").get(0)).isEqualTo(expected1);
    assertThat(resultMap.get("mfilelist").get(1)).isEqualTo(expected2);
    assertThat(resultMap.get("other").size()).isEqualTo(1);
    assertThat(resultMap.get("other").get(0)).isEqualTo(expected3);
  }

  public void handle(
          @RequestParam Map<String, String> param1,
          @RequestParam MultiValueMap<String, String> param2,
          @RequestParam Map<String, MultipartFile> param3,
          @RequestParam MultiValueMap<String, MultipartFile> param4,
          @RequestParam Map<String, Part> param5,
          @RequestParam MultiValueMap<String, Part> param6,
          @RequestParam("name") Map<String, String> param7,
          Map<String, String> param8) {
  }

}
