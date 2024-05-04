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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/18 17:05
 */
class ModelMethodProcessorTests {

  private ModelMethodProcessor processor;

  private MethodParameter paramModel;
  private MethodParameter redirectModelParam;

  private ServletRequestContext webRequest;

  HandlerMethod redirectModelHandler;

  @BeforeEach
  public void setUp() throws Exception {
    processor = new ModelMethodProcessor();
    Method method = getClass().getDeclaredMethod("model", Model.class);
    Method redirectModel = getClass().getDeclaredMethod("redirectModel", RedirectModel.class);
    redirectModelHandler = new HandlerMethod(this, redirectModel);

    paramModel = new MethodParameter(method, 0);
    redirectModelParam = new MethodParameter(redirectModel, 0);

    webRequest = new ServletRequestContext(null, new MockHttpServletRequest(), null);
    webRequest.setBinding(new BindingContext());
  }

  @Test
  public void supportsParameter() {
    assertThat(processor.supportsParameter(new ResolvableMethodParameter(paramModel))).isTrue();
    assertThat(processor.supportsParameter(new ResolvableMethodParameter(redirectModelParam))).isTrue();
  }

  @Test
  public void supportsReturnType() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("model", Model.class);
    HandlerMethod handlerMethod = new HandlerMethod(this, method);

    assertThat(processor.supportsHandler(null)).isFalse();
    assertThat(processor.supportsHandler(handlerMethod)).isTrue();
    assertThat(processor.supportsHandler(redirectModelHandler)).isTrue();
    assertThat(processor.supportsReturnValue(handlerMethod)).isFalse();
    assertThat(processor.supportsReturnValue(new ModelMap())).isTrue();
  }

  @Test
  public void resolveArgumentValue() throws Throwable {
    BindingContext bindingContext = webRequest.getBinding();
    ModelMap model = bindingContext.getModel();
    assertThat(processor.resolveArgument(webRequest, new ResolvableMethodParameter(paramModel))).isSameAs(model);
    assertThat(processor.resolveArgument(webRequest, new ResolvableMethodParameter(redirectModelParam))).isSameAs(bindingContext.getRedirectModel());
    assertThat(webRequest.hasAttribute(RedirectModel.OUTPUT_ATTRIBUTE)).isTrue();
    assertThat((Object) RedirectModel.findOutputModel(webRequest)).isSameAs(bindingContext.getRedirectModel());
  }

  @Test
  public void handleModelReturnValue() throws Exception {

    BindingContext bindingContext = webRequest.getBinding();
    bindingContext.addAttribute("attr1", "value1");

    ModelMap returnValue = new ModelMap();
    returnValue.addAttribute("attr2", "value2");

    Method method = getClass().getDeclaredMethod("model", Model.class);
    HandlerMethod handlerMethod = new HandlerMethod(this, method);

    processor.handleReturnValue(webRequest, handlerMethod, returnValue);

    assertThat(bindingContext.getModel().get("attr1")).isEqualTo("value1");
    assertThat(bindingContext.getModel().get("attr2")).isEqualTo("value2");

    // RedirectModel
    assertThat((Object) bindingContext.getRedirectModel()).isNull();
    assertThat(webRequest.hasAttribute(RedirectModel.OUTPUT_ATTRIBUTE)).isFalse();
    assertThat((Object) RedirectModel.findOutputModel(webRequest)).isNull();
    assertThat((Object) RedirectModel.findOutputModel(webRequest)).isSameAs(bindingContext.getRedirectModel());

    processor.handleReturnValue(webRequest, redirectModelHandler, new RedirectModel("attr3", "value3"));

    assertThat(webRequest.hasAttribute(RedirectModel.OUTPUT_ATTRIBUTE)).isTrue();
    assertThat((Object) RedirectModel.findOutputModel(webRequest)).isSameAs(bindingContext.getRedirectModel());

    assertThat((Object) bindingContext.getRedirectModel()).isNotNull();
    assertThat(bindingContext.getRedirectModel().getAttribute("attr3")).isEqualTo("value3");

    processor.handleReturnValue(webRequest, redirectModelHandler, new RedirectModel("attr4", "value4"));

    assertThat(bindingContext.getRedirectModel().getAttribute("attr3")).isEqualTo("value3");
    assertThat(bindingContext.getRedirectModel().getAttribute("attr4")).isEqualTo("value4");
  }

  @Test
  public void handleModelReturnValueUnsupported() throws Exception {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      processor.handleReturnValue(webRequest, redirectModelHandler, "");
    });
    processor.handleReturnValue(webRequest, null, "");
  }

  @SuppressWarnings("unused")
  private Model model(Model model) {
    return null;
  }

  @SuppressWarnings("unused")
  private RedirectModel redirectModel(RedirectModel model) {
    return new RedirectModel();
  }

}
