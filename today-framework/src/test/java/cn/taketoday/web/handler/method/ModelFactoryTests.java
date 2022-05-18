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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import cn.taketoday.core.LocalVariableTableParameterNameDiscoverer;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.bind.resolver.ModelMethodProcessor;
import cn.taketoday.web.bind.support.DefaultSessionAttributeStore;
import cn.taketoday.web.bind.support.SessionAttributeStore;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.session.WebSessionRequiredException;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.RedirectModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/18 17:48
 */
class ModelFactoryTests {

  private ServletRequestContext webRequest;

  private SessionAttributesHandler attributeHandler;

  private SessionAttributeStore attributeStore;

  private TestController controller = new TestController();

  private BindingContext bindingContext;

  @BeforeEach
  public void setUp() throws Throwable {
    this.webRequest = new ServletRequestContext(null, new MockHttpServletRequest(), null);
    this.attributeStore = new DefaultSessionAttributeStore();
    this.attributeHandler = new SessionAttributesHandler(TestController.class, this.attributeStore);
    webRequest.setBindingContext(new BindingContext());
    bindingContext = webRequest.getBindingContext();

    this.controller = new TestController();
  }

  @Test
  public void modelAttributeMethod() throws Throwable {
    BindingContext container = webRequest.getBindingContext();
    ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, container, handlerMethod);

    assertThat(container.getModel().get("modelAttr")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithExplicitName() throws Throwable {
    BindingContext mavContainer = webRequest.getBindingContext();

    ModelFactory modelFactory = createModelFactory("modelAttrWithName");
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, mavContainer, handlerMethod);

    assertThat(mavContainer.getModel().get("name")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithNameByConvention() throws Throwable {
    BindingContext bindingContext = webRequest.getBindingContext();

    ModelFactory modelFactory = createModelFactory("modelAttrConvention");
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.getModel().get("boolean")).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void modelAttributeMethodWithNullReturnValue() throws Throwable {
    BindingContext bindingContext = webRequest.getBindingContext();

    ModelFactory modelFactory = createModelFactory("nullModelAttr");
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.containsAttribute("name")).isTrue();
    assertThat(bindingContext.getModel().get("name")).isNull();
  }

  @Test
  public void modelAttributeWithBindingDisabled() throws Throwable {
    BindingContext bindingContext = webRequest.getBindingContext();

    ModelFactory modelFactory = createModelFactory("modelAttrWithBindingDisabled");
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.containsAttribute("foo")).isTrue();
    assertThat(bindingContext.isBindingDisabled("foo")).isTrue();
  }

  @Test
  public void modelAttributeFromSessionWithBindingDisabled() throws Throwable {

    Foo foo = new Foo();
    this.attributeStore.storeAttribute(this.webRequest, "foo", foo);

    ModelFactory modelFactory = createModelFactory("modelAttrWithBindingDisabled");
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.containsAttribute("foo")).isTrue();
    assertThat(bindingContext.getModel().get("foo")).isSameAs(foo);
    assertThat(bindingContext.isBindingDisabled("foo")).isTrue();
  }

  @Test
  public void sessionAttribute() throws Throwable {
    this.attributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");

    ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
    HandlerMethod handlerMethod = createHandlerMethod("handle");
    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);

    assertThat(bindingContext.getModel().get("sessionAttr")).isEqualTo("sessionAttrValue");
  }

  @Test
  public void sessionAttributeNotPresent() throws Throwable {
    ModelFactory modelFactory = new ModelFactory(null, null, this.attributeHandler);
    HandlerMethod handlerMethod = createHandlerMethod("handleSessionAttr", String.class);
    assertThatExceptionOfType(WebSessionRequiredException.class)
            .isThrownBy(() -> modelFactory.initModel(this.webRequest, bindingContext, handlerMethod));

    // Now add attribute and try again
    this.attributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");

    modelFactory.initModel(this.webRequest, bindingContext, handlerMethod);
    assertThat(bindingContext.getModel().get("sessionAttr")).isEqualTo("sessionAttrValue");
  }

  @Test
  public void updateModelBindingResult() throws Throwable {
    String commandName = "attr1";
    Object command = new Object();
    BindingContext container = new BindingContext();
    container.addAttribute(commandName, command);

    RequestContextDataBinder dataBinder = new RequestContextDataBinder(command, commandName);
    given(container.createBinder(this.webRequest, command, commandName)).willReturn(dataBinder);

    ModelFactory modelFactory = new ModelFactory(null, container, this.attributeHandler);
    modelFactory.updateModel(this.webRequest, container);

    assertThat(container.getModel().get(commandName)).isEqualTo(command);
    String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + commandName;
    assertThat(container.getModel().get(bindingResultKey)).isSameAs(dataBinder.getBindingResult());
    assertThat(container.getModel().size()).isEqualTo(2);
  }

  @Test
  public void updateModelSessionAttributesSaved() throws Throwable {
    String attributeName = "sessionAttr";
    String attribute = "value";
    BindingContext container = new BindingContext();
    container.addAttribute(attributeName, attribute);

    RequestContextDataBinder dataBinder = new RequestContextDataBinder(attribute, attributeName);
    given(container.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

    ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
    modelFactory.updateModel(this.webRequest, container);

    assertThat(container.getModel().get(attributeName)).isEqualTo(attribute);
    assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isEqualTo(attribute);
  }

  @Test
  public void updateModelSessionAttributesRemoved() throws Throwable {
    String attributeName = "sessionAttr";
    String attribute = "value";
    BindingContext container = new BindingContext();
    container.addAttribute(attributeName, attribute);

    this.attributeStore.storeAttribute(this.webRequest, attributeName, attribute);

    WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
    given(container.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

    container.getSessionStatus().setComplete();

    ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
    modelFactory.updateModel(this.webRequest, container);

    assertThat(container.getModel().get(attributeName)).isEqualTo(attribute);
    assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isNull();
  }

  @Test  // SPR-12542
  public void updateModelWhenRedirecting() throws Throwable {
    String attributeName = "sessionAttr";
    String attribute = "value";
    BindingContext container = new BindingContext();
    container.addAttribute(attributeName, attribute);

    String queryParam = "123";
    String queryParamName = "q";
    container.setRedirectModel(new RedirectModel(queryParamName, queryParam));
    container.setRedirectModelScenario(true);

    WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
    WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
    given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

    ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
    modelFactory.updateModel(this.webRequest, container);

    assertThat(container.getModel().get(queryParamName)).isEqualTo(queryParam);
    assertThat(container.getModel().size()).isEqualTo(1);
    assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isEqualTo(attribute);
  }

  private ModelFactory createModelFactory(String methodName, Class<?>... parameterTypes) throws Throwable {
    HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
    resolvers.addResolver(new ModelMethodProcessor());

    InvocableHandlerMethod modelMethod = createHandlerMethod(methodName, parameterTypes);
    modelMethod.setHandlerMethodArgumentResolvers(resolvers);
    modelMethod.setDataBinderFactory(null);
    modelMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

    return new ModelFactory(Collections.singletonList(modelMethod), null, this.attributeHandler);
  }

  private InvocableHandlerMethod createHandlerMethod(String methodName, Class<?>... paramTypes) throws Throwable {
    Method method = this.controller.getClass().getMethod(methodName, paramTypes);
    return new InvocableHandlerMethod(this.controller, method);
  }

  @SessionAttributes({ "sessionAttr", "foo" })
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