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
import infra.web.BindingContext;
import infra.web.DelegatingServerHttpExchange;
import infra.web.annotation.RequestParam;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.handler.method.support.RequestParamMethodArgumentResolver;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;

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

  MockRequest request = new MockRequest();

  private final MockServerHttpExchange webRequest = new MockServerHttpExchange(null, request, null);

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
  void doesNotReplaceBindingContextWhenNoInitBinderMethodApplies() throws Exception {
    BindingContext original = new BindingContext();
    BindingContext.set(webRequest, original);

    InitBinderBindingContext factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
    factory.createBinder(this.webRequest, null, "invalidName");

    assertThat(BindingContext.find(webRequest)).isSameAs(original);
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
  void restoresBindingContextWhenInitBinderFails() throws Exception {
    BindingContext original = new BindingContext();
    BindingContext.set(webRequest, original);

    InitBinderBindingContext factory = createFactory("initBinderReturnValue", WebDataBinder.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> factory.createBinder(this.webRequest, null, "foo"));

    assertThat(BindingContext.find(webRequest)).isSameAs(original);
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

  @Test
  void temporarilyReplacesBindingContextAndRestoresItAfterInitBinder() throws Exception {
    BindingContext original = new BindingContext().addAttribute("requestParam", "not an int");
    BindingContext.set(webRequest, original);
    request.setParameter("requestParam", "22");
    argumentResolvers.addCustomizedStrategies(new RequestParamMethodArgumentResolver(null, false));

    InitBinderBindingContext factory = createFactory("initBinderTypeConversion", WebDataBinder.class, int.class);
    var wrapper = new DelegatingServerHttpExchange(webRequest);
    WebDataBinder dataBinder = factory.createBinder(wrapper, null, "foo");

    assertThat(dataBinder.getDisallowedFields()).containsExactly("requestParam-22");
    assertThat(BindingContext.obtain(wrapper)).isSameAs(original);
    assertThat(BindingContext.obtain(webRequest)).isSameAs(original);
  }

  private InitBinderBindingContext createFactory(String methodName, Class<?>... parameterTypes)
          throws Exception {

    Object handler = new InitBinderHandler();
    Method method = handler.getClass().getMethod(methodName, parameterTypes);

    ControllerMethodResolver methodResolver = new ControllerMethodResolver(null, argumentResolvers);
    InvocableHandlerMethod handlerMethod = methodResolver.createHandlerMethod(handler, method, null);

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
