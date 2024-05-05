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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.CrossOrigin;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.HandlerMethodMappingNamingStrategy;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;

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

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    ServletRequestContext context = new ServletRequestContext(null, request, null);
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

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

    ServletRequestContext context = new ServletRequestContext(null, request, null);
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
            this.mapping.getHandlerInternal(new ServletRequestContext(
                    null, new MockHttpServletRequest("GET", "/foo"), null)));
  }

  @Test // gh-26490
  public void ambiguousMatchOnPreFlightRequestWithoutCorsConfig() throws Throwable {
    this.mapping.registerMapping("/foo", this.handler, this.method1);
    this.mapping.registerMapping("/f??", this.handler, this.method2);

    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/foo");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext context = new ServletRequestContext(null, request, response);

    HandlerExecutionChain chain = getHandler(context);

    assertThat(chain).isNotNull();
    assertThat(chain.getRawHandler()).isInstanceOf(HttpRequestHandler.class);

    if (chain.getRawHandler() instanceof HttpRequestHandler httpRequestHandler) {
      httpRequestHandler.handleRequest(context);
    }

    assertThat(response.getStatus()).isEqualTo(403);
  }

  private HandlerExecutionChain getHandler(ServletRequestContext context) throws Exception {
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

    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/foo");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext context = new ServletRequestContext(null, request, response);
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
    assertThat(this.mapping.getHandlerInternal(new ServletRequestContext(null, new MockHttpServletRequest("GET", key), null))).isNotNull();

    this.mapping.unregisterMapping(key);
    assertThat(mapping.getHandlerInternal(new ServletRequestContext(null, new MockHttpServletRequest("GET", key), null))).isNull();
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
    HandlerMethod handlerMethod = this.mapping.getHandlerInternal(new ServletRequestContext(
            null, new MockHttpServletRequest("GET", key), null));
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
