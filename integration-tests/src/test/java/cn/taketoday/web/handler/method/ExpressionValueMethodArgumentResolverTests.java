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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.bind.resolver.ExpressionValueMethodArgumentResolver;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.mock.support.GenericWebApplicationContext;

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
  HttpMockRequestImpl request = new HttpMockRequestImpl();

  private ServletRequestContext webRequest;

  @BeforeEach
  @SuppressWarnings("resource")
  public void setUp() throws Exception {
    GenericWebApplicationContext context = new GenericWebApplicationContext();
    context.refresh();
    resolver = new ExpressionValueMethodArgumentResolver(context.getBeanFactory());

    DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    Method method = getClass().getMethod("params", int.class, String.class, String.class);
    paramSystemProperty = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramContextPath = new ResolvableMethodParameter(new MethodParameter(method, 1));
    paramNotSupported = new ResolvableMethodParameter(new MethodParameter(method, 2));

    paramSystemProperty.getParameter().initParameterNameDiscovery(discoverer);
    paramContextPath.getParameter().initParameterNameDiscovery(discoverer);
    paramNotSupported.getParameter().initParameterNameDiscovery(discoverer);

    webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    // Expose request to the current thread (for SpEL expressions)
    RequestContextHolder.set(webRequest);
  }

  @AfterEach
  public void teardown() {
    RequestContextHolder.cleanup();
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
    Object value = resolver.resolveArgument(webRequest, paramContextPath);

    assertThat(value).isEqualTo("");
  }

  public void params(@Value("#{systemProperties.systemProperty}") int param1,
          @Value("#{request.requestPath.value()}") String param2, String notSupported) {
  }

}
