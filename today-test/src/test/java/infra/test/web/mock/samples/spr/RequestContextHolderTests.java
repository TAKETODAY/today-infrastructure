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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;
import infra.mock.api.FilterChain;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.session.config.EnableSession;
import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.context.annotation.RequestScope;
import infra.web.context.annotation.SessionScope;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.filter.GenericFilterBean;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see CustomRequestAttributesRequestContextHolderTests
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextConfiguration
@DirtiesContext
public class RequestContextHolderTests {

  private static final String FROM_TCF_MOCK = "fromTestContextFrameworkMock";
  private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
  private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";
  private static final String FROM_REQUEST_FILTER = "fromRequestFilter";
  private static final String FROM_REQUEST_ATTRIBUTES_FILTER = "fromRequestAttributesFilter";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private HttpMockRequestImpl mockRequest;

  @Autowired
  private RequestScopedController requestScopedController;

  @Autowired
  private RequestScopedService requestScopedService;

  @Autowired
  private SessionScopedService sessionScopedService;

  @Autowired
  private FilterWithSessionScopedService filterWithSessionScopedService;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockRequest.setAttribute(FROM_TCF_MOCK, FROM_TCF_MOCK);

    this.mockMvc = webAppContextSetup(this.wac)
            .addFilters(new RequestFilter(), new RequestAttributesFilter(), this.filterWithSessionScopedService)
            .defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, FROM_MVC_TEST_DEFAULT))
            .alwaysExpect(status().isOk())
            .build();
  }

  @Test
  public void singletonController() throws Exception {
    this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
  }

  @Test
  public void requestScopedController() throws Exception {
    assertThat(AopUtils.isCglibProxy(this.requestScopedController)).as("request-scoped controller must be a CGLIB proxy").isTrue();
    this.mockMvc.perform(get("/requestScopedController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
  }

  @Test
  public void requestScopedService() throws Exception {
    assertThat(AopUtils.isCglibProxy(this.requestScopedService)).as("request-scoped service must be a CGLIB proxy").isTrue();
    this.mockMvc.perform(get("/requestScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
  }

  @Test
  public void sessionScopedService() throws Exception {
    assertThat(AopUtils.isCglibProxy(this.sessionScopedService)).as("session-scoped service must be a CGLIB proxy").isTrue();
    this.mockMvc.perform(get("/sessionScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
  }

  @AfterEach
  public void verifyRestoredRequestAttributes() {
    assertRequestAttributes(false);
  }

  // -------------------------------------------------------------------

  @Configuration
  @EnableWebMvc
  @EnableSession
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public SingletonController singletonController() {
      return new SingletonController();
    }

    @Bean
    @Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public RequestScopedController requestScopedController() {
      return new RequestScopedController();
    }

    @Bean
    @RequestScope
    public RequestScopedService requestScopedService() {
      return new RequestScopedService();
    }

    @Bean
    public ControllerWithRequestScopedService controllerWithRequestScopedService() {
      return new ControllerWithRequestScopedService();
    }

    @Bean
    @SessionScope
    public SessionScopedService sessionScopedService() {
      return new SessionScopedService();
    }

    @Bean
    public ControllerWithSessionScopedService controllerWithSessionScopedService() {
      return new ControllerWithSessionScopedService();
    }

    @Bean
    public FilterWithSessionScopedService filterWithSessionScopedService() {
      return new FilterWithSessionScopedService();
    }
  }

  @RestController
  static class SingletonController {

    @RequestMapping("/singletonController")
    public void handle() {
      assertRequestAttributes();
    }
  }

  @RestController
  static class RequestScopedController {

    @Autowired
    private RequestContext request;

    @RequestMapping("/requestScopedController")
    public void handle() {
      assertRequestAttributes(request);
      assertRequestAttributes();
    }
  }

  static class RequestScopedService {

    @Autowired
    private RequestContext request;

    void process() {
      assertRequestAttributes(request);
    }
  }

  static class SessionScopedService {

    @Autowired
    private RequestContext request;

    void process() {
      assertRequestAttributes(this.request);
    }
  }

  @RestController
  static class ControllerWithRequestScopedService {

    @Autowired
    private RequestScopedService service;

    @RequestMapping("/requestScopedService")
    public void handle() {
      this.service.process();
      assertRequestAttributes();
    }
  }

  @RestController
  static class ControllerWithSessionScopedService {

    @Autowired
    private SessionScopedService service;

    @RequestMapping("/sessionScopedService")
    public void handle() {
      this.service.process();
      assertRequestAttributes();
    }
  }

  static class FilterWithSessionScopedService extends GenericFilterBean {

    @Autowired
    private SessionScopedService service;

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, MockException {
      this.service.process();
      RequestContext requestContext = RequestContextHolder.get();
      assertRequestAttributes(requestContext);
      assertRequestAttributes();
      chain.doFilter(request, response);
    }
  }

  static class RequestFilter extends GenericFilterBean {

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, MockException {
      request.setAttribute(FROM_REQUEST_FILTER, FROM_REQUEST_FILTER);
      chain.doFilter(request, response);
    }
  }

  static class RequestAttributesFilter extends GenericFilterBean {

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, MockException {
      RequestContextHolder.getRequired()
              .setAttribute(FROM_REQUEST_ATTRIBUTES_FILTER, FROM_REQUEST_ATTRIBUTES_FILTER);
      chain.doFilter(request, response);
    }
  }

  private static void assertRequestAttributes() {
    assertRequestAttributes(true);
  }

  private static void assertRequestAttributes(boolean withinMockMvc) {
    RequestContext requestAttributes = RequestContextHolder.getRequired();
    assertRequestAttributes(requestAttributes, withinMockMvc);
  }

  private static void assertRequestAttributes(RequestContext request) {
    assertRequestAttributes(request, true);
  }

  private static void assertRequestAttributes(RequestContext request, boolean withinMockMvc) {
    if (withinMockMvc) {
      assertThat(request.getAttribute(FROM_TCF_MOCK)).isNull();
      assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isEqualTo(FROM_MVC_TEST_DEFAULT);
      assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isEqualTo(FROM_MVC_TEST_MOCK);
      assertThat(request.getAttribute(FROM_REQUEST_FILTER)).isEqualTo(FROM_REQUEST_FILTER);
      assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER)).isEqualTo(FROM_REQUEST_ATTRIBUTES_FILTER);
    }
    else {
      assertThat(request.getAttribute(FROM_TCF_MOCK)).isEqualTo(FROM_TCF_MOCK);
      assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isNull();
      assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isNull();
      assertThat(request.getAttribute(FROM_REQUEST_FILTER)).isNull();
      assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER)).isNull();
    }
  }

}
