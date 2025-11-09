/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HttpRequestHandler;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.ModelAndView;
import infra.web.view.RedirectView;
import infra.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

  @Test
  void constructorInitializesWithDefaultValues() {
    ParameterizableViewController controller = new ParameterizableViewController();

    assertThat(controller.getViewName()).isNull();
    assertThat(controller.getView()).isNull();
    assertThat(controller.getStatusCode()).isNull();
    assertThat(controller.getContentType()).isNull();
    assertThat(controller.isStatusOnly()).isFalse();
    assertThat(controller.getReturnValue()).isNull();
  }

  @Test
  void setViewNameAndGetViewName() {
    ParameterizableViewController controller = new ParameterizableViewController();
    String viewName = "testView";

    controller.setViewName(viewName);

    assertThat(controller.getViewName()).isEqualTo(viewName);
    assertThat(controller.getReturnValue()).isEqualTo(viewName);
  }

  @Test
  void setViewAndGetView() {
    ParameterizableViewController controller = new ParameterizableViewController();
    View view = new View() {
      @Override
      public String getContentType() {
        return "text/html";
      }

      @Override
      public void render(Map m, RequestContext context) {
        // noop
      }
    };

    controller.setView(view);

    assertThat(controller.getView()).isSameAs(view);
    assertThat(controller.getReturnValue()).isSameAs(view);
  }

  @Test
  void getViewNameWithRedirectStatusCode() {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setViewName("/redirect");
    controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);

    String viewName = controller.getViewName();

    assertThat(viewName).isEqualTo("redirect:/redirect");
  }

  @Test
  void getViewNameWithExistingRedirectViewNameAndRedirectStatusCode() {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setViewName("redirect:/existing");
    controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);

    String viewName = controller.getViewName();

    assertThat(viewName).isEqualTo("redirect:/existing");
  }

  @Test
  void setStatusCodeAndGetStatusCode() {
    ParameterizableViewController controller = new ParameterizableViewController();
    HttpStatusCode statusCode = HttpStatus.NOT_FOUND;

    controller.setStatusCode(statusCode);

    assertThat(controller.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  void setStatusOnlyAndIsStatusOnly() {
    ParameterizableViewController controller = new ParameterizableViewController();

    controller.setStatusOnly(true);

    assertThat(controller.isStatusOnly()).isTrue();

    controller.setStatusOnly(false);

    assertThat(controller.isStatusOnly()).isFalse();
  }

  @Test
  void setReturnValueWithObject() {
    ParameterizableViewController controller = new ParameterizableViewController();
    String returnValue = "testReturnValue";

    controller.setReturnValue(returnValue);

    assertThat(controller.getReturnValue()).isEqualTo(returnValue);
  }

  @Test
  void setReturnValueWithSupplier() {
    ParameterizableViewController controller = new ParameterizableViewController();
    Supplier<String> supplier = () -> "suppliedValue";

    controller.setReturnValue(supplier);

    assertThat(controller.getReturnValue()).isEqualTo(supplier);
  }

  @Test
  void setReturnValueWithHttpRequestHandler() {
    ParameterizableViewController controller = new ParameterizableViewController();
    HttpRequestHandler handler = request -> null;

    controller.setReturnValue(handler);

    assertThat(controller.getReturnValue()).isEqualTo(handler);
  }

  @Test
  void setContentTypeAndGetContentType() {
    ParameterizableViewController controller = new ParameterizableViewController();
    String contentType = "application/json";

    controller.setContentType(contentType);

    assertThat(controller.getContentType()).isEqualTo(contentType);
  }

  @Test
  void handleRequestInternalWithContentType() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setContentType("text/plain");
    RequestContext context = mock(RequestContext.class);

    controller.handleRequestInternal(context);

    verify(context).setContentType("text/plain");
  }

  @Test
  void handleRequestInternalWith3xxStatusCode() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    RequestContext context = mock(RequestContext.class);

    controller.handleRequestInternal(context);

    verify(context).setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.PERMANENT_REDIRECT);
  }

  @Test
  void handleRequestInternalWith204StatusCodeAndNoView() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setStatusCode(HttpStatus.NO_CONTENT);
    RequestContext context = mock(RequestContext.class);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isEqualTo(ParameterizableViewController.NONE_RETURN_VALUE);
    verify(context).setStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  void handleRequestInternalWithStatusOnly() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setStatusCode(HttpStatus.NOT_FOUND);
    controller.setStatusOnly(true);
    RequestContext context = mock(RequestContext.class);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isEqualTo(ParameterizableViewController.NONE_RETURN_VALUE);
    verify(context).setStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void handleRequestInternalWithSupplierReturnValue() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    Supplier<String> supplier = () -> "suppliedValue";
    controller.setReturnValue(supplier);
    RequestContext context = mock(RequestContext.class);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isEqualTo("suppliedValue");
  }

  @Test
  void handleRequestInternalWithHttpRequestHandlerReturnValue() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    Object handlerResult = new Object();
    HttpRequestHandler handler = request -> handlerResult;
    controller.setReturnValue(handler);
    RequestContext context = mock(RequestContext.class);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isSameAs(handlerResult);
  }

  @Test
  void handleRequestInternalWithPlainObjectReturnValue() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    String returnValue = "plainValue";
    controller.setReturnValue(returnValue);
    RequestContext context = mock(RequestContext.class);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isEqualTo(new ModelAndView(returnValue));
  }

  @Test
  void handleRequestInternalWithViewNameAndRedirectModel() throws Throwable {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setViewName("testView");

    StaticWebApplicationContext appContext = new StaticWebApplicationContext();
    appContext.refresh();
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, new RedirectModel("flashAttr", "flashValue"));
    RequestContext context = new MockRequestContext(appContext, request, response);

    Object result = controller.handleRequestInternal(context);

    assertThat(result).isInstanceOf(ModelAndView.class);
    ModelAndView mav = (ModelAndView) result;
    assertThat(mav.getModel().get("flashAttr")).isEqualTo("flashValue");
  }

  @Test
  void toStringWithStatusCodeAndViewName() {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setStatusCode(HttpStatus.NOT_FOUND);
    controller.setViewName("errorView");

    String result = controller.toString();

    assertThat(result).contains("status=404 NOT_FOUND");
    assertThat(result).contains("view=\"errorView\"");
  }

  @Test
  void toStringWithOnlyStatusCode() {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

    String result = controller.toString();

    assertThat(result).contains("status=500 INTERNAL_SERVER_ERROR");
    assertThat(result).doesNotContain("view=");
  }

  @Test
  void toStringWithOnlyViewName() {
    ParameterizableViewController controller = new ParameterizableViewController();
    controller.setViewName("testView");

    String result = controller.toString();

    assertThat(result).contains("view=\"testView\"");
    assertThat(result).doesNotContain("status=");
  }

}
