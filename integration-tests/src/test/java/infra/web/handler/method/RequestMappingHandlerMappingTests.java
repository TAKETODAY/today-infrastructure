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

package infra.web.handler.method;

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

import infra.core.annotation.AliasFor;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.stereotype.Controller;
import infra.util.CollectionUtils;
import infra.util.ReflectionUtils;
import infra.web.annotation.DeleteMapping;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PatchMapping;
import infra.web.annotation.PostMapping;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.handler.condition.ConsumesRequestCondition;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.service.annotation.HttpExchange;
import infra.web.service.annotation.PostExchange;
import infra.web.service.annotation.PutExchange;
import infra.web.util.pattern.PathPattern;
import infra.web.view.PathPatternsParameterizedTest;

import static infra.http.HttpMethod.POST;
import static infra.web.handler.method.RequestMappingInfo.paths;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 15:45
 */
class RequestMappingHandlerMappingTests {

  @SuppressWarnings("unused")
  static Stream<Arguments> pathPatternsArguments() {
    RequestMappingHandlerMapping mapping1 = new RequestMappingHandlerMapping();
    StaticWebApplicationContext wac1 = new StaticWebApplicationContext(new MockContextImpl());
    mapping1.setApplicationContext(wac1);

    RequestMappingHandlerMapping mapping2 = new RequestMappingHandlerMapping();
    StaticWebApplicationContext wac2 = new StaticWebApplicationContext(new MockContextImpl());
    mapping2.setApplicationContext(wac2);

    return Stream.of(Arguments.of(mapping1, wac1), Arguments.of(mapping2, wac2));
  }

  @Test
  void builderConfiguration() {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext(new MockContextImpl()));

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

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/api/get");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(null, request, null))).isNotNull();

