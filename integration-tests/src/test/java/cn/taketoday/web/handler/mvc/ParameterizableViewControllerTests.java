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

package cn.taketoday.web.handler.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectView;
import cn.taketoday.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 16:41
 */
class ParameterizableViewControllerTests {

  private ParameterizableViewController controller;

  private HttpMockRequestImpl request;

  private RequestContext context;

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  @BeforeEach
  public void setup() {
    this.controller = new ParameterizableViewController();
    this.request = new HttpMockRequestImpl("GET", "/");
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.refresh();
    this.context = new MockRequestContext(context, request, response);
  }

  @Test
  public void handleRequestWithViewName() throws Throwable {
    String viewName = "testView";
    this.controller.setViewName(viewName);
    ModelAndView mav = handleRequest();
    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getModel().isEmpty()).isTrue();
  }

  private ModelAndView handleRequest() throws Throwable {
    Object result = this.controller.handleRequest(context);
    if (result instanceof ModelAndView modelAndView) {
      return modelAndView;
    }
    return null;
  }

  @Test
  public void handleRequestWithoutViewName() throws Throwable {
    ModelAndView mav = handleRequest();
    assertThat(mav.getViewName()).isNull();
    assertThat(mav.getModel().isEmpty()).isTrue();
  }

  @Test
  public void handleRequestWithFlashAttributes() throws Throwable {
    this.request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, new RedirectModel("name", "value"));
    ModelAndView mav = handleRequest();
    assertThat(mav.getModel().size()).isEqualTo(1);
    assertThat(mav.getModel().get("name")).isEqualTo("value");
  }

  @Test
  public void handleRequestHttpOptions() throws Throwable {
    this.request.setMethod(HttpMethod.OPTIONS.name());
    ModelAndView mav = handleRequest();

    context.flush();
    assertThat(mav).isNull();
    assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
  }

  @Test
  public void defaultViewName() throws Throwable {
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isNull();
  }

  @Test
  public void viewName() throws Throwable {
    this.controller.setViewName("view");
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isEqualTo("view");
  }

  @Test
  public void viewNameAndStatus() throws Throwable {
    this.controller.setViewName("view");
    this.controller.setStatusCode(HttpStatus.NOT_FOUND);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getViewName()).isEqualTo("view");
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

  @Test
  public void viewNameAndStatus204() throws Throwable {
    this.controller.setStatusCode(HttpStatus.NO_CONTENT);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView).isNull();
    assertThat(this.response.getStatus()).isEqualTo(204);
  }

  @Test
  public void redirectStatus() throws Throwable {
    this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    this.controller.setViewName("/foo");
    ModelAndView modelAndView = handleRequest();

    assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
    assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
    assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  public void redirectStatusWithRedirectPrefix() throws Throwable {
    this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    this.controller.setViewName("redirect:/foo");
    ModelAndView modelAndView = handleRequest();

    assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
    assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
    assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  public void redirectView() throws Throwable {
    RedirectView view = new RedirectView("/foo");
    this.controller.setView(view);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView.getView()).isSameAs(view);
  }

  @Test
  public void statusOnly() throws Throwable {
    this.controller.setStatusCode(HttpStatus.NOT_FOUND);
    this.controller.setStatusOnly(true);
    ModelAndView modelAndView = handleRequest();
    assertThat(modelAndView).isNull();
    assertThat(this.response.getStatus()).isEqualTo(404);
  }

}
