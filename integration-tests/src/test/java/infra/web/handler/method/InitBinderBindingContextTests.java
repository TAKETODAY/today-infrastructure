/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.conversion.ConversionService;
import infra.format.support.DefaultFormattingConversionService;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.annotation.RequestParam;
import infra.web.bind.RequestContextDataBinder;
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
class InitBinderBindingContextTests {

  private final ConfigurableWebBindingInitializer bindingInitializer =
          new ConfigurableWebBindingInitializer();

  private final ParameterResolvingRegistry argumentResolvers =
          new ParameterResolvingRegistry();

  HttpMockRequestImpl request = new HttpMockRequestImpl();

  private final MockRequestContext webRequest = new MockRequestContext(null, request, null);

  @Test
  public void createBinder() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinder", RequestContextDataBinder.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getDisallowedFields()).isNotNull();
    assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
  }

  @Test
  public void createBinderWithGlobalInitialization() throws Throwable {
    ConversionService conversionService = new DefaultFormattingConversionService();
    bindingInitializer.setConversionService(conversionService);

    InitBinderBindingContext factory = createFactory("initBinder", RequestContextDataBinder.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getConversionService()).isSameAs(conversionService);
  }

  @Test
  public void createBinderWithAttrName() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", RequestContextDataBinder.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

    assertThat(dataBinder.getDisallowedFields()).isNotNull();
    assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
  }

  @Test
  public void createBinderWithAttrNameNoMatch() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", RequestContextDataBinder.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, "invalidName");

    assertThat(dataBinder.getDisallowedFields()).isNull();
  }

  @Test
  public void createBinderNullAttrName() throws Throwable {
    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", RequestContextDataBinder.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

    assertThat(dataBinder.getDisallowedFields()).isNull();
  }

  @Test
  public void returnValueNotExpected() throws Exception {
    InitBinderBindingContext factory = createFactory("initBinderReturnValue", RequestContextDataBinder.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> factory.createBinder(this.webRequest, null, "invalidName"));
  }

  @Test
  public void createBinderTypeConversion() throws Throwable {
    request.setParameter("requestParam", "22");

    this.argumentResolvers.addCustomizedStrategies(new RequestParamMethodArgumentResolver(null, false));

    InitBinderBindingContext factory = createFactory("initBinderTypeConversion", RequestContextDataBinder.class, int.class);
    RequestContextDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

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
    public void initBinder(RequestContextDataBinder dataBinder) {
      dataBinder.setDisallowedFields("id");
    }

    @InitBinder(value = "foo")
    public void initBinderWithAttributeName(RequestContextDataBinder dataBinder) {
      dataBinder.setDisallowedFields("id");
    }

    @InitBinder
    public String initBinderReturnValue(RequestContextDataBinder dataBinder) {
      return "invalid";
    }

    @InitBinder
    public void initBinderTypeConversion(RequestContextDataBinder dataBinder, @RequestParam int requestParam) {
      dataBinder.setDisallowedFields("requestParam-" + requestParam);
    }

    public void handle() {

    }
  }

}
