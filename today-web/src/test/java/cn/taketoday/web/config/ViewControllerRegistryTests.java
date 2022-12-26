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

package cn.taketoday.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;
import cn.taketoday.web.handler.mvc.ParameterizableViewController;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 22:24
 */
class ViewControllerRegistryTests {

  private ViewControllerRegistry registry;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() {
    this.registry = new ViewControllerRegistry(new StaticApplicationContext());
    this.request = new MockHttpServletRequest("GET", "/");
    this.response = new MockHttpServletResponse();
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
    this.request.setContextPath("/context");
    redirectView.render(Collections.emptyMap(), new ServletRequestContext(null, this.request, this.response));

    assertThat(this.response.getStatus()).isEqualTo(302);
    assertThat(this.response.getRedirectedUrl()).isEqualTo("/context/redirectTo");
    assertThat(redirectView.getApplicationContext()).isNotNull();
  }

  @Test
  public void addRedirectViewControllerWithCustomSettings() throws Exception {
    this.registry.addRedirectViewController("/path", "/redirectTo")
            .setContextRelative(false)
            .setKeepQueryParams(true)
            .setStatusCode(HttpStatus.PERMANENT_REDIRECT);

    RedirectView redirectView = getRedirectView("/path");
    this.request.setQueryString("a=b");
    this.request.setContextPath("/context");
    ServletRequestContext context = new ServletRequestContext(null, this.request, this.response);
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

  private ParameterizableViewController getController(String path) {
    Map<String, ?> urlMap = this.registry.buildHandlerMapping().getUrlMap();
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
