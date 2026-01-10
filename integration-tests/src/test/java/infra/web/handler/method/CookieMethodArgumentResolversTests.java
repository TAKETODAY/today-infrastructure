/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.mock.api.http.Cookie;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.annotation.CookieValue;
import infra.web.bind.RequestBindingException;
import infra.web.bind.resolver.CookieParameterResolver;
import infra.web.bind.resolver.ParameterResolvingStrategies;
import infra.web.mock.MockRequestContext;
import infra.web.mock.bind.resolver.MockParameterResolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class CookieMethodArgumentResolversTests {

  private ResolvableMethodParameter paramNamedCookie;

  private ResolvableMethodParameter paramNamedDefaultValueString;

  private ResolvableMethodParameter paramString;

  private MockRequestContext webRequest;

  private HttpMockRequestImpl request;

  private ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
  final MockContextImpl mockContext = new MockContextImpl();

  @BeforeEach
  public void setUp() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();

    CookieParameterResolver.register(strategies, context.getBeanFactory());
    MockParameterResolvers.register(context.getBeanFactory(), strategies, mockContext);

    Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
    paramNamedCookie = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));
    paramNamedDefaultValueString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 1));
    paramString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 2));

    request = new HttpMockRequestImpl();
    webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());
  }

  @Test
  public void supportsParameter() {
    assertThat(strategies.supportsParameter(paramNamedCookie)).as("Cookie parameter not supported").isTrue();
    assertThat(strategies.supportsParameter(paramNamedDefaultValueString)).as("Cookie string parameter not supported").isTrue();
    assertThat(strategies.supportsParameter(paramString)).as("non-@CookieValue parameter supported").isFalse();
  }

  @Test
  public void resolveCookieDefaultValue() throws Throwable {
    Object result = strategies.resolveArgument(webRequest, paramNamedDefaultValueString);

    boolean condition = result instanceof String;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo("bar");
  }

  @Test
  public void notFound() throws Exception {
    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> strategies.resolveArgument(webRequest, paramNamedCookie));
  }

  public void params(@CookieValue("name") Cookie param1,
          @CookieValue(name = "name", defaultValue = "bar") String param2,
          String param3) {
  }

}