    request = new HttpMockRequestImpl("GET", "/api/get.pdf");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(null, request, null))).isNull();
  }

  @PathPatternsParameterizedTest
  void resolveRequestMappingViaComposedAnnotation(RequestMappingHandlerMapping mapping) {

    RequestMappingInfo info = assertComposedAnnotationMapping(
            mapping, "postJson", "/postJson", POST);

    assertThat(info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString())
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(CollectionUtils.firstElement(info.getProducesCondition().getProducibleMediaTypes()).toString())
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
    RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(POST);

    ConsumesRequestCondition condition = requestMappingInfo.getConsumesCondition();
    assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));
  }

  @PathPatternsParameterizedTest
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
    assertComposedAnnotationMapping(POST);
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

  @Test
  void httpExchangeWithMultipleAnnotationsAtClassLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = MultipleClassLevelAnnotationsHttpExchangeController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    assertThatIllegalStateException()
            .isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
            .withMessageContainingAll(
                    "Multiple @HttpExchange annotations found on " + controllerClass,
                    HttpExchange.class.getSimpleName(),
                    ExtraHttpExchange.class.getSimpleName()
            );
  }

  @Test
  void httpExchangeWithMultipleAnnotationsAtMethodLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = MultipleMethodLevelAnnotationsHttpExchangeController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    assertThatIllegalStateException()
            .isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
            .withMessageContainingAll(
                    "Multiple @HttpExchange annotations found on " + method,
                    PostExchange.class.getSimpleName(),
                    PutExchange.class.getSimpleName()
            );
  }

  @Test
  void httpExchangeWithMixedAnnotationsAtClassLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = MixedClassLevelAnnotationsController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    assertThatIllegalStateException()
            .isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
            .withMessageContainingAll(
                    controllerClass.getName(),
                    "is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
                    RequestMapping.class.getSimpleName(),
                    HttpExchange.class.getSimpleName()
            );
  }

  @Test
  void httpExchangeWithMixedAnnotationsAtMethodLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = MixedMethodLevelAnnotationsController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    assertThatIllegalStateException()
            .isThrownBy(() -> mapping.getMappingForMethod(method, controllerClass))
            .withMessageContainingAll(
                    method.toString(),
                    "is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed:",
                    PostMapping.class.getSimpleName(),
                    PostExchange.class.getSimpleName()
            );
  }

  @Test
  void httpExchangeAnnotationsOverriddenAtClassLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = ClassLevelOverriddenHttpExchangeAnnotationsController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);

    assertThat(info).isNotNull();
    assertThat(info.getPathPatternsCondition()).isNotNull();

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/service/postExchange");
    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(request))).isNull();

    request = new HttpMockRequestImpl("POST", "/controller/postExchange");
    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(request))).isNotNull();
  }

  @Test
  void httpExchangeAnnotationsOverriddenAtMethodLevel() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = MethodLevelOverriddenHttpExchangeAnnotationsController.class;
    Method method = controllerClass.getDeclaredMethod("post");

    RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);

    assertThat(info).isNotNull();
    assertThat(info.getPathPatternsCondition()).isNotNull();

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/service/postExchange");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(request))).isNull();

    request = new HttpMockRequestImpl("POST", "/controller/postMapping");

    assertThat(info.getPathPatternsCondition().getMatchingCondition(new MockRequestContext(request))).isNotNull();
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void httpExchangeWithDefaultValues() throws NoSuchMethodException {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext(new MockContextImpl()));
    mapping.afterPropertiesSet();

    RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
            HttpExchangeController.class.getMethod("defaultValuesExchange"),
            HttpExchangeController.class);

    assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
            .extracting(PathPattern::toString)
            .containsOnly("/exchange");

    assertThat(mappingInfo.getMethodsCondition().getMethods()).isEmpty();
    assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();
    assertThat(mappingInfo.getHeadersCondition().getExpressions()).isEmpty();
    assertThat(mappingInfo.getConsumesCondition().getExpressions()).isEmpty();
    assertThat(mappingInfo.getProducesCondition().getExpressions()).isEmpty();
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void httpExchangeWithCustomValues() throws Exception {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext(new MockContextImpl()));
    mapping.afterPropertiesSet();

    RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
            HttpExchangeController.class.getMethod("customValuesExchange"),
            HttpExchangeController.class);

    assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
            .extracting(PathPattern::toString)
            .containsOnly("/exchange/custom");

    assertThat(mappingInfo.getMethodsCondition().getMethods()).containsOnly(POST);
    assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();
    assertThat(mappingInfo.getHeadersCondition().getExpressions()).isEmpty();

    assertThat(mappingInfo.getConsumesCondition().getExpressions())
            .extracting("mediaType")
            .containsOnly(MediaType.APPLICATION_JSON);

    assertThat(mappingInfo.getProducesCondition().getExpressions())
            .extracting("mediaType")
            .containsOnly(MediaType.valueOf("text/plain;charset=UTF-8"));
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void httpExchangeWithCustomHeaders() throws Exception {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext(new MockContextImpl()));
    mapping.afterPropertiesSet();

    RequestMappingInfo mappingInfo = mapping.getMappingForMethod(
            HttpExchangeController.class.getMethod("customHeadersExchange"),
            HttpExchangeController.class);

    assertThat(mappingInfo.getPathPatternsCondition().getPatterns())
            .extracting(PathPattern::toString)
            .containsOnly("/exchange/headers");

    assertThat(mappingInfo.getMethodsCondition().getMethods()).containsOnly(HttpMethod.GET);
    assertThat(mappingInfo.getParamsCondition().getExpressions()).isEmpty();

    assertThat(mappingInfo.getHeadersCondition().getExpressions().stream().map(Object::toString))
            .containsExactly("h1=hv1", "!h2");
  }

  @Test
  void requestBodyAnnotationFromInterfaceIsRespected() throws Exception {
    String path = "/controller/postMapping";
    MediaType mediaType = MediaType.APPLICATION_JSON;

    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = InterfaceControllerImpl.class;
    Method method = controllerClass.getDeclaredMethod("post", Foo.class);

    RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);
    assertThat(info).isNotNull();

    // Original ConsumesCondition
    ConsumesRequestCondition consumesCondition = info.getConsumesCondition();
    assertThat(consumesCondition).isNotNull();
    assertThat(consumesCondition.isBodyRequired()).isTrue();
    assertThat(consumesCondition.getConsumableMediaTypes()).containsOnly(mediaType);

    mapping.registerHandlerMethod(new InterfaceControllerImpl(), method, info);

    // Updated ConsumesCondition
    consumesCondition = info.getConsumesCondition();
    assertThat(consumesCondition).isNotNull();
    assertThat(consumesCondition.isBodyRequired()).isFalse();
    assertThat(consumesCondition.getConsumableMediaTypes()).containsOnly(mediaType);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", path);

    request.setContentType(mediaType.toString());

    RequestMappingInfo matchingInfo = info.getMatchingCondition(new MockRequestContext(request));
    // Since the request has no body AND the required flag is false, the
    // ConsumesCondition in the matching condition in an EMPTY_CONDITION.
    // In other words, the "consumes" expressions are removed.
    assertThat(matchingInfo).isEqualTo(paths(path).methods(POST).build());
  }

  @Test
  void requestBodyAnnotationFromImplementationOverridesInterface() throws Exception {
    String path = "/controller/postMapping";
    MediaType mediaType = MediaType.APPLICATION_JSON;

    RequestMappingHandlerMapping mapping = createMapping();

    Class<?> controllerClass = InterfaceControllerImplOverridesRequestBody.class;
    Method method = controllerClass.getDeclaredMethod("post", Foo.class);

    RequestMappingInfo info = mapping.getMappingForMethod(method, controllerClass);
    assertThat(info).isNotNull();

    // Original ConsumesCondition
    ConsumesRequestCondition consumesCondition = info.getConsumesCondition();
    assertThat(consumesCondition).isNotNull();
    assertThat(consumesCondition.isBodyRequired()).isTrue();
    assertThat(consumesCondition.getConsumableMediaTypes()).containsOnly(mediaType);

    mapping.registerHandlerMethod(new InterfaceControllerImplOverridesRequestBody(), method, info);

    // Updated ConsumesCondition
    consumesCondition = info.getConsumesCondition();
    assertThat(consumesCondition).isNotNull();
    assertThat(consumesCondition.isBodyRequired()).isTrue();
    assertThat(consumesCondition.getConsumableMediaTypes()).containsOnly(mediaType);

    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", path);
    request.setContentType(mediaType.toString());

    RequestMappingInfo matchingInfo = info.getMatchingCondition(new MockRequestContext(request));
    RequestMappingInfo expected = paths(path).methods(POST).consumes(mediaType.toString()).build();
    assertThat(matchingInfo).isEqualTo(expected);
  }

  private static RequestMappingHandlerMapping createMapping() {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticWebApplicationContext());
    mapping.afterPropertiesSet();
    return mapping;
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

    @AliasFor(annotation = RequestMapping.class)
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

  @RestController
  @HttpExchange("/exchange")
  static class HttpExchangeController {

    @HttpExchange
    public void defaultValuesExchange() { }

    @HttpExchange(value = "/custom", accept = "text/plain;charset=UTF-8",
            method = "POST", contentType = "application/json")
    public void customValuesExchange() { }

    @HttpExchange(method = "GET", url = "/headers",
            headers = { "h1=hv1", "!h2", "Accept=application/ignored" })
    public String customHeadersExchange() {
      return "info";
    }
  }

  @HttpExchange("/exchange")
  @ExtraHttpExchange
  static class MultipleClassLevelAnnotationsHttpExchangeController {

    @PostExchange("/post")
    void post() { }
  }

  static class MultipleMethodLevelAnnotationsHttpExchangeController {

    @PostExchange("/post")
    @PutExchange("/post")
    void post() { }
  }

  @Controller
  @RequestMapping("/api")
  @HttpExchange("/api")
  static class MixedClassLevelAnnotationsController {

    @PostExchange("/post")
    void post() { }
  }

  @Controller
  @RequestMapping("/api")
  static class MixedMethodLevelAnnotationsController {

    @PostMapping("/post")
    @PostExchange("/post")
    void post() { }
  }

  @HttpExchange("/service")
  interface Service {

    @PostExchange("/postExchange")
    void post();

  }

  @Controller
  @RequestMapping("/controller")
  static class ClassLevelOverriddenHttpExchangeAnnotationsController implements Service {

    @Override
    public void post() { }
  }

  @Controller
  @RequestMapping("/controller")
  static class MethodLevelOverriddenHttpExchangeAnnotationsController implements Service {

    @PostMapping("/postMapping")
    @Override
    public void post() { }
  }

  @RestController
  @RequestMapping(path = "/controller", consumes = "application/json")
  interface InterfaceController {

    @PostMapping("/postMapping")
    void post(@RequestBody(required = false) Foo foo);
  }

  static class InterfaceControllerImpl implements InterfaceController {

    @Override
    public void post(Foo foo) { }
  }

  static class InterfaceControllerImplOverridesRequestBody implements InterfaceController {

    @Override
    public void post(@RequestBody(required = true) Foo foo) { }
  }

  @HttpExchange
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface ExtraHttpExchange {
  }

  private static class Foo {
  }
}
