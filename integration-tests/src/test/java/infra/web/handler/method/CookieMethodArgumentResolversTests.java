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
