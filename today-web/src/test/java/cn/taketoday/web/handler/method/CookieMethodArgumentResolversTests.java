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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.CookieParameterResolver;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.bind.resolver.ServletParameterResolvers;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.http.Cookie;

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

  private ServletRequestContext webRequest;

  private MockHttpServletRequest request;

  private ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
  final MockServletContext servletContext = new MockServletContext();

  @BeforeEach
  public void setUp() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();

    CookieParameterResolver.register(strategies, context.getBeanFactory());
    ServletParameterResolvers.register(context.getBeanFactory(), strategies, servletContext);

    Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
    paramNamedCookie = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));
    paramNamedDefaultValueString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 1));
    paramString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 2));

    request = new MockHttpServletRequest();
    webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());
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
