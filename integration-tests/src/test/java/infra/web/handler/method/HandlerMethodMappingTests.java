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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import infra.context.support.StaticApplicationContext;
import infra.util.AntPathMatcher;
import infra.util.PathMatcher;
import infra.core.annotation.AnnotatedElementUtils;
import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Controller;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;
import infra.web.annotation.CrossOrigin;
import infra.web.annotation.RequestMapping;
import infra.web.cors.CorsConfiguration;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.HandlerMethodMappingNamingStrategy;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test for {@link AbstractHandlerMethodMapping}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unused")
public class HandlerMethodMappingTests {

  private MyHandlerMethodMapping mapping;

  private MyHandler handler;

  private Method method1;

  private Method method2;

  @BeforeEach
  public void setUp() throws Exception {
    this.mapping = new MyHandlerMethodMapping();
    this.handler = new MyHandler();
    this.method1 = handler.getClass().getMethod("handlerMethod1");
    this.method2 = handler.getClass().getMethod("handlerMethod2");
  }

  @Test
  public void registerDuplicates() {
    this.mapping.registerMapping("foo", this.handler, this.method1);
    assertThatIllegalStateException().isThrownBy(() ->
            this.mapping.registerMapping("foo", this.handler, this.method2));
  }

  @Test
  public void directMatch() throws Exception {
    this.mapping.registerMapping("/foo", this.handler, this.method1);
    this.mapping.registerMapping("/fo*", this.handler, this.method2);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo");
    MockRequestContext context = new MockRequestContext(null, request, null);
    HandlerMethod result = this.mapping.getHandlerInternal(context);

    assertThat(result.getMethod()).isEqualTo(method1);
    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (matchingMetadata != null) {
      assertThat(matchingMetadata.getHandler()).isEqualTo(result);
    }
    assertThat(this.mapping.getMatches()).containsExactly("/foo");
  }

  @Test
  public void patternMatch() throws Exception {
    this.mapping.registerMapping("/fo*", this.handler, this.method1);
    this.mapping.registerMapping("/f*", this.handler, this.method2);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo");

    MockRequestContext context = new MockRequestContext(null, request, null);
    HandlerMethod result = this.mapping.getHandlerInternal(context);

    assertThat(result.getMethod()).isEqualTo(method1);

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    if (matchingMetadata != null) {
      assertThat(matchingMetadata.getHandler()).isEqualTo(result);
    }

  }

  @Test
  public void ambiguousMatch() {
    this.mapping.registerMapping("/f?o", this.handler, this.method1);
    this.mapping.registerMapping("/fo?", this.handler, this.method2);

    assertThatIllegalStateException().isThrownBy(() ->
            this.mapping.getHandlerInternal(new MockRequestContext(
                    null, new HttpMockRequestImpl("GET", "/foo"), null)));
  }

  @Test
  public void ambiguousMatchOnPreFlightRequestWithoutCorsConfig() throws Throwable {
    this.mapping.registerMapping("/foo", this.handler, this.method1);
    this.mapping.registerMapping("/f??", this.handler, this.method2);

    HttpMockRequestImpl request = new HttpMockRequestImpl("OPTIONS", "/foo");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockRequestContext context = new MockRequestContext(null, request, response);

    HandlerExecutionChain chain = getHandler(context);

    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isInstanceOf(HttpRequestHandler.class);

    if (chain.getRawHandler() instanceof HttpRequestHandler httpRequestHandler) {
      httpRequestHandler.handleRequest(context);
    }

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNull();
  }

  private HandlerExecutionChain getHandler(MockRequestContext context) throws Exception {
    Object mappingHandler = this.mapping.getHandler(context);
    if (mappingHandler instanceof HandlerExecutionChain chain) {
      return chain;
    }
    return new HandlerExecutionChain(mappingHandler);
  }

