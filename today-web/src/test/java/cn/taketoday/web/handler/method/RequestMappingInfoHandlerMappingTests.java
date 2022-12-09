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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.UnsatisfiedRequestParameterException;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.MappedInterceptor;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.UrlPathHelper;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.view.PathPatternsParameterizedTest;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/9 20:00
 */
class RequestMappingInfoHandlerMappingTests {
  static final String HandlerMatchingMetadataKey = "HandlerMatchingMetadata";

  @SuppressWarnings("unused")
  static Stream<?> pathPatternsArguments() {
    TestController controller = new TestController();

    TestRequestMappingInfoHandlerMapping mapping1 = new TestRequestMappingInfoHandlerMapping();

    UrlPathHelper pathHelper = new UrlPathHelper();
    pathHelper.setRemoveSemicolonContent(false);

    TestRequestMappingInfoHandlerMapping mapping2 = new TestRequestMappingInfoHandlerMapping();

    return Stream.of(mapping1, mapping2).peek(mapping -> {
      mapping.setApplicationContext(new StaticWebApplicationContext());
      mapping.registerHandler(controller);
      mapping.afterPropertiesSet();
    });
  }

  private HandlerMethod fooMethod;

  private HandlerMethod fooParamMethod;

  private HandlerMethod barMethod;

  private HandlerMethod emptyMethod;

  @BeforeEach
  void setup() throws Exception {
    TestController controller = new TestController();
    this.fooMethod = new HandlerMethod(controller, "foo");
    this.fooParamMethod = new HandlerMethod(controller, "fooParam");
    this.barMethod = new HandlerMethod(controller, "bar");
    this.emptyMethod = new HandlerMethod(controller, "empty");
  }

  @PathPatternsParameterizedTest
  void getDirectPaths(TestRequestMappingInfoHandlerMapping mapping) {
    String[] patterns = { "/foo/*", "/foo", "/bar/*", "/bar" };
    RequestMappingInfo info = mapping.createInfo(patterns);
    Set<String> actual = mapping.getDirectPaths(info);

    assertThat(actual).contains("/foo", "/bar");
    assertThat(info.getPatternValues()).isEqualTo(new HashSet<>(Arrays.asList(patterns)));
  }

  @PathPatternsParameterizedTest
  void getHandlerDirectMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    HandlerMethod handlerMethod = getHandler(mapping, request);

