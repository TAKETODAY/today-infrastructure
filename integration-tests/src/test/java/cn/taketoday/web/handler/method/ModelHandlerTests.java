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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.ui.Model;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.resolver.ModelMethodProcessor;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/18 17:48
 */
class ModelHandlerTests {

  private MockRequestContext webRequest;

  private TestController controller = new TestController();

  private BindingContext bindingContext;

  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SessionConfig.class);
  ControllerMethodResolver methodResolver;

  ReturnValueHandlerManager returnValueHandlerManager;

  @EnableWebSession
  static class SessionConfig {

  }

  @BeforeEach
  public void setUp() throws Throwable {
    this.webRequest = new MockRequestContext(
            context, new HttpMockRequestImpl(), new MockHttpResponseImpl());

    webRequest.setBinding(new BindingContext());
    bindingContext = webRequest.getBinding();
    returnValueHandlerManager = new ReturnValueHandlerManager();
    returnValueHandlerManager.setApplicationContext(context);
    returnValueHandlerManager.registerDefaultHandlers();
    ResolvableParameterFactory resolvableParameterFactory = new ResolvableParameterFactory();

    this.methodResolver = new ControllerMethodResolver(context, resolvableParameterFactory);
    this.controller = new TestController();
  }

  @Test
  public void modelAttributeMethod() throws Throwable {
    BindingContext container = webRequest.getBinding();

    HandlerMethod handlerMethod = createHandlerMethod("handle");
    ModelHandler modelHandler = createModelFactory(handlerMethod, "modelAttr", Model.class);

    modelHandler.initModel(this.webRequest, container, handlerMethod);

    assertThat(container.getModel().get("modelAttr")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithExplicitName() throws Throwable {
    BindingContext mavContainer = webRequest.getBinding();
    HandlerMethod handlerMethod = createHandlerMethod("handle");

    ModelHandler modelHandler = createModelFactory(handlerMethod, "modelAttrWithName");
    modelHandler.initModel(this.webRequest, mavContainer, handlerMethod);

    assertThat(mavContainer.getModel().get("name")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithNameByConvention() throws Throwable {
    BindingContext bindingContext = webRequest.getBinding();

    HandlerMethod handlerMethod = createHandlerMethod("handle");
    ModelHandler modelHandler = createModelFactory(handlerMethod, "modelAttrConvention");
    modelHandler.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.getModel().get("boolean")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithNullReturnValue() throws Throwable {
    BindingContext bindingContext = webRequest.getBinding();

    HandlerMethod handlerMethod = createHandlerMethod("handle");
    ModelHandler modelHandler = createModelFactory(handlerMethod, "nullModelAttr");
    modelHandler.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.containsAttribute("name")).isTrue();
    assertThat(bindingContext.getModel().get("name")).isNull();
  }

  @Test
  public void modelAttributeWithBindingDisabled() throws Throwable {
    BindingContext bindingContext = webRequest.getBinding();

    HandlerMethod handlerMethod = createHandlerMethod("handle");
    ModelHandler modelHandler = createModelFactory(handlerMethod, "modelAttrWithBindingDisabled");
    modelHandler.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.containsAttribute("foo")).isTrue();
    assertThat(bindingContext.isBindingDisabled("foo")).isTrue();
  }

  @Test
  public void updateModelBindingResult() throws Throwable {
    String commandName = "attr1";
    Object command = new Object();
    WebDataBinder dataBinder = new WebDataBinder(command, commandName);
    BindingContext container = new BindingContext0(dataBinder);
    container.addAttribute(commandName, command);

    ModelHandler modelHandler = new ModelHandler(methodResolver);
    modelHandler.updateModel(this.webRequest, container);

    assertThat(container.getModel().get(commandName)).isEqualTo(command);
    String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + commandName;
    assertThat(container.getModel().get(bindingResultKey)).isSameAs(dataBinder.getBindingResult());
    assertThat(container.getModel().size()).isEqualTo(2);
  }

  static class BindingContext0 extends BindingContext {
    final WebDataBinder dataBinder;

    BindingContext0(WebDataBinder dataBinder) {
      this.dataBinder = dataBinder;
    }

    @Override
    public WebDataBinder createBinder(RequestContext request, @Nullable Object target, String objectName) throws Throwable {
      return dataBinder;
    }
  }

  @Test  // SPR-12542
  public void updateModelWhenRedirecting() throws Throwable {
    String attributeName = "sessionAttr";
    String attribute = "value";

    WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
    BindingContext container = new BindingContext0(dataBinder);
    container.addAttribute(attributeName, attribute);

    String queryParam = "123";
    String queryParamName = "q";
    container.setRedirectModel(new RedirectModel(queryParamName, queryParam));

    ModelHandler modelHandler = new ModelHandler(methodResolver);
    modelHandler.updateModel(this.webRequest, container);

    assertThat(container.getRedirectModel().get(queryParamName)).isEqualTo(queryParam);
    assertThat(container.getRedirectModel().size()).isEqualTo(1);
  }

  private ModelHandler createModelFactory(
          HandlerMethod handlerMethod, String methodName, Class<?>... parameterTypes) throws Throwable {
    ControllerMethodResolver methodResolver = mock(ControllerMethodResolver.class);

    given(methodResolver.getModelAttributeMethods(handlerMethod))
            .willReturn(List.of(createHandlerMethod(methodName, parameterTypes)));

    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    registry.getCustomizedStrategies().add(new ModelMethodProcessor());
    return new ModelHandler(methodResolver);
  }

  private InvocableHandlerMethod createHandlerMethod(String methodName, Class<?>... paramTypes) throws Throwable {
    Method method = this.controller.getClass().getMethod(methodName, paramTypes);
    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    var parameterFactory = new RegistryResolvableParameterFactory(registry);
    registry.getCustomizedStrategies().add(new ModelMethodProcessor());

    return new InvocableHandlerMethod(this.controller, method, parameterFactory);
  }

  static class TestController {

    @ModelAttribute
    public void modelAttr(Model model) {
      model.addAttribute("modelAttr", Boolean.TRUE);
    }

    @ModelAttribute("name")
    public Boolean modelAttrWithName() {
      return Boolean.TRUE;
    }

    @ModelAttribute
    public Boolean modelAttrConvention() {
      return Boolean.TRUE;
    }

    @ModelAttribute("name")
    public Boolean nullModelAttr() {
      return null;
    }

    @ModelAttribute(name = "foo", binding = false)
    public Foo modelAttrWithBindingDisabled() {
      return new Foo();
    }

    public void handle() {
    }

    public void handleSessionAttr(@ModelAttribute("sessionAttr") String sessionAttr) {
    }
  }

  private static class Foo {
  }

}
