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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.DeleteMapping;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PatchMapping;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.PutMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.handler.condition.ConsumesRequestCondition;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.view.PathPatternsParameterizedTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 15:45
 */
class RequestMappingHandlerMappingTests {

  @SuppressWarnings("unused")
  static Stream<Arguments> pathPatternsArguments() {
    RequestMappingHandlerMapping mapping1 = new RequestMappingHandlerMapping();
    StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
    mapping1.setApplicationContext(wac1);

    RequestMappingHandlerMapping mapping2 = new RequestMappingHandlerMapping();
    StaticWebApplicationContext wac2 = new StaticWebApplicationContext();
    mapping2.setApplicationContext(wac2);

    return Stream.of(Arguments.of(mapping1, wac1), Arguments.of(mapping2, wac2));
  }

  @Test
  void builderConfiguration() {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext());

    RequestMappingInfo.BuilderConfiguration config = mapping.getBuilderConfiguration();
    assertThat(config).isNotNull();

    mapping.afterPropertiesSet();
    assertThat(mapping.getBuilderConfiguration()).isNotNull().isSameAs(config);
  }

  @PathPatternsParameterizedTest
  void resolveEmbeddedValuesInPatterns(RequestMappingHandlerMapping mapping) {

    mapping.setEmbeddedValueResolver(
            value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value
    );

    String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
    String[] result = mapping.resolveEmbeddedValuesInPatterns(patterns);

    assertThat(result).isEqualTo(new String[] { "/foo", "/foo/bar" });
  }

  @PathPatternsParameterizedTest
  void pathPrefix(RequestMappingHandlerMapping mapping) throws Exception {

    mapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
    mapping.setPathPrefixes(Collections.singletonMap(
            "/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));
    mapping.afterPropertiesSet();

    Method method = UserController.class.getMethod("getUser");
    RequestMappingInfo info = mapping.getMappingForMethod(method, UserController.class);

    assertThat(info).isNotNull();
    assertThat(info.getPatternValues()).isEqualTo(Collections.singleton("/api/user/{id}"));
  }

  @PathPatternsParameterizedTest
    // gh-23907
  void pathPrefixPreservesPathMatchingSettings(RequestMappingHandlerMapping mapping) throws Exception {
    mapping.setPathPrefixes(Collections.singletonMap("/api", HandlerTypePredicate.forAnyHandlerType()));
    mapping.afterPropertiesSet();

    Method method = ComposedAnnotationController.class.getMethod("get");
    RequestMappingInfo info = mapping.getMappingForMethod(method, ComposedAnnotationController.class);

    assertThat(info).isNotNull();
    assertThat(info.getPathPatternsCondition()).isNotNull();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/get");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new ServletRequestContext(null, request, null))).isNotNull();

    request = new MockHttpServletRequest("GET", "/api/get.pdf");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new ServletRequestContext(null, request, null))).isNull();
  }

  @PathPatternsParameterizedTest
  void resolveRequestMappingViaComposedAnnotation(RequestMappingHandlerMapping mapping) {

    RequestMappingInfo info = assertComposedAnnotationMapping(
            mapping, "postJson", "/postJson", HttpMethod.POST);

    assertThat(info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString())
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(info.getProducesCondition().getProducibleMediaTypes().iterator().next().toString())
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
    // SPR-14988
  void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
    RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(HttpMethod.POST);

    ConsumesRequestCondition condition = requestMappingInfo.getConsumesCondition();
    assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));
  }

  @PathPatternsParameterizedTest
    // gh-22010
  void consumesWithOptionalRequestBody(RequestMappingHandlerMapping mapping, StaticWebApplicationContext wac) {
    wac.registerSingleton("testController", ComposedAnnotationController.class);
    wac.refresh();
    mapping.afterPropertiesSet();
    RequestMappingInfo result = mapping.getHandlerMethods().keySet().stream()
            .filter(info -> info.getPatternValues().equals(Collections.singleton("/post")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No /post"));

    assertThat(result.getConsumesCondition().isBodyRequired()).isFalse();
  }

  @Test
  void getMapping() throws Exception {
    assertComposedAnnotationMapping(HttpMethod.GET);
  }

  @Test
  void postMapping() throws Exception {
    assertComposedAnnotationMapping(HttpMethod.POST);
  }

  @Test
  void putMapping() throws Exception {
    assertComposedAnnotationMapping(HttpMethod.PUT);
  }

  @Test
  void deleteMapping() throws Exception {
    assertComposedAnnotationMapping(HttpMethod.DELETE);
  }

  @Test
  void patchMapping() throws Exception {
    assertComposedAnnotationMapping(HttpMethod.PATCH);
  }

  private RequestMappingInfo assertComposedAnnotationMapping(HttpMethod requestMethod) throws Exception {

    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext());

    String methodName = requestMethod.name().toLowerCase();
    String path = "/" + methodName;

    return assertComposedAnnotationMapping(mapping, methodName, path, requestMethod);
  }

  private RequestMappingInfo assertComposedAnnotationMapping(
          RequestMappingHandlerMapping mapping, String methodName, String path, HttpMethod requestMethod) {

    Class<?> clazz = ComposedAnnotationController.class;
    Method method = ReflectionUtils.getMethod(clazz, methodName, (Class<?>[]) null);
    RequestMappingInfo info = mapping.getMappingForMethod(method, clazz);

    assertThat(info).isNotNull();

    Set<String> paths = info.getPatternValues();
    assertThat(paths.size()).isEqualTo(1);
    assertThat(paths.iterator().next()).isEqualTo(path);

    Set<HttpMethod> methods = info.getMethodsCondition().getMethods();
    assertThat(methods.size()).isEqualTo(1);
    assertThat(methods.iterator().next()).isEqualTo(requestMethod);

    return info;
  }

  @Controller
  @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  static class ComposedAnnotationController {

    @RequestMapping
    public void handle() {
    }

    @PostJson("/postJson")
    public void postJson() {
    }

    @GetMapping("/get")
    public void get() {
    }

    @PostMapping(path = "/post", consumes = MediaType.APPLICATION_XML_VALUE)
    public void post(@RequestBody(required = false) Foo foo) {
    }

    @PutMapping("/put")
    public void put() {
    }

    @DeleteMapping("/delete")
    public void delete() {
    }

    @PatchMapping("/patch")
    public void patch() {
    }

  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,
               consumes = MediaType.APPLICATION_JSON_VALUE)
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface PostJson {

    @AliasFor(annotation = ActionMapping.class)
    String[] value() default {};
  }

  @RestController
  @RequestMapping("/user")
  static class UserController {

    @GetMapping("/{id}")
    public Principal getUser() {
      return mock(Principal.class);
    }
  }

  private static class Foo {
  }

}
