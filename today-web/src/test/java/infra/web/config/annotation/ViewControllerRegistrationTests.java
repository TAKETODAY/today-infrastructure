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

package infra.web.config.annotation;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.context.ApplicationContext;
import infra.http.HttpStatusCode;
import infra.web.HttpRequestHandler;
import infra.web.handler.mvc.ParameterizableViewController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 20:07
 */
class ViewControllerRegistrationTests {

  @Test
  void constructorInitializesWithUrlPath() {
    String urlPath = "/test";
    ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);

    assertThat(registration.getUrlPath()).isEqualTo(urlPath);
    assertThat(registration.getViewController()).isNotNull();
  }

  @Test
  void constructorThrowsExceptionForNullUrlPath() {
    assertThatThrownBy(() -> new ViewControllerRegistration(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'urlPath' is required.");
  }

  @Test
  void setStatusCodeSetsStatusCodeOnController() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    HttpStatusCode statusCode = HttpStatusCode.valueOf(404);

    ViewControllerRegistration result = registration.setStatusCode(statusCode);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  void setViewNameSetsViewNameOnController() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    String viewName = "testView";

    ViewControllerRegistration result = registration.setViewName(viewName);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getViewName()).isEqualTo(viewName);
  }

  @Test
  void setViewNameWithNullValue() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");

    ViewControllerRegistration result = registration.setViewName(null);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getViewName()).isNull();
  }

  @Test
  void setContentTypeSetsContentTypeOnController() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    String contentType = "text/html";

    ViewControllerRegistration result = registration.setContentType(contentType);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getContentType()).isEqualTo(contentType);
  }

  @Test
  void setReturnValueWithObject() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    String returnValue = "testReturnValue";

    ViewControllerRegistration result = registration.setReturnValue(returnValue);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getReturnValue()).isEqualTo(returnValue);
  }

  @Test
  void setReturnValueWithSupplier() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    Supplier<Object> supplier = () -> "suppliedValue";

    ViewControllerRegistration result = registration.setReturnValue(supplier);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getReturnValue()).isEqualTo(supplier);
  }

  @Test
  void setReturnValueWithHttpRequestHandler() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    HttpRequestHandler handler = request -> null;

    ViewControllerRegistration result = registration.setReturnValue(handler);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getReturnValue()).isEqualTo(handler);
  }

  @Test
  void setApplicationContextSetsContextOnController() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    ApplicationContext context = new infra.context.support.StaticApplicationContext();

    registration.setApplicationContext(context);

    assertThat(registration.getViewController().getApplicationContext()).isEqualTo(context);
  }

  @Test
  void getViewControllerReturnsSameInstance() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");

    ParameterizableViewController controller1 = registration.getViewController();
    ParameterizableViewController controller2 = registration.getViewController();

    assertThat(controller1).isSameAs(controller2);
  }

  @Test
  void chainMethodsReturnSameInstance() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");
    HttpStatusCode statusCode = HttpStatusCode.valueOf(201);
    String viewName = "testView";
    String contentType = "application/json";
    Object returnValue = "returnValue";

    ViewControllerRegistration result1 = registration.setStatusCode(statusCode);
    ViewControllerRegistration result2 = result1.setViewName(viewName);
    ViewControllerRegistration result3 = result2.setContentType(contentType);
    ViewControllerRegistration result4 = result3.setReturnValue(returnValue);

    assertThat(result1).isSameAs(registration);
    assertThat(result2).isSameAs(registration);
    assertThat(result3).isSameAs(registration);
    assertThat(result4).isSameAs(registration);

    ParameterizableViewController controller = registration.getViewController();
    assertThat(controller.getStatusCode()).isEqualTo(statusCode);
    assertThat(controller.getViewName()).isEqualTo(returnValue);
    assertThat(controller.getContentType()).isEqualTo(contentType);
    assertThat(controller.getReturnValue()).isEqualTo(returnValue);
  }

  @Test
  void setStatusCodeWithNullValue() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");

    ViewControllerRegistration result = registration.setStatusCode(null);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getStatusCode()).isNull();
  }

  @Test
  void setContentTypeWithNullValue() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");

    ViewControllerRegistration result = registration.setContentType(null);

    assertThat(result).isSameAs(registration);
    assertThat(registration.getViewController().getContentType()).isNull();
  }

  @Test
  void setApplicationContextWithNullValue() {
    ViewControllerRegistration registration = new ViewControllerRegistration("/test");

    registration.setApplicationContext(null);

    assertThat(registration.getViewController().getApplicationContext()).isNull();
  }

  @Test
  void getUrlPathReturnsCorrectValue() {
    String urlPath = "/custom/path";
    ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);

    assertThat(registration.getUrlPath()).isEqualTo(urlPath);
  }

}