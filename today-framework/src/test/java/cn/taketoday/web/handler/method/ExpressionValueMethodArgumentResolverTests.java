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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.bind.resolver.ExpressionValueMethodArgumentResolver;
import cn.taketoday.web.context.support.GenericWebApplicationContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ExpressionValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ExpressionValueMethodArgumentResolverTests {

  private ExpressionValueMethodArgumentResolver resolver;

  private ResolvableMethodParameter paramSystemProperty;

  private ResolvableMethodParameter paramContextPath;

  private ResolvableMethodParameter paramNotSupported;
  MockHttpServletRequest request = new MockHttpServletRequest();

  private ServletRequestContext webRequest;

  @BeforeEach
  @SuppressWarnings("resource")
  public void setUp() throws Exception {
    GenericWebApplicationContext context = new GenericWebApplicationContext();
    context.refresh();
    resolver = new ExpressionValueMethodArgumentResolver(context.getBeanFactory());

    Method method = getClass().getMethod("params", int.class, String.class, String.class);
    paramSystemProperty = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramContextPath = new ResolvableMethodParameter(new MethodParameter(method, 1));
    paramNotSupported = new ResolvableMethodParameter(new MethodParameter(method, 2));

    webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    // Expose request to the current thread (for SpEL expressions)
    RequestContextHolder.set(webRequest);
  }

  @AfterEach
  public void teardown() {
    RequestContextHolder.remove();
  }

  @Test
  public void supportsParameter() throws Exception {
    assertThat(resolver.supportsParameter(paramSystemProperty)).isTrue();
    assertThat(resolver.supportsParameter(paramContextPath)).isTrue();
    assertThat(resolver.supportsParameter(paramNotSupported)).isFalse();
  }

  @Test
  public void resolveSystemProperty() throws Throwable {
    System.setProperty("systemProperty", "22");
    Object value = resolver.resolveArgument(webRequest, paramSystemProperty);
    System.clearProperty("systemProperty");

    assertThat(value).isEqualTo("22");
  }

  @Test
  public void resolveContextPath() throws Throwable {
    request.setContextPath("/contextPath");
    Object value = resolver.resolveArgument(webRequest, paramContextPath);

    assertThat(value).isEqualTo("/contextPath");
  }

  public void params(@Value("#{systemProperties.systemProperty}") int param1,
          @Value("#{request.contextPath}") String param2, String notSupported) {
  }

}
