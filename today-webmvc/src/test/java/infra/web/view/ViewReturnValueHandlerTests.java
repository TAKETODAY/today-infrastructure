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

package infra.web.view;

import org.junit.jupiter.api.Test;

import infra.web.LocaleResolver;
import infra.web.RequestContext;
import infra.web.annotation.ResponseBody;
import infra.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  @Test
  void shouldCreateViewReturnValueHandlerWithViewResolver() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);

    // when
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);

    // then
    assertThat(handler).isNotNull();
    assertThat(handler.getViewResolver()).isEqualTo(viewResolver);
    assertThat(handler.getLocaleResolver()).isNull();
  }

  @Test
  void shouldCreateViewReturnValueHandlerWithViewResolverAndLocaleResolver() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    LocaleResolver localeResolver = mock(LocaleResolver.class);

    // when
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver, localeResolver);

    // then
    assertThat(handler).isNotNull();
    assertThat(handler.getViewResolver()).isEqualTo(viewResolver);
    assertThat(handler.getLocaleResolver()).isEqualTo(localeResolver);
  }

  @Test
  void shouldSetAndGetLocaleResolver() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    LocaleResolver localeResolver = mock(LocaleResolver.class);

    // when
    handler.setLocaleResolver(localeResolver);

    // then
    assertThat(handler.getLocaleResolver()).isEqualTo(localeResolver);
  }

  @Test
  void shouldSupportCharSequenceReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);

    // when & then
    assertThat(handler.supportsReturnValue("viewName")).isTrue();
    assertThat(handler.supportsReturnValue(new StringBuilder("viewName"))).isTrue();
  }

  @Test
  void shouldSupportViewReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    View view = mock(View.class);

    // when & then
    assertThat(handler.supportsReturnValue(view)).isTrue();
  }

  @Test
  void shouldSupportViewRefReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    ViewRef viewRef = ViewRef.forViewName("viewName");

    // when & then
    assertThat(handler.supportsReturnValue(viewRef)).isTrue();
  }

  @Test
  void shouldSupportModelAndViewReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    ModelAndView modelAndView = new ModelAndView();

    // when & then
    assertThat(handler.supportsReturnValue(modelAndView)).isTrue();
  }

  @Test
  void shouldNotSupportNullReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);

    // when & then
    assertThat(handler.supportsReturnValue(null)).isFalse();
  }

  @Test
  void shouldHandleCharSequenceReturnValue() throws Exception {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    RequestContext context = mock(RequestContext.class);
    View view = mock(View.class);

    when(viewResolver.resolveViewName("viewName", context.getLocale())).thenReturn(view);

    // when & then
    assertThatNoException().isThrownBy(() -> handler.handleReturnValue(context, null, "viewName"));
  }

  @Test
  void shouldHandleViewReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    RequestContext context = mock(RequestContext.class);
    View view = mock(View.class);

    // when & then
    assertThatNoException().isThrownBy(() -> handler.handleReturnValue(context, null, view));
  }

  @Test
  void shouldHandleViewRefReturnValue() throws Exception {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    RequestContext context = mock(RequestContext.class);
    ViewRef viewRef = ViewRef.forViewName("viewName");
    View view = mock(View.class);

    when(viewResolver.resolveViewName("viewName", context.getLocale())).thenReturn(view);

    // when & then
    assertThatNoException().isThrownBy(() -> handler.handleReturnValue(context, null, viewRef));
  }

  @Test
  void shouldHandleModelAndViewReturnValue() throws Exception {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    RequestContext context = mock(RequestContext.class);
    ModelAndView modelAndView = new ModelAndView("viewName");
    View view = mock(View.class);

    when(viewResolver.resolveViewName("viewName", context.getLocale())).thenReturn(view);

    // when & then
    assertThatNoException().isThrownBy(() -> handler.handleReturnValue(context, null, modelAndView));
  }

  @Test
  void shouldThrowExceptionForUnsupportedReturnValue() {
    // given
    ViewResolver viewResolver = mock(ViewResolver.class);
    ViewReturnValueHandler handler = new ViewReturnValueHandler(viewResolver);
    RequestContext context = mock(RequestContext.class);
    Object unsupportedReturnValue = new Object();

    // when & then
    assertThatThrownBy(() -> handler.handleReturnValue(context, null, unsupportedReturnValue))
            .isInstanceOf(ViewRenderingException.class)
            .hasMessageContaining("Unsupported render result");
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