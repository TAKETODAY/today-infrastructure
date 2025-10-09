/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.beans.factory.annotation.Value;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.BindingContext;
import infra.web.RequestContextHolder;
import infra.web.bind.resolver.ExpressionValueMethodArgumentResolver;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;

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

  private MockRequestContext webRequest;

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

    webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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

  @Test
  public void supportsParameterWithoutValueAnnotation() throws Exception {
    Method method = getClass().getMethod("params", int.class, String.class, String.class);
    ResolvableMethodParameter param = new ResolvableMethodParameter(new MethodParameter(method, 2));
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  public void resolveArgumentWithPlaceholder() throws Throwable {
    System.setProperty("test.property", "test-value");

    Method method = getClass().getMethod("methodWithPlaceholder", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter param = new ResolvableMethodParameter(parameter);

    Object value = resolver.resolveArgument(webRequest, param);
    System.clearProperty("test.property");

    assertThat(value).isEqualTo("test-value");
  }

  @Test
  public void resolveArgumentWithDefaultValue() throws Throwable {
    Method method = getClass().getMethod("methodWithDefaultValue", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter param = new ResolvableMethodParameter(parameter);

    Object value = resolver.resolveArgument(webRequest, param);

    assertThat(value).isEqualTo("default");
  }

  @Test
  public void resolveArgumentWithSpelExpression() throws Throwable {
    request.setRequestURI("/test/path");

    Method method = getClass().getMethod("methodWithSpelExpression", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter param = new ResolvableMethodParameter(parameter);

    Object value = resolver.resolveArgument(webRequest, param);

    assertThat(value).isEqualTo("/test/path");
  }

  @Test
  public void resolveArgumentWithTypeConversion() throws Throwable {
    System.setProperty("number.property", "123");

    Method method = getClass().getMethod("methodWithNumber", int.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());
    ResolvableMethodParameter param = new ResolvableMethodParameter(parameter);

    Object value = resolver.resolveArgument(webRequest, param);
    assertThat(value).isEqualTo("123");

    webRequest.setBinding(new BindingContext());
    value = resolver.resolveArgument(webRequest, param);

    System.clearProperty("number.property");

    assertThat(value).isEqualTo(123);
  }

  @SuppressWarnings("unused")
  public void methodWithPlaceholder(@Value("${test.property}") String param) { }

  @SuppressWarnings("unused")
  public void methodWithDefaultValue(@Value("${nonexistent.property:default}") String param) { }

  @SuppressWarnings("unused")
  public void methodWithSpelExpression(@Value("#{request.requestURI}") String param) { }

  @SuppressWarnings("unused")
  public void methodWithNumber(@Value("${number.property}") int param) {
  }

  public void params(@Value("#{systemProperties.systemProperty}") int param1,
          @Value("#{request.requestPath.value()}") String param2, String notSupported) {
  }

}