    assertThat(handlerMethod.getMethod()).isEqualTo(this.fooMethod.getMethod());
  }

  @PathPatternsParameterizedTest
  void getHandlerGlobMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bar");
    HandlerMethod handlerMethod = getHandler(mapping, request);
    assertThat(handlerMethod.getMethod()).isEqualTo(this.barMethod.getMethod());
  }

  @PathPatternsParameterizedTest
  void getHandlerEmptyPathMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
    HandlerMethod handlerMethod = getHandler(mapping, request);

    assertThat(handlerMethod.getMethod()).isEqualTo(this.emptyMethod.getMethod());
  }

  @PathPatternsParameterizedTest
  void getHandlerBestMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    request.setParameter("p", "anything");
    HandlerMethod handlerMethod = getHandler(mapping, request);

    assertThat(handlerMethod.getMethod()).isEqualTo(this.fooParamMethod.getMethod());
  }

  @PathPatternsParameterizedTest
  void getHandlerHttpMethodNotAllowed(TestRequestMappingInfoHandlerMapping mapping) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bar");
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());
    assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class)
            .isThrownBy(() -> mapping.getHandler(context))
            .satisfies(ex -> assertThat(ex.getSupportedMethods()).containsExactly("GET", "HEAD"));
  }

  @PathPatternsParameterizedTest
  void getHandlerHttpMethodMatchFalsePositive(TestRequestMappingInfoHandlerMapping mapping) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
    request.addHeader("Accept", "application/xml");

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    mapping.registerHandler(new UserController());
    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> mapping.getHandler(context));
  }

  @PathPatternsParameterizedTest
  void getHandlerMediaTypeNotSupported(TestRequestMappingInfoHandlerMapping mapping) {
    testHttpMediaTypeNotSupportedException(mapping, "/person/1");
    testHttpMediaTypeNotSupportedException(mapping, "/person/1.json");
  }

  @PathPatternsParameterizedTest
  void getHandlerHttpOptions(TestRequestMappingInfoHandlerMapping mapping) throws Throwable {
    testHttpOptions(mapping, "/foo", "GET,HEAD,OPTIONS", null);
    testHttpOptions(mapping, "/person/1", "PUT,OPTIONS", null);
    testHttpOptions(mapping, "/persons", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS,CONNECT", null);
    testHttpOptions(mapping, "/something", "PUT,POST", null);
    testHttpOptions(mapping, "/qux", "PATCH,GET,HEAD,OPTIONS", new MediaType("foo", "bar"));
  }

  @PathPatternsParameterizedTest
  void getHandlerTestInvalidContentType(TestRequestMappingInfoHandlerMapping mapping) {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/person/1");
    request.setContentType("bogus");

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());
    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> mapping.getHandler(context))
            .withMessage("Invalid mime type \"bogus\": does not contain '/'");
  }

  @PathPatternsParameterizedTest
    // SPR-8462
  void getHandlerMediaTypeNotAccepted(TestRequestMappingInfoHandlerMapping mapping) {
    testHttpMediaTypeNotAcceptableException(mapping, "/persons");
    if (mapping.getPatternParser() == null) {
      testHttpMediaTypeNotAcceptableException(mapping, "/persons.json");
    }
  }

  @PathPatternsParameterizedTest
  void getHandlerUnsatisfiedRequestParameterException(TestRequestMappingInfoHandlerMapping mapping) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/params");
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    assertThatExceptionOfType(UnsatisfiedRequestParameterException.class)
            .isThrownBy(() -> mapping.getHandler(context))
            .satisfies(ex -> assertThat(ex.getParamConditionGroups().stream().map(group -> group[0]))
                    .containsExactlyInAnyOrder("foo=bar", "bar=baz"));
  }

  @PathPatternsParameterizedTest
  void getHandlerProducibleMediaTypesAttribute(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/content");
    request.addHeader("Accept", "application/xml");

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    mapping.getHandler(context);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(context.hasMatchingMetadata()).isTrue();
    assertThat(matchingMetadata).isNotNull();
    MediaType[] producibleMediaTypes = matchingMetadata.getProducibleMediaTypes();
    assertThat(producibleMediaTypes).isNotNull().contains(MediaType.APPLICATION_XML);

    request = new MockHttpServletRequest("GET", "/content");
    context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    request.addHeader("Accept", "application/json");
    mapping.getHandler(context);
    matchingMetadata = context.getMatchingMetadata();

    assertThat(context.hasMatchingMetadata()).isTrue();
    assertThat(matchingMetadata).isNotNull();
    assertThat(matchingMetadata.getProducibleMediaTypes())
            .as("Negated expression shouldn't be listed as producible type").isNull();
  }

  @Test
  void getHandlerMappedInterceptors() throws Exception {
    String path = "/foo";
    HandlerInterceptor interceptor = new HandlerInterceptor() { };
    MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { path }, interceptor);

    TestRequestMappingInfoHandlerMapping mapping = new TestRequestMappingInfoHandlerMapping();
    mapping.registerHandler(new TestController());
    mapping.setInterceptors(mappedInterceptor);
    mapping.setApplicationContext(new StaticWebApplicationContext());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    Object handler = mapping.getHandler(context);
    assertThat(handler).isNotNull()
            .isInstanceOf(HandlerExecutionChain.class);

    HandlerExecutionChain chain = (HandlerExecutionChain) handler;
    assertThat(chain.getInterceptors()).isNotEmpty().containsExactly(interceptor);

    request = new MockHttpServletRequest("GET", "/invalid");
    context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    handler = mapping.getHandler(context);
    assertThat(handler).isNull();
  }

  @SuppressWarnings("unchecked")
  @PathPatternsParameterizedTest
  void handleMatchUriTemplateVariables(TestRequestMappingInfoHandlerMapping mapping) {
    RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/{path2}").build();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
    String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    var match = getMappingInfoMatch(mapping, key);

    mapping.handleMatch(match, lookupPath, context);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(matchingMetadata).isNotNull();
    assertThat(context.hasMatchingMetadata()).isTrue();

    Map<String, String> uriVariables = matchingMetadata.getUriVariables();

    assertThat(uriVariables).isNotNull();
    assertThat(uriVariables.get("path1")).isEqualTo("1");
    assertThat(uriVariables.get("path2")).isEqualTo("2");
  }

  @SuppressWarnings("unchecked")
  @PathPatternsParameterizedTest
  void handleMatchUriTemplateVariablesDecode(TestRequestMappingInfoHandlerMapping mapping) {
    RequestMappingInfo key = RequestMappingInfo.paths("/{group}/{identifier}").build();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/group/a%2Fb");

    UrlPathHelper pathHelper = new UrlPathHelper();
    pathHelper.setUrlDecode(false);
    String lookupPath = pathHelper.getLookupPathForRequest(request);

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    var match = getMappingInfoMatch(mapping, key);

    mapping.handleMatch(match, lookupPath, context);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(matchingMetadata).isNotNull();
    assertThat(context.hasMatchingMetadata()).isTrue();

    Map<String, String> uriVariables = matchingMetadata.getUriVariables();

    assertThat(uriVariables).isNotNull();
    assertThat(uriVariables.get("group")).isEqualTo("group");
    assertThat(uriVariables.get("identifier")).isEqualTo("a/b");
  }

  @PathPatternsParameterizedTest
  void handleMatchBestMatchingPatternAttribute(TestRequestMappingInfoHandlerMapping mapping) {
    RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/2", "/**").build();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    var infoMatch = getMappingInfoMatch(mapping, key);
    mapping.handleMatch(infoMatch, "/1/2", context);
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(matchingMetadata).isNotNull();
    assertThat(context.hasMatchingMetadata()).isTrue();

    assertThat(matchingMetadata.getBestMatchingPattern().toString()).isEqualTo("/{path1}/2");
  }

  @PathPatternsParameterizedTest
  void handleMatchBestMatchingPatternAttributeNoPatternsDefined(TestRequestMappingInfoHandlerMapping mapping) {
    String path = "";
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    RequestMappingInfo build = RequestMappingInfo.paths().build();
    var match = getMappingInfoMatch(mapping, build);

    mapping.handleMatch(match, path, context);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(matchingMetadata).isNotNull();
    assertThat(context.hasMatchingMetadata()).isTrue();

    assertThat(matchingMetadata.getBestMatchingPattern().toString()).isEqualTo(path);
  }

  @PathPatternsParameterizedTest
  void handleMatchMatrixVariables(TestRequestMappingInfoHandlerMapping mapping) {
    MockHttpServletRequest request;
    MultiValueMap<String, String> matrixVariables;
    Map<String, String> uriVariables;

    // URI var parsed into path variable + matrix params..
    request = new MockHttpServletRequest("GET", "/cars;colors=red,blue,green;year=2012");
    handleMatch(mapping, request, "/{cars}", request.getRequestURI());

    matrixVariables = getMatrixVariables(request, "cars");
    uriVariables = getUriTemplateVariables(request);

    assertThat(matrixVariables).isNotNull();
    assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
    assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
    assertThat(uriVariables.get("cars")).isEqualTo("cars");

    // URI var with regex for path variable, and URI var for matrix params.
    request = new MockHttpServletRequest("GET", "/cars;colors=red,blue,green;year=2012");
    handleMatch(mapping, request, "/{cars:[^;]+}{params}", request.getRequestURI());

    matrixVariables = getMatrixVariables(request, "params");
    uriVariables = getUriTemplateVariables(request);

    assertThat(matrixVariables).isNotNull();
    assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
    assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
    assertThat(uriVariables.get("cars")).isEqualTo("cars");
    if (mapping.getPatternParser() == null) {
      assertThat(uriVariables.get("params")).isEqualTo(";colors=red,blue,green;year=2012");
    }

    // URI var with regex for path variable, and (empty) URI var for matrix params.
    request = new MockHttpServletRequest("GET", "/cars");
    handleMatch(mapping, request, "/{cars:[^;]+}{params}", request.getRequestURI());

    matrixVariables = getMatrixVariables(request, "params");
    uriVariables = getUriTemplateVariables(request);

    assertThat(matrixVariables).isNull();
    assertThat(uriVariables.get("cars")).isEqualTo("cars");
    assertThat(uriVariables.get("params")).isEqualTo("");

    // SPR-11897
    request = new MockHttpServletRequest("GET", "/a=42;b=c");
    handleMatch(mapping, request, "/{foo}", request.getRequestURI());

    matrixVariables = getMatrixVariables(request, "foo");
    uriVariables = getUriTemplateVariables(request);

    assertThat(matrixVariables).isNotNull();
    if (mapping.getPatternParser() != null) {
      assertThat(matrixVariables).hasSize(1);
      assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
      assertThat(uriVariables.get("foo")).isEqualTo("a=42");
    }
    else {
      assertThat(matrixVariables).hasSize(2);
      assertThat(matrixVariables.getFirst("a")).isEqualTo("42");
      assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
      assertThat(uriVariables.get("foo")).isEqualTo("a=42");
    }
  }

  @PathPatternsParameterizedTest
  void handleMatchMatrixVariablesDecoding(TestRequestMappingInfoHandlerMapping mapping) {

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cars;mvar=a%2Fb");
    handleMatch(mapping, request, "/{cars}", request.getRequestURI());

    MultiValueMap<String, String> matrixVariables = getMatrixVariables(request, "cars");
    Map<String, String> uriVariables = getUriTemplateVariables(request);

    assertThat(matrixVariables).isNotNull();
    assertThat(matrixVariables.get("mvar")).isEqualTo(Collections.singletonList("a/b"));
    assertThat(uriVariables.get("cars")).isEqualTo("cars");
  }

  @PathPatternsParameterizedTest
  void handleNoMatchWithoutPartialMatches(TestRequestMappingInfoHandlerMapping mapping) {
    String path = "/non-existent";
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    HandlerMethod handlerMethod = mapping.handleNoMatch(new HashSet<>(), path, context);
    assertThat(handlerMethod).isNull();

    handlerMethod = mapping.handleNoMatch(null, path, context);
    assertThat(handlerMethod).isNull();
  }

  private HandlerMethod getHandler(
          TestRequestMappingInfoHandlerMapping mapping, MockHttpServletRequest request) throws Exception {
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    Object handler = mapping.getHandler(context);
    assertThat(handler).isNotNull().isInstanceOf(HandlerExecutionChain.class);
    HandlerExecutionChain chain = (HandlerExecutionChain) handler;
    return (HandlerMethod) chain.getHandler();
  }

  private void testHttpMediaTypeNotSupportedException(TestRequestMappingInfoHandlerMapping mapping, String url) {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", url);
    request.setContentType("application/json");
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> mapping.getHandler(context))
            .satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
  }

  private void testHttpOptions(TestRequestMappingInfoHandlerMapping mapping, String requestURI,
          String allowHeader, @Nullable MediaType acceptPatch) throws Throwable {

    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", requestURI);
    HandlerMethod handlerMethod = getHandler(mapping, request);

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    context.setBindingContext(new BindingContext());
    Object result = new InvocableHandlerMethod(handlerMethod, new ResolvableParameterFactory())
            .invokeForRequest(context);

    assertThat(result).isNotNull().isInstanceOf(HttpHeaders.class);
    HttpHeaders headers = (HttpHeaders) result;
    Set<HttpMethod> allowedMethods = Arrays.stream(allowHeader.split(","))
            .map(HttpMethod::valueOf)
            .collect(Collectors.toSet());
    assertThat(headers.getAllow()).hasSameElementsAs(allowedMethods);

    if (acceptPatch != null && headers.getAllow().contains(HttpMethod.PATCH)) {
      assertThat(headers.getAcceptPatch()).containsExactly(acceptPatch);
    }
  }

  private void testHttpMediaTypeNotAcceptableException(TestRequestMappingInfoHandlerMapping mapping, String url) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", url);
    request.addHeader("Accept", "application/json");
    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() ->
                    mapping.getHandler(context))
            .satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
  }

  private void handleMatch(TestRequestMappingInfoHandlerMapping mapping,
          MockHttpServletRequest request, String pattern, String lookupPath) {

    var context = new ServletRequestContext(null, request, new MockHttpServletResponse());

    RequestMappingInfo info = mapping.createInfo(pattern);
    var match = getMappingInfoMatch(mapping, info);
    mapping.handleMatch(match, lookupPath, context);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    assertThat(matchingMetadata).isNotNull();
    assertThat(context.hasMatchingMetadata()).isTrue();

    request.setAttribute(HandlerMatchingMetadataKey, matchingMetadata);
  }

  private static AbstractHandlerMethodMapping.Match<RequestMappingInfo> getMappingInfoMatch(
          TestRequestMappingInfoHandlerMapping mapping, RequestMappingInfo info) {
    var registration = mapping.mappingRegistry.registrations.get(info);
    if (registration == null) {
      registration = new AbstractHandlerMethodMapping.MappingRegistration<>(
              info, new HandlerMethod(new TestController(),
              ReflectionUtils.getMethod(TestController.class, "empty")),
              info.getDirectPaths(), info.getName(), false
      );
    }
    return new AbstractHandlerMethodMapping.Match<>(info, registration);
  }

  private MultiValueMap<String, String> getMatrixVariables(HttpServletRequest request, String uriVarName) {
    Object attribute = request.getAttribute(HandlerMatchingMetadataKey);
    if (attribute instanceof HandlerMatchingMetadata matchingMetadata) {
      return matchingMetadata.getMatrixVariable(uriVarName);
    }
    return fail("");
  }

  private Map<String, String> getUriTemplateVariables(HttpServletRequest request) {
    Object attribute = request.getAttribute(HandlerMatchingMetadataKey);
    if (attribute instanceof HandlerMatchingMetadata matchingMetadata) {
      return matchingMetadata.getUriVariables();
    }
    return fail("");
  }

  @SuppressWarnings("unused")
  @Controller
  private static class TestController {

    @RequestMapping(value = "/foo", method = HttpMethod.GET)
    public void foo() {
    }

    @RequestMapping(value = "/foo", method = HttpMethod.GET, params = "p")
    public void fooParam() {
    }

    @RequestMapping(value = "/ba*", method = { HttpMethod.GET, HttpMethod.HEAD })
    public void bar() {
    }

    @RequestMapping("")
    public void empty() {
    }

    @RequestMapping(value = "/person/{id}", method = HttpMethod.PUT, consumes = "application/xml")
    public void consumes(@RequestBody String text) {
    }

    @RequestMapping(value = "/persons", produces = "application/xml")
    public String produces() {
      return "";
    }

    @RequestMapping(value = "/params", params = "foo=bar")
    public String param() {
      return "";
    }

    @RequestMapping(value = "/params", params = "bar=baz")
    public String param2() {
      return "";
    }

    @RequestMapping(value = "/content", produces = "application/xml")
    public String xmlContent() {
      return "";
    }

    @RequestMapping(value = "/content", produces = "!application/xml")
    public String nonXmlContent() {
      return "";
    }

    @RequestMapping(value = "/something", method = HttpMethod.OPTIONS)
    public HttpHeaders fooOptions() {
      HttpHeaders headers = HttpHeaders.create();
      headers.add("Allow", "PUT,POST");
      return headers;
    }

    @RequestMapping(value = "/qux", method = HttpMethod.GET, produces = "application/xml")
    public String getBaz() {
      return "";
    }

    @RequestMapping(value = "/qux", method = HttpMethod.PATCH, consumes = "foo/bar")
    public void patchBaz(String value) {
    }
  }

  @SuppressWarnings("unused")
  @Controller
  private static class UserController {

    @RequestMapping(value = "/users", method = HttpMethod.GET, produces = "application/json")
    public void getUser() {
    }

    @RequestMapping(value = "/users", method = HttpMethod.PUT)
    public void saveUser() {
    }
  }

  private static class TestRequestMappingInfoHandlerMapping extends RequestMappingInfoHandlerMapping {

    void registerHandler(Object handler) {
      super.detectHandlerMethods(handler);
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
      return AnnotationUtils.findAnnotation(beanType, RequestMapping.class) != null;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
      RequestMapping annot = AnnotationUtils.findAnnotation(method, RequestMapping.class);
      if (annot != null) {
        return RequestMappingInfo.paths(annot.value())
                .methods(annot.method())
                .params(annot.params())
                .headers(annot.headers())
                .consumes(annot.consumes())
                .produces(annot.produces())
                .options(getBuilderConfig())
                .build();
      }
      else {
        return null;
      }
    }

    private RequestMappingInfo.BuilderConfiguration getBuilderConfig() {
      RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
      config.setPatternParser(getPatternParser());
      return config;
    }

    RequestMappingInfo createInfo(String... patterns) {
      return RequestMappingInfo.paths(patterns).options(getBuilderConfig()).build();
    }

  }

}