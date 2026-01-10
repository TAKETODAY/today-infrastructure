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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.ResolvableMethod;
import infra.web.annotation.RequestParam;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.Part;
import infra.web.testfixture.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/18 16:38
 */
class RequestParamMapMethodArgumentResolverTests {

  private RequestParamMapMethodArgumentResolver resolver = new RequestParamMapMethodArgumentResolver();

  private HttpMockRequestImpl request = new HttpMockRequestImpl();

  private MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected1 = new MockMultipartFile("mfile", "Hello World".getBytes());
    Part expected2 = new MockMultipartFile("other", "Hello World 3".getBytes());
    request.addPart(expected1);
    request.addPart(expected2);
    webRequest = new MockRequestContext(null, request, null);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class, String.class, Part.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Map;
    assertThat(condition).isTrue();
    Map<String, Part> resultMap = (Map<String, Part>) result;
    assertThat(resultMap.size()).isEqualTo(2);
    assertThat(resultMap.get("mfile")).isEqualTo(expected1);
    assertThat(resultMap.get("other")).isEqualTo(expected2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resolveMultiValueMapOfMultipartFile() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
    Part expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
    Part expected3 = new MockMultipartFile("other", "Hello World 3".getBytes());
    request.addPart(expected1);
    request.addPart(expected2);
    request.addPart(expected3);
    webRequest = new MockRequestContext(null, request, null);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(MultiValueMap.class, String.class, Part.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof MultiValueMap;
    assertThat(condition).isTrue();
    MultiValueMap<String, Part> resultMap = (MultiValueMap<String, Part>) result;
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
          @RequestParam Map<String, Part> param3,
          @RequestParam MultiValueMap<String, Part> param4,
          @RequestParam("name") Map<String, String> param7,
          Map<String, String> param8) {
  }

}
