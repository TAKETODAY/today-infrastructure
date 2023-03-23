/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.RedirectView;
import cn.taketoday.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 16:41
 */
class ParameterizableViewControllerTests {

  private ParameterizableViewController controller;

  private MockHttpServletRequest request;

  private RequestContext context;

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  public void setup() {
    this.controller = new ParameterizableViewController();
    this.request = new MockHttpServletRequest("GET", "/");
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.refresh();
    this.context = new ServletRequestContext(context, request, response);
  }

  @Test
  public void handleRequestWithViewName() throws Exception {
    String viewName = "testView";
    this.controller.setViewName(viewName);
    ModelAndView mav = handleRequest();
    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getModel().isEmpty()).isTrue();
  }

  private ModelAndView handleRequest() throws Exception {
    return (ModelAndView) this.controller.handleRequest(context);
  }

  @Test
  public void handleRequestWithoutViewName() throws Exception {
    ModelAndView mav = handleRequest();
    assertThat(mav.getViewName()).isNull();
    assertThat(mav.getModel().isEmpty()).isTrue();
  }

  @Test
  public void handleRequestWithFlashAttributes() throws Exception {
    this.request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, new RedirectModel("name", "value"));
    ModelAndView mav = handleRequest();
    assertThat(mav.getModel().size()).isEqualTo(1);
    assertThat(mav.getModel().get("name")).isEqualTo("value");
  }

  @Test
  public void handleRequestHttpOptions() throws Exception {
    this.request.setMethod(HttpMethod.OPTIONS.name());
    ModelAndView mav = handleRequest();

    context.flush();
    assertThat(mav).isNull();
    assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
  }
//

  @Test
  public void defaultViewName() throws Exception {
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isNull();
  }

  @Test
  public void viewName() throws Exception {
    this.controller.setViewName("view");
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isEqualTo("view");
  }

  @Test
  public void viewNameAndStatus() throws Exception {
    this.controller.setViewName("view");
    this.controller.setStatusCode(HttpStatus.NOT_FOUND);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isEqualTo("view");
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

  @Test
  public void viewNameAndStatus204() throws Exception {
    this.controller.setStatusCode(HttpStatus.NO_CONTENT);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView).isNull();
    assertThat(this.response.getStatus()).isEqualTo(204);
  }

  @Test
  public void redirectStatus() throws Exception {
    this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    this.controller.setViewName("/foo");
    ModelAndView modelAndView = handleRequest();

    assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
    assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
    assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  public void redirectStatusWithRedirectPrefix() throws Exception {
    this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    this.controller.setViewName("redirect:/foo");
    ModelAndView modelAndView = handleRequest();

    assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
    assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
    assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  public void redirectView() throws Exception {
    RedirectView view = new RedirectView("/foo");
    this.controller.setView(view);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getView()).isSameAs(view);
  }

  @Test
  public void statusOnly() throws Exception {
    this.controller.setStatusCode(HttpStatus.NOT_FOUND);
    this.controller.setStatusOnly(true);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView).isNull();
    assertThat(this.response.getStatus()).isEqualTo(404);
  }
}