  @Test // gh-26490
  public void ambiguousMatchOnPreFlightRequestWithCorsConfig() throws Throwable {
    this.mapping.registerMapping("/f?o", this.handler, this.method1);
    this.mapping.registerMapping("/fo?", this.handler, this.handler.getClass().getMethod("corsHandlerMethod"));

    HttpMockRequestImpl request = new HttpMockRequestImpl("OPTIONS", "/foo");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockRequestContext context = new MockRequestContext(null, request, response);
    HandlerExecutionChain chain = getHandler(context);
    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isInstanceOf(HttpRequestHandler.class);

    if (chain.getRawHandler() instanceof HttpRequestHandler httpRequestHandler) {
      httpRequestHandler.handleRequest(context);
    }

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain.com");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
  }

  @Test
  public void detectHandlerMethodsInAncestorContexts() {
    StaticApplicationContext cxt = new StaticApplicationContext();
    cxt.registerSingleton("myHandler", MyHandler.class);

    AbstractHandlerMethodMapping<String> mapping1 = new MyHandlerMethodMapping();
    mapping1.setApplicationContext(new StaticApplicationContext(cxt));
    mapping1.afterPropertiesSet();

    assertThat(mapping1.getHandlerMethods().size()).isEqualTo(0);

    AbstractHandlerMethodMapping<String> mapping2 = new MyHandlerMethodMapping();
    mapping2.setDetectHandlerMethodsInAncestorContexts(true);
    mapping2.setApplicationContext(new StaticApplicationContext(cxt));
    mapping2.afterPropertiesSet();

    assertThat(mapping2.getHandlerMethods().size()).isEqualTo(2);
  }

  @Test
  public void registerMapping() {
    String key1 = "/foo";
    String key2 = "/foo*";
    this.mapping.registerMapping(key1, this.handler, this.method1);
    this.mapping.registerMapping(key2, this.handler, this.method2);

    // Direct URL lookup

    List<String> directUrlMatches = this.mapping.mappingRegistry.getDirectPathMappings(key1);
    assertThat(directUrlMatches).isNotNull();
    assertThat(directUrlMatches.size()).isEqualTo(1);
    assertThat(directUrlMatches.get(0)).isEqualTo(key1);

    // Mapping name lookup

    HandlerMethod handlerMethod1 = new HandlerMethod(this.handler, this.method1);
    HandlerMethod handlerMethod2 = new HandlerMethod(this.handler, this.method2);

    String name1 = this.method1.getName();
    List<HandlerMethod> handlerMethods = this.mapping.mappingRegistry.getHandlerMethodsByMappingName(name1);
    assertThat(handlerMethods).isNotNull();
    assertThat(handlerMethods.size()).isEqualTo(1);
    assertThat(handlerMethods.get(0)).isEqualTo(handlerMethod1);

    String name2 = this.method2.getName();
    handlerMethods = this.mapping.mappingRegistry.getHandlerMethodsByMappingName(name2);
    assertThat(handlerMethods).isNotNull();
    assertThat(handlerMethods.size()).isEqualTo(1);
    assertThat(handlerMethods.get(0)).isEqualTo(handlerMethod2);
  }

  @Test
  public void registerMappingWithSameMethodAndTwoHandlerInstances() {
    String key1 = "foo";
    String key2 = "bar";

    MyHandler handler1 = new MyHandler();
    MyHandler handler2 = new MyHandler();

    HandlerMethod handlerMethod1 = new HandlerMethod(handler1, this.method1);
    HandlerMethod handlerMethod2 = new HandlerMethod(handler2, this.method1);

    this.mapping.registerMapping(key1, handler1, this.method1);
    this.mapping.registerMapping(key2, handler2, this.method1);

    // Direct URL lookup

    List<String> directUrlMatches = this.mapping.mappingRegistry.getDirectPathMappings(key1);
    assertThat(directUrlMatches).isNotNull();
    assertThat(directUrlMatches.size()).isEqualTo(1);
    assertThat(directUrlMatches.get(0)).isEqualTo(key1);

    // Mapping name lookup

    String name = this.method1.getName();
    List<HandlerMethod> handlerMethods = this.mapping.mappingRegistry.getHandlerMethodsByMappingName(name);
    assertThat(handlerMethods).isNotNull();
    assertThat(handlerMethods.size()).isEqualTo(2);
    assertThat(handlerMethods.get(0)).isEqualTo(handlerMethod1);
    assertThat(handlerMethods.get(1)).isEqualTo(handlerMethod2);
  }

