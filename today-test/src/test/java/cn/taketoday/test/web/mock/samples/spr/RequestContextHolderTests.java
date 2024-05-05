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

package cn.taketoday.test.web.mock.samples.spr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.context.annotation.RequestScope;
import cn.taketoday.web.context.annotation.SessionScope;
import cn.taketoday.web.mock.WebApplicationContext;
import cn.taketoday.web.mock.filter.GenericFilterBean;
import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the following use cases.
 * <ul>
 * <li>SPR-10025: Access to request attributes via RequestContextHolder</li>
 * <li>SPR-13217: Populate RequestContext before invoking Filters in MockMvc</li>
 * <li>SPR-13260: No reuse of mock requests</li>
 * </ul>
 *
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
  @EnableWebSession
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
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, ServletException {
      this.service.process();
      RequestContext requestContext = RequestContextHolder.get();
      assertRequestAttributes(requestContext);
      assertRequestAttributes();
      chain.doFilter(request, response);
    }
  }

  static class RequestFilter extends GenericFilterBean {

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, ServletException {
      request.setAttribute(FROM_REQUEST_FILTER, FROM_REQUEST_FILTER);
      chain.doFilter(request, response);
    }
  }

  static class RequestAttributesFilter extends GenericFilterBean {

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, ServletException {
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
