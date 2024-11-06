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

package cn.taketoday.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.InfraConfigurationException;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;
import cn.taketoday.web.handler.mvc.ParameterizableViewController;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
