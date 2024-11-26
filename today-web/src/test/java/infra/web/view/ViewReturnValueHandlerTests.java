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

package infra.web.view;

import org.junit.jupiter.api.Test;

import infra.web.annotation.ResponseBody;
import infra.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/14 16:06
 */
class ViewReturnValueHandlerTests {

  final ViewReturnValueHandler returnValueHandler = new ViewReturnValueHandler(new UrlBasedViewResolver());

  HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("handle"));

  HandlerMethod handleString = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("handleString"));
  HandlerMethod modelAndView = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("modelAndView"));
  HandlerMethod handleViewRef = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("handleViewRef"));
  HandlerMethod handleView = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("handleView"));
  HandlerMethod handleStringResponseBody = new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod("handleStringResponseBody"));

  ViewReturnValueHandlerTests() throws NoSuchMethodException { }

  @Test
  void supportsHandler() {
    assertThat(returnValueHandler.supportsHandler(null)).isFalse();
    assertThat(returnValueHandler.supportsHandler(null, null)).isFalse();
    assertThat(returnValueHandler.supportsHandler(null, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(null, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(null, new ModelAndView())).isTrue();
    assertThat(returnValueHandler.supportsHandler(null, ViewRef.forViewName("/viewname"))).isTrue();

    assertThat(returnValueHandler.supportsHandler(handlerMethod)).isFalse();
    assertThat(returnValueHandler.supportsHandler(handlerMethod, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(handlerMethod, null)).isFalse();
    assertThat(returnValueHandler.supportsHandler(handlerMethod, mock(View.class))).isTrue();

    assertThat(returnValueHandler.supportsHandler(handleString, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleString, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleString, null)).isFalse();
    assertThat(returnValueHandler.supportsHandler(handleString, new ModelAndView())).isTrue();

    assertThat(returnValueHandler.supportsHandler(modelAndView, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(modelAndView, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(modelAndView, null)).isTrue();
    assertThat(returnValueHandler.supportsHandler(modelAndView, new ModelAndView())).isTrue();

    assertThat(returnValueHandler.supportsHandler(handleViewRef, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleViewRef, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleViewRef, null)).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleViewRef, new ModelAndView())).isTrue();

    assertThat(returnValueHandler.supportsHandler(handleView, "")).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleView, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleView, null)).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleView, new ModelAndView())).isTrue();

    assertThat(returnValueHandler.supportsHandler(handleStringResponseBody, "")).isFalse();
    assertThat(returnValueHandler.supportsHandler(handleStringResponseBody, mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsHandler(handleStringResponseBody, null)).isFalse();
    assertThat(returnValueHandler.supportsHandler(handleStringResponseBody, new ModelAndView())).isTrue();

  }

  @Test
  void supportsReturnValue() {
    assertThat(returnValueHandler.supportsReturnValue(null)).isFalse();
    assertThat(returnValueHandler.supportsReturnValue("")).isTrue();
    assertThat(returnValueHandler.supportsReturnValue(new StringBuilder())).isTrue();
    assertThat(returnValueHandler.supportsReturnValue(mock(View.class))).isTrue();
    assertThat(returnValueHandler.supportsReturnValue(new ModelAndView())).isTrue();
    assertThat(returnValueHandler.supportsReturnValue(ViewRef.forViewName("viewname"))).isTrue();

  }

  static class TestController {

    void handle() {

    }

    String handleString() {
      return "view";
    }

    @ResponseBody
    String handleStringResponseBody() {
      return "body";
    }

    ViewRef handleViewRef() {
      return ViewRef.forViewName("view");
    }

    ModelAndView modelAndView() {
      return new ModelAndView("view");
    }

    View handleView() {
      return mock(View.class);
    }

  }

}