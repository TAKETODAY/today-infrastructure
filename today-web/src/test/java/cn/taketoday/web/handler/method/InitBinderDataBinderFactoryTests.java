/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.RequestParamMethodArgumentResolver;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.bind.support.DefaultSessionAttributeStore;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/20 10:50
 */
class InitBinderDataBinderFactoryTests {

  private final ConfigurableWebBindingInitializer bindingInitializer =
          new ConfigurableWebBindingInitializer();

  private final ParameterResolvingRegistry argumentResolvers =
          new ParameterResolvingRegistry();

  MockHttpServletRequest request = new MockHttpServletRequest();

  private final ServletRequestContext webRequest = new ServletRequestContext(null, request, null);

  @Test
  public void createBinder() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinder", WebDataBinder.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getDisallowedFields()).isNotNull();
    assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
  }

  @Test
  public void createBinderWithGlobalInitialization() throws Throwable {
    ConversionService conversionService = new DefaultFormattingConversionService();
    bindingInitializer.setConversionService(conversionService);

    InitBinderBindingContext factory = createFactory("initBinder", WebDataBinder.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getConversionService()).isSameAs(conversionService);
  }

  @Test
  public void createBinderWithAttrName() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

    assertThat(dataBinder.getDisallowedFields()).isNotNull();
    assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
  }

  @Test
  public void createBinderWithAttrNameNoMatch() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "invalidName");

    assertThat(dataBinder.getDisallowedFields()).isNull();
  }

  @Test
  public void createBinderNullAttrName() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getDisallowedFields()).isNull();
  }

  @Test
  public void returnValueNotExpected() throws Exception {
    InitBinderBindingContext factory = createFactory("initBinderReturnValue", WebDataBinder.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> factory.createBinder(this.webRequest, null, "invalidName"));
  }

  @Test
  public void createBinderTypeConversion() throws Throwable {
    request.setParameter("requestParam", "22");

    this.argumentResolvers.addCustomizedStrategies(new RequestParamMethodArgumentResolver(null, false));

    InitBinderBindingContext factory = createFactory("initBinderTypeConversion", WebDataBinder.class, int.class);
    WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

    assertThat(dataBinder.getDisallowedFields()).isNotNull();
    assertThat(dataBinder.getDisallowedFields()[0]).isEqualToIgnoringCase("requestParam-22");
  }

  private InitBinderBindingContext createFactory(String methodName, Class<?>... parameterTypes)
          throws Exception {

    Object handler = new InitBinderHandler();
    Method method = handler.getClass().getMethod(methodName, parameterTypes);

    var parameterFactory = new RegistryResolvableParameterFactory(argumentResolvers);
    InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method, parameterFactory);

    ControllerMethodResolver methodResolver = new ControllerMethodResolver(null,
            new DefaultSessionAttributeStore(), parameterFactory, new ReturnValueHandlerManager());

    ModelHandler modelHandler = new ModelHandler(methodResolver);

    return new InitBinderBindingContext(
            modelHandler, this.bindingInitializer, methodResolver, List.of(handlerMethod), handlerMethod);
  }

  private static class InitBinderHandler {

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
      dataBinder.setDisallowedFields("id");
    }

    @InitBinder(value = "foo")
    public void initBinderWithAttributeName(WebDataBinder dataBinder) {
      dataBinder.setDisallowedFields("id");
    }

    @InitBinder
    public String initBinderReturnValue(WebDataBinder dataBinder) {
      return "invalid";
    }

    @InitBinder
    public void initBinderTypeConversion(WebDataBinder dataBinder, @RequestParam int requestParam) {
      dataBinder.setDisallowedFields("requestParam-" + requestParam);
    }

    public void handle() {

    }
  }

}
