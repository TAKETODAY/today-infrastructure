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

package infra.web.config.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.io.ClassPathResource;
import infra.http.HttpStatus;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.InfraConfigurationException;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.handler.mvc.ParameterizableViewController;
import infra.web.mock.MockRequestContext;
import infra.web.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 22:24
 */
class ViewControllerRegistryTests {

  private ViewControllerRegistry registry;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  @BeforeEach
  public void setup() {
    this.registry = new ViewControllerRegistry(new StaticApplicationContext());
    this.request = new HttpMockRequestImpl("GET", "/");
    this.response = new MockHttpResponseImpl();
  }

  @Test
  public void noViewControllers() {
    assertThat(this.registry.buildHandlerMapping()).isNull();
  }

  @Test
  public void addViewController() {
    this.registry.addViewController("/path").setViewName("viewName");
    ParameterizableViewController controller = getController("/path");

    assertThat(controller.getViewName()).isEqualTo("viewName");
    assertThat(controller.getStatusCode()).isNull();
    assertThat(controller.isStatusOnly()).isFalse();
    assertThat(controller.getApplicationContext()).isNotNull();
  }

  @Test
  public void addViewControllerWithDefaultViewName() {
    this.registry.addViewController("/path");
    ParameterizableViewController controller = getController("/path");

    assertThat(controller.getViewName()).isNull();
    assertThat(controller.getStatusCode()).isNull();
    assertThat(controller.isStatusOnly()).isFalse();
    assertThat(controller.getApplicationContext()).isNotNull();
  }

  @Test
  public void addRedirectViewController() throws Exception {
    this.registry.addRedirectViewController("/path", "/redirectTo");
    RedirectView redirectView = getRedirectView("/path");
    this.request.setQueryString("a=b");
    redirectView.render(Collections.emptyMap(), new MockRequestContext(null, this.request, this.response));

    assertThat(this.response.getStatus()).isEqualTo(302);
    assertThat(this.response.getRedirectedUrl()).isEqualTo("/redirectTo");
    assertThat(redirectView.getApplicationContext()).isNotNull();
  }

  @Test
  public void addRedirectViewControllerWithCustomSettings() throws Exception {
    this.registry.addRedirectViewController("/path", "/redirectTo")
//            .setContextRelative(false)
            .setKeepQueryParams(true)
            .setStatusCode(HttpStatus.PERMANENT_REDIRECT);

    RedirectView redirectView = getRedirectView("/path");
    this.request.setQueryString("a=b");
    MockRequestContext context = new MockRequestContext(null, this.request, this.response);
    redirectView.render(Collections.emptyMap(), context);

    context.flush();
    assertThat(this.response.getStatus()).isEqualTo(308);
    assertThat(response.getRedirectedUrl()).isEqualTo("/redirectTo?a=b");
    assertThat(redirectView.getApplicationContext()).isNotNull();
  }

  @Test
  public void addStatusController() {
    this.registry.addStatusController("/path", HttpStatus.NOT_FOUND);
    ParameterizableViewController controller = getController("/path");

    assertThat(controller.getViewName()).isNull();
    assertThat(controller.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(controller.isStatusOnly()).isTrue();
    assertThat(controller.getApplicationContext()).isNotNull();
  }

  @Test
  public void order() {
    this.registry.addViewController("/path");
    SimpleUrlHandlerMapping handlerMapping = this.registry.buildHandlerMapping();
    assertThat(handlerMapping.getOrder()).isEqualTo(1);

    this.registry.setOrder(2);
    handlerMapping = this.registry.buildHandlerMapping();
    assertThat(handlerMapping.getOrder()).isEqualTo(2);
  }

  // web-mvc.xml

  @Test
  void registerWebViewXml() {
    ClassPathResource resource = new ClassPathResource("not-found");
    assertThatThrownBy(() -> registry.registerWebViewXml(resource))
            .isInstanceOf(InfraConfigurationException.class)
            .hasMessage("Your provided configuration location: [%s], does not exist".formatted(resource));

    registry.registerWebViewXml();

    var controller = getController("/test");

    assertThat(controller.getViewName()).isEqualTo("/xml/test");
    assertThat(controller.getStatusCode()).isNull();
    assertThat(controller.isStatusOnly()).isFalse();
    assertThat(controller.getApplicationContext()).isNotNull();
  }

  @Test
  void constructorInitializesApplicationContext() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    assertThat(registry).extracting("applicationContext").isSameAs(applicationContext);
  }

