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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.conversion.ConversionService;
import infra.format.support.DefaultFormattingConversionService;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.annotation.RequestParam;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.RequestParamMethodArgumentResolver;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.mock.MockRequestContext;

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

  HttpMockRequestImpl request = new HttpMockRequestImpl();

  private final MockRequestContext webRequest = new MockRequestContext(null, request, null);

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

    ControllerMethodResolver methodResolver = new ControllerMethodResolver(null, parameterFactory);

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