  @Test
  public void unregisterMapping() throws Exception {
    String key = "foo";
    HandlerMethod handlerMethod = new HandlerMethod(this.handler, this.method1);

    this.mapping.registerMapping(key, this.handler, this.method1);
    assertThat(this.mapping.getHandlerInternal(new MockRequestContext(null, new HttpMockRequestImpl("GET", key), null))).isNotNull();

    this.mapping.unregisterMapping(key);
    assertThat(mapping.getHandlerInternal(new MockRequestContext(null, new HttpMockRequestImpl("GET", key), null))).isNull();
    assertThat(this.mapping.mappingRegistry.getDirectPathMappings(key)).isNull();
    assertThat(this.mapping.mappingRegistry.getHandlerMethodsByMappingName(this.method1.getName())).isNull();
    assertThat(this.mapping.mappingRegistry.getCorsConfiguration(handlerMethod)).isNull();
  }

  @Test
  public void getCorsConfigWithBeanNameHandler() throws Exception {
    String key = "foo";
    String beanName = "handler1";

    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.registerSingleton(beanName, MyHandler.class);

    this.mapping.setApplicationContext(context);
    this.mapping.registerMapping(key, beanName, this.method1);
    HandlerMethod handlerMethod = this.mapping.getHandlerInternal(new MockRequestContext(
            null, new HttpMockRequestImpl("GET", key), null));
  }

  private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

    private PathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> matches = new ArrayList<>();

    public MyHandlerMethodMapping() {
      setHandlerMethodMappingNamingStrategy(new SimpleMappingNamingStrategy());
    }

    public List<String> getMatches() {
      return this.matches;
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
      return true;
    }

    @Override
    protected Set<String> getDirectPaths(String mapping) {
      return (pathMatcher.isPattern(mapping) ? Collections.emptySet() : Collections.singleton(mapping));
    }

    @Override
    protected String getMappingForMethod(Method method, Class<?> handlerType) {
      String methodName = method.getName();
      return methodName.startsWith("handler") ? methodName : null;
    }

    @Override
    protected CorsConfiguration initCorsConfiguration(Object handler, HandlerMethod handlerMethod, Method method, String mapping) {
      CrossOrigin crossOrigin = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);
      if (crossOrigin != null) {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Collections.singletonList("https://domain.com"));
        return corsConfig;
      }
      return null;
    }

    @Override
    protected void handleMatch(Match<String> bestMatch, String directLookupPath, RequestContext request) {

    }

    @Override
    protected String getMatchingMapping(String pattern, RequestContext request) {
      String lookupPath = request.getRequestPath().value();
      String match = (this.pathMatcher.match(pattern, lookupPath) ? pattern : null);
      if (match != null) {
        this.matches.add(match);
      }
      return match;
    }

    @Override
    protected Comparator<String> getMappingComparator(RequestContext request) {
      String lookupPath = request.getRequestPath().value();
      return this.pathMatcher.getPatternComparator(lookupPath);
    }

  }

  private static class SimpleMappingNamingStrategy implements HandlerMethodMappingNamingStrategy<String> {

    @Override
    public String getName(HandlerMethod handlerMethod, String mapping) {
      return handlerMethod.getMethod().getName();
    }
  }

  @Controller
  static class MyHandler {

    @RequestMapping
    public void handlerMethod1() {
    }

    @RequestMapping
    public void handlerMethod2() {
    }

    @RequestMapping
    @CrossOrigin(originPatterns = "*")
    public void corsHandlerMethod() {
    }
  }
}
