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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Stream;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.CrossOrigin;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.context.support.StaticWebApplicationContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.view.PathPatternsParameterizedTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/21 10:54
 */
class CrossOriginTests {

  @SuppressWarnings("unused")
  static Stream<TestRequestMappingInfoHandlerMapping> pathPatternsArguments() {
    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    Properties props = new Properties();
    props.setProperty("myOrigin", "https://example.com");
    props.setProperty("myDomainPattern", "http://*.example.com");
    wac.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("ps", props));
    wac.registerSingleton("ppc", PropertySourcesPlaceholderConfigurer.class);
    wac.refresh();

    TestRequestMappingInfoHandlerMapping mapping1 = new TestRequestMappingInfoHandlerMapping();
    wac.getAutowireCapableBeanFactory().initializeBean(mapping1, "mapping1");

    TestRequestMappingInfoHandlerMapping mapping2 = new TestRequestMappingInfoHandlerMapping();
    wac.getAutowireCapableBeanFactory().initializeBean(mapping2, "mapping2");
    wac.close();

    return Stream.of(mapping1, mapping2);
  }

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @BeforeEach
  void setup() {
    this.request.setMethod("GET");
    this.request.addHeader(HttpHeaders.ORIGIN, "https://domain.com/");
  }

  @PathPatternsParameterizedTest
  void noAnnotationWithoutOrigin(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no");
    HandlerExecutionChain chain = getHandler(mapping, request);
    assertThat(getCorsConfiguration(chain, false)).isNull();
  }

  @PathPatternsParameterizedTest
  void noAnnotationWithAccessControlHttpMethod(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/no");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    HandlerExecutionChain chain = getHandler(mapping, request);
    assertThat(chain).isNotNull();
    assertThat(chain.getHandler().toString())
            .endsWith("RequestMappingInfoHandlerMapping$HttpOptionsHandler#handle()");
  }

  @PathPatternsParameterizedTest
  void noAnnotationWithPreflightRequest(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/no");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain.com/");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    HandlerExecutionChain chain = getHandler(mapping, request);
    assertThat(chain).isNotNull();
    assertThat(chain.getHandler().getClass().getName()).endsWith("AbstractHandlerMapping$PreFlightHandler");
  }

  @PathPatternsParameterizedTest
    // SPR-12931
  void noAnnotationWithOrigin(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/no");
    HandlerExecutionChain chain = getHandler(mapping, request);
    assertThat(getCorsConfiguration(chain, false)).isNull();
  }

  @PathPatternsParameterizedTest
    // SPR-12931
  void noAnnotationPostWithOrigin(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setMethod("POST");
    this.request.setRequestURI("/no");
    HandlerExecutionChain chain = getHandler(mapping, request);
    assertThat(getCorsConfiguration(chain, false)).isNull();
  }

  @PathPatternsParameterizedTest
  void defaultAnnotation(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/default");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isNull();
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(CollectionUtils.isEmpty(config.getExposedHeaders())).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(1800));
  }

  @PathPatternsParameterizedTest
  void customized(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/customized");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("DELETE");
    assertThat(config.getAllowedOrigins()).containsExactly("https://site1.com", "https://site2.com");
    assertThat(config.getAllowedHeaders()).containsExactly("header1", "header2");
    assertThat(config.getExposedHeaders()).containsExactly("header3", "header4");
    assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(123));
    assertThat(config.getAllowCredentials()).isFalse();
  }

  @PathPatternsParameterizedTest
  void customOriginDefinedViaValueAttribute(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/customOrigin");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).isEqualTo(Collections.singletonList("https://example.com"));
    assertThat(config.getAllowCredentials()).isNull();
  }

  @PathPatternsParameterizedTest
  void customOriginDefinedViaPlaceholder(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/someOrigin");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).isEqualTo(Collections.singletonList("https://example.com"));
    assertThat(config.getAllowCredentials()).isNull();
  }

  @PathPatternsParameterizedTest
  public void customOriginPatternViaValueAttribute(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/customOriginPattern");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).isEqualTo(Collections.singletonList("http://*.example.com"));
    assertThat(config.getAllowCredentials()).isNull();
  }

  @PathPatternsParameterizedTest
  public void customOriginPatternViaPlaceholder(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setRequestURI("/customOriginPatternPlaceholder");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).isEqualTo(Collections.singletonList("http://*.example.com"));
    assertThat(config.getAllowCredentials()).isNull();
  }

  @PathPatternsParameterizedTest
  void bogusAllowCredentialsValue(TestRequestMappingInfoHandlerMapping mapping) {
    assertThatIllegalStateException()
            .isThrownBy(() -> mapping.registerHandler(new MethodLevelControllerWithBogusAllowCredentialsValue()))
            .withMessageContaining("@CrossOrigin's allowCredentials")
            .withMessageContaining("current value is [bogus]");
  }

  @PathPatternsParameterizedTest
  void allowCredentialsWithDefaultOrigin(TestRequestMappingInfoHandlerMapping mapping) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> mapping.registerHandler(new CredentialsWithDefaultOriginController()))
            .withMessageContaining("When allowCredentials is true, allowedOrigins cannot contain");
  }

  @PathPatternsParameterizedTest
  void allowCredentialsWithWildcardOrigin(TestRequestMappingInfoHandlerMapping mapping) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> mapping.registerHandler(new CredentialsWithWildcardOriginController()))
            .withMessageContaining("When allowCredentials is true, allowedOrigins cannot contain");
  }

  @PathPatternsParameterizedTest
  void classLevel(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new ClassLevelController());

    this.request.setRequestURI("/foo");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isFalse();

    this.request.setRequestURI("/bar");
    chain = getHandler(mapping, request);
    config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isFalse();

    this.request.setRequestURI("/baz");
    chain = getHandler(mapping, request);
    config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isTrue();
  }

  @PathPatternsParameterizedTest
    // SPR-13468
  void classLevelComposedAnnotation(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new ClassLevelMappingWithComposedAnnotation());

    this.request.setRequestURI("/foo");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("http://www.foo.example");
    assertThat(config.getAllowCredentials()).isTrue();
  }

  @PathPatternsParameterizedTest
    // SPR-13468
  void methodLevelComposedAnnotation(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelMappingWithComposedAnnotation());

    this.request.setRequestURI("/foo");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("http://www.foo.example");
    assertThat(config.getAllowCredentials()).isTrue();
  }

  @Nullable
  private HandlerExecutionChain getHandler(TestRequestMappingInfoHandlerMapping mapping, MockHttpServletRequest request) throws Exception {
    Object handler = mapping.getHandler(new ServletRequestContext(null, request, null));
    if (handler instanceof HandlerExecutionChain chain) {
      return chain;
    }
    if (handler != null) {
      return new HandlerExecutionChain(handler);
    }
    return null;
  }

  @PathPatternsParameterizedTest
  void preFlightRequest(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setMethod("OPTIONS");
    this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.request.setRequestURI("/default");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, true);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isNull();
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(CollectionUtils.isEmpty(config.getExposedHeaders())).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(1800));
  }

  @PathPatternsParameterizedTest
  void ambiguousHeaderPreFlightRequest(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setMethod("OPTIONS");
    this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
    this.request.setRequestURI("/ambiguous-header");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, true);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("*");
    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(CollectionUtils.isEmpty(config.getExposedHeaders())).isTrue();
    assertThat(config.getMaxAge()).isNull();
  }

  @PathPatternsParameterizedTest
  void ambiguousProducesPreFlightRequest(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MethodLevelController());
    this.request.setMethod("OPTIONS");
    this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    this.request.setRequestURI("/ambiguous-produces");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, true);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("*");
    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(CollectionUtils.isEmpty(config.getExposedHeaders())).isTrue();
    assertThat(config.getMaxAge()).isNull();
  }

  @PathPatternsParameterizedTest
  void preFlightRequestWithoutHttpMethodHeader(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/default");
    request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
    assertThat(getHandler(mapping, request)).isNull();
  }

  @PathPatternsParameterizedTest
  void maxAgeWithDefaultOrigin(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
    mapping.registerHandler(new MaxAgeWithDefaultOriginController());

    this.request.setRequestURI("/classAge");
    HandlerExecutionChain chain = getHandler(mapping, request);
    CorsConfiguration config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getMaxAge()).isEqualTo(10);

    this.request.setRequestURI("/methodAge");
    chain = getHandler(mapping, request);
    config = getCorsConfiguration(chain, false);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedMethods()).containsExactly("GET");
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getMaxAge()).isEqualTo(100);
  }

  @Nullable
  private CorsConfiguration getCorsConfiguration(@Nullable HandlerExecutionChain chain, boolean isPreFlightRequest) {
    assertThat(chain).isNotNull();
    if (isPreFlightRequest) {
      Object handler = chain.getHandler();
      assertThat(handler.getClass().getSimpleName()).isEqualTo("PreFlightHandler");
      DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
      return (CorsConfiguration) accessor.getPropertyValue("config");
    }
    else {
      for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
        if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
          DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
          return (CorsConfiguration) accessor.getPropertyValue("config");
        }
      }
    }
    return null;
  }

  @Controller
  @SuppressWarnings("unused")
  private static class MethodLevelController {

    @GetMapping("/no")
    public void noAnnotation() {
    }

    @PostMapping("/no")
    public void noAnnotationPost() {
    }

    @CrossOrigin
    @GetMapping(path = "/default")
    public void defaultAnnotation() {
    }

    @CrossOrigin
    @GetMapping(path = "/default", params = "q")
    public void defaultAnnotationWithParams() {
    }

    @CrossOrigin
    @GetMapping(path = "/ambiguous-header", headers = "header1=a")
    public void ambiguousHeader1a() {
    }

    @CrossOrigin
    @GetMapping(path = "/ambiguous-header", headers = "header1=b")
    public void ambiguousHeader1b() {
    }

    @CrossOrigin
    @GetMapping(path = "/ambiguous-produces", produces = "application/xml")
    public String ambiguousProducesXml() {
      return "<a></a>";
    }

    @CrossOrigin
    @GetMapping(path = "/ambiguous-produces", produces = "application/json")
    public String ambiguousProducesJson() {
      return "{}";
    }

    @CrossOrigin(origins = { "https://site1.com", "https://site2.com" },
                 allowedHeaders = { "header1", "header2" },
                 exposedHeaders = { "header3", "header4" },
                 methods = HttpMethod.DELETE,
                 maxAge = 123,
                 allowCredentials = "false")
    @RequestMapping(path = "/customized", method = { HttpMethod.GET, HttpMethod.POST })
    public void customized() {
    }

    @CrossOrigin("https://example.com")
    @RequestMapping("/customOrigin")
    public void customOriginDefinedViaValueAttribute() {
    }

    @CrossOrigin("${myOrigin}")
    @RequestMapping("/someOrigin")
    public void customOriginDefinedViaPlaceholder() {
    }

    @CrossOrigin(originPatterns = "http://*.example.com")
    @RequestMapping("/customOriginPattern")
    public void customOriginPatternDefinedViaValueAttribute() {
    }

    @CrossOrigin(originPatterns = "${myDomainPattern}")
    @RequestMapping("/customOriginPatternPlaceholder")
    public void customOriginPatternDefinedViaPlaceholder() {
    }
  }

  @Controller
  @SuppressWarnings("unused")
  private static class MethodLevelControllerWithBogusAllowCredentialsValue {

    @CrossOrigin(allowCredentials = "bogus")
    @RequestMapping("/bogus")
    public void bogusAllowCredentialsValue() {
    }
  }

  @Controller
  @CrossOrigin(allowCredentials = "false")
  private static class ClassLevelController {

    @RequestMapping(path = "/foo", method = HttpMethod.GET)
    public void foo() {
    }

    @CrossOrigin
    @RequestMapping(path = "/bar", method = HttpMethod.GET)
    public void bar() {
    }

    @CrossOrigin(originPatterns = "*", allowCredentials = "true")
    @RequestMapping(path = "/baz", method = HttpMethod.GET)
    public void baz() {
    }
  }

  @Controller
  @CrossOrigin(maxAge = 10)
  private static class MaxAgeWithDefaultOriginController {

    @CrossOrigin
    @GetMapping("/classAge")
    void classAge() {
    }

    @CrossOrigin(maxAge = 100)
    @GetMapping("/methodAge")
    void methodAge() {
    }
  }

  @Controller
  @CrossOrigin(allowCredentials = "true")
  private static class CredentialsWithDefaultOriginController {

    @GetMapping(path = "/no-origin")
    public void noOrigin() {
    }
  }

  @Controller
  @CrossOrigin(allowCredentials = "true")
  private static class CredentialsWithWildcardOriginController {

    @GetMapping(path = "/no-origin")
    @CrossOrigin(origins = "*")
    public void wildcardOrigin() {
    }
  }

  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @CrossOrigin
  private @interface ComposedCrossOrigin {

    String[] origins() default {};

    String allowCredentials() default "";
  }

  @Controller
  @ComposedCrossOrigin(origins = "http://www.foo.example/", allowCredentials = "true")
  private static class ClassLevelMappingWithComposedAnnotation {

    @RequestMapping(path = "/foo", method = HttpMethod.GET)
    public void foo() {
    }

  }

  @Controller
  private static class MethodLevelMappingWithComposedAnnotation {

    @RequestMapping(path = "/foo", method = HttpMethod.GET)
    @ComposedCrossOrigin(origins = "http://www.foo.example/", allowCredentials = "true")
    public void foo() {
    }
  }

  private static class TestRequestMappingInfoHandlerMapping extends RequestMappingHandlerMapping {

    void registerHandler(Object handler) {
      super.detectHandlerMethods(handler);
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
      return AnnotationUtils.findAnnotation(beanType, Controller.class) != null;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
      ActionMapping annotation = AnnotatedElementUtils.findMergedAnnotation(method, ActionMapping.class);
      if (annotation != null) {
        RequestMappingInfo.BuilderConfiguration options = getBuilderConfiguration();
        return RequestMappingInfo.paths(annotation.value())
                .methods(annotation.method())
                .params(annotation.params())
                .combine(annotation.combine())
                .headers(annotation.headers())
                .consumes(annotation.consumes())
                .produces(annotation.produces())
                .options(options)
                .build();
      }
      else {
        return null;
      }
    }

  }

}
