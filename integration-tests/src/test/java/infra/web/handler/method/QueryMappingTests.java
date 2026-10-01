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

package infra.web.handler.method;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.annotation.MergedAnnotations;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.web.annotation.QUERY;
import infra.web.annotation.QueryMapping;
import infra.web.annotation.RequestMapping;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QueryMapping} and {@link QUERY} request mappings.
 *
 * @author TODAY
 */
class QueryMappingTests {

  @ParameterizedTest
  @ValueSource(classes = { QueryMappingController.class, ShortQueryController.class })
  void defaultAttributes(Class<?> controllerType) throws Exception {
    RequestMapping mapping = MergedAnnotations.from(controllerType.getDeclaredMethod("search"))
            .get(RequestMapping.class).synthesize();

    assertThat(mapping.method()).containsExactly(HttpMethod.QUERY);
    assertThat(mapping.value()).containsExactly("/search");
    assertThat(mapping.path()).containsExactly("/search");
    assertThat(mapping.name()).isEmpty();
    assertThat(mapping.combine()).isTrue();
    assertThat(mapping.params()).isEmpty();
    assertThat(mapping.headers()).isEmpty();
    assertThat(mapping.consumes()).isEmpty();
    assertThat(mapping.produces()).isEmpty();
    assertThat(mapping.version()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(classes = { QueryMappingController.class, ShortQueryController.class })
  void customAttributesAreAliasedToRequestMapping(Class<?> controllerType) throws Exception {
    RequestMapping mapping = MergedAnnotations.from(controllerType.getDeclaredMethod("custom"))
            .get(RequestMapping.class).synthesize();

    assertThat(mapping.method()).containsExactly(HttpMethod.QUERY);
    assertThat(mapping.name()).isEqualTo("customQuery");
    assertThat(mapping.value()).containsExactly("/custom", "/alternative");
    assertThat(mapping.path()).containsExactly("/custom", "/alternative");
    assertThat(mapping.combine()).isFalse();
    assertThat(mapping.params()).containsExactly("q");
    assertThat(mapping.headers()).containsExactly("X-Query=true");
    assertThat(mapping.consumes()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    assertThat(mapping.produces()).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    assertThat(mapping.version()).isEqualTo("1.0");
  }

  @ParameterizedTest
  @ValueSource(classes = { QueryMappingController.class, ShortQueryController.class })
  void combinesTypeAndMethodMappingsAndMatchesOnlyQueryRequests(Class<?> controllerType) throws Exception {
    try (var context = new AnnotationConfigApplicationContext(Object.class)) {
      RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
      mapping.setApplicationContext(context);
      mapping.afterPropertiesSet();

      for (String methodName : new String[] { "search", "inherited" }) {
        RequestMappingInfo info = mapping.getMappingForMethod(
                controllerType.getDeclaredMethod(methodName), controllerType);

        assertThat(info).isNotNull();
        String path = "/api/" + methodName;
        assertThat(info.getPatternValues()).containsExactly(path);
        assertThat(info.getMethodsCondition().getMethods()).containsExactly(HttpMethod.QUERY);
        assertThat(info.getMatchingCondition(new MockHttpContext(new MockRequest("QUERY", path)))).isNotNull();
        assertThat(info.getMatchingCondition(new MockHttpContext(new MockRequest("QUERY", "/other")))).isNull();

        for (HttpMethod method : HttpMethod.values()) {
          if (method != HttpMethod.QUERY) {
            assertThat(info.getMatchingCondition(new MockHttpContext(new MockRequest(method.name(), path))))
                    .as("%s must not match the QUERY mapping", method).isNull();
          }
        }
      }
    }
  }

  @QueryMapping("/api")
  static class QueryMappingController {

    @QueryMapping("/search")
    void search() {
    }

    @RequestMapping("/inherited")
    void inherited() {
    }

    @QueryMapping(name = "customQuery", path = { "/custom", "/alternative" }, combine = false,
            params = "q", headers = "X-Query=true", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE, version = "1.0")
    void custom() {
    }
  }

  @QUERY("/api")
  static class ShortQueryController {

    @QUERY("/search")
    void search() {
    }

    @RequestMapping("/inherited")
    void inherited() {
    }

    @QUERY(name = "customQuery", path = { "/custom", "/alternative" }, combine = false,
            params = "q", headers = "X-Query=true", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE, version = "1.0")
    void custom() {
    }
  }

}