  @Test
  void addViewControllerWithResource() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    ViewControllerRegistration registration = registry.addViewController("/test", "viewName");

    assertThat(registration).isNotNull();
    assertThat(registration.getViewController().getViewName()).isEqualTo("viewName");
  }

  @Test
  void addStatusControllerWithIntegerStatus() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    registry.addStatusController("/notfound", 404);

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();
    Map<String, ?> urlMap = handlerMapping.getUrlMap();
    ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/notfound");

    assertThat(controller.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(controller.isStatusOnly()).isTrue();
  }

  @Test
  void setOrderChangesHandlerMappingOrder() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);
    registry.addViewController("/test").setViewName("view");

    registry.setOrder(5);
    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();

    assertThat(handlerMapping.getOrder()).isEqualTo(5);
  }

  @Test
  void buildHandlerMappingReturnsNullWhenNoRegistrations() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();

    assertThat(handlerMapping).isNull();
  }

  @Test
  void buildHandlerMappingReturnsHandlerMappingWithRegistrations() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);
    registry.addViewController("/test").setViewName("view");

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();

    assertThat(handlerMapping).isNotNull();
    assertThat(handlerMapping.getUrlMap()).isNotEmpty();
  }

  @Test
  void resolveEmbeddedVariablesResolvesPlaceholders() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("testProperty", "testValue");
    context.refresh();
    ViewControllerRegistry registry = new ViewControllerRegistry(context);

    String result = registry.resolveEmbeddedVariables("#{testProperty}");

    assertThat(result).isEqualTo("testValue");
  }

  @Test
  void processActionWithRedirectType() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    Element actionElement = mock(Element.class);
    when(actionElement.getAttribute("name")).thenReturn("redirectAction");
    when(actionElement.getAttribute("resource")).thenReturn("/target");
    when(actionElement.getAttribute("type")).thenReturn("redirect");
    when(actionElement.getAttribute("status")).thenReturn("308");

    registry.processAction("", "", actionElement);

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();
    Map<String, ?> urlMap = handlerMapping.getUrlMap();
    ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/redirectAction");

    assertThat(controller.getView()).isInstanceOf(RedirectView.class);
    RedirectView redirectView = (RedirectView) controller.getView();
    assertThat(redirectView).extracting("statusCode").isEqualTo(HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  void processActionWithForwardTypeAndContentType() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    Element actionElement = mock(Element.class);
    when(actionElement.getAttribute("name")).thenReturn("forwardAction");
    when(actionElement.getAttribute("resource")).thenReturn("viewName");
    when(actionElement.getAttribute("type")).thenReturn("forward");
    when(actionElement.getAttribute("content-type")).thenReturn("text/html");

    registry.processAction("", "", actionElement);

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();
    Map<String, ?> urlMap = handlerMapping.getUrlMap();
    ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/forwardAction");

    assertThat(controller.getViewName()).isEqualTo("viewName");
    assertThat(controller.getContentType()).isEqualTo("text/html");
  }

  @Test
  void processActionWithResourceAndPrefixSuffix() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ViewControllerRegistry registry = new ViewControllerRegistry(applicationContext);

    Element actionElement = mock(Element.class);
    when(actionElement.getAttribute("name")).thenReturn("prefixedAction");
    when(actionElement.getAttribute("resource")).thenReturn("view");
    when(actionElement.getAttribute("type")).thenReturn("forward");

    registry.processAction("/WEB-INF/views/", ".jsp", actionElement);

    SimpleUrlHandlerMapping handlerMapping = registry.buildHandlerMapping();
    Map<String, ?> urlMap = handlerMapping.getUrlMap();
    ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/prefixedAction");

    assertThat(controller.getViewName()).isEqualTo("/WEB-INF/views/view.jsp");
  }

  private ParameterizableViewController getController(String path) {
    SimpleUrlHandlerMapping handlerMapping = this.registry.buildHandlerMapping();
    assertThat(handlerMapping).isNotNull();
    Map<String, ?> urlMap = handlerMapping.getUrlMap();
    ParameterizableViewController controller = (ParameterizableViewController) urlMap.get(path);
    assertThat(controller).isNotNull();
    return controller;
  }

  private RedirectView getRedirectView(String path) {
    ParameterizableViewController controller = getController(path);
    assertThat(controller.getViewName()).isNull();
    assertThat(controller.getView()).isNotNull();
    assertThat(controller.getView().getClass()).isEqualTo(RedirectView.class);
    return (RedirectView) controller.getView();
  }

}
