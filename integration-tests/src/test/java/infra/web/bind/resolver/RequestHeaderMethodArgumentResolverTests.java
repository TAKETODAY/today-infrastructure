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

package infra.web.bind.resolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import infra.core.DefaultParameterNameDiscoverer;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.format.support.DefaultFormattingConversionService;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ReflectionUtils;
import infra.web.BindingContext;
import infra.web.RequestContextHolder;
import infra.web.annotation.RequestHeader;
import infra.web.bind.RequestBindingException;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.handler.method.MethodArgumentTypeMismatchException;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/15 14:06
 */
class RequestHeaderMethodArgumentResolverTests {

  private RequestHeaderMethodArgumentResolver resolver;

  private ResolvableMethodParameter paramNamedDefaultValueStringHeader;
  private ResolvableMethodParameter paramNamedValueStringArray;
  private ResolvableMethodParameter paramSystemProperty;
  private ResolvableMethodParameter paramContextPath;
  private ResolvableMethodParameter paramResolvedNameWithExpression;
  private ResolvableMethodParameter paramResolvedNameWithPlaceholder;
  private ResolvableMethodParameter paramNamedValueMap;
  private ResolvableMethodParameter paramDate;
  private ResolvableMethodParameter paramInstant;
  private ResolvableMethodParameter paramUuid;
  private ResolvableMethodParameter paramUuidOptional;

  private HttpMockRequestImpl mockRequest;

  private MockRequestContext webRequest;
  DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

  @BeforeEach
  @SuppressWarnings("resource")
  void setup() throws Throwable {

    GenericWebApplicationContext context = new GenericWebApplicationContext();
    context.refresh();
    resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

    Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
    paramNamedDefaultValueStringHeader = new ResolvableMethodParameter(getParameter(method, 0));
    paramNamedValueStringArray = new ResolvableMethodParameter(getParameter(method, 1));
    paramSystemProperty = new ResolvableMethodParameter(getParameter(method, 2));
    paramContextPath = new ResolvableMethodParameter(getParameter(method, 3));
    paramResolvedNameWithExpression = new ResolvableMethodParameter(getParameter(method, 4));
    paramResolvedNameWithPlaceholder = new ResolvableMethodParameter(getParameter(method, 5));
    paramNamedValueMap = new ResolvableMethodParameter(getParameter(method, 6));
    paramDate = new ResolvableMethodParameter(getParameter(method, 7));
    paramInstant = new ResolvableMethodParameter(getParameter(method, 8));
    paramUuid = new ResolvableMethodParameter(getParameter(method, 9));
    paramUuidOptional = new ResolvableMethodParameter(getParameter(method, 10));

    mockRequest = new HttpMockRequestImpl();
    webRequest = new MockRequestContext(null, mockRequest, new MockHttpResponseImpl());

    // Expose request to the current thread (for SpEL expressions)
    RequestContextHolder.set(webRequest);

  }

  private SynthesizingMethodParameter getParameter(Method method, int parameterIndex) {
    SynthesizingMethodParameter parameter = new SynthesizingMethodParameter(method, parameterIndex);
    parameter.initParameterNameDiscovery(discoverer);
    return parameter;
  }

  @AfterEach
  void reset() {
    RequestContextHolder.cleanup();
  }

  @Test
  void supportsParameter() {
    assertThat(resolver.supportsParameter(paramNamedDefaultValueStringHeader)).as("String parameter not supported").isTrue();
    assertThat(resolver.supportsParameter(paramNamedValueStringArray)).as("String array parameter not supported").isTrue();
    assertThat(resolver.supportsParameter(paramNamedValueMap)).as("non-@RequestParam parameter supported").isFalse();
  }

  @Test
  void resolveStringArgument() throws Throwable {
    String expected = "foo";
    mockRequest.addHeader("name", expected);

    Object result = resolver.resolveArgument(webRequest, paramNamedDefaultValueStringHeader);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void resolveStringArrayArgument() throws Throwable {
    String[] expected = new String[] { "foo", "bar" };
    mockRequest.addHeader("name", expected);
    webRequest.setBinding(new BindingContext());

    Object result = resolver.resolveArgument(webRequest, paramNamedValueStringArray);
    assertThat(result).isInstanceOf(String[].class);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void resolveDefaultValue() throws Throwable {
    Object result = resolver.resolveArgument(webRequest, paramNamedDefaultValueStringHeader);

    assertThat(result).isEqualTo("bar");
  }

  @Test
  void resolveDefaultValueFromSystemProperty() throws Throwable {
    System.setProperty("systemProperty", "bar");
    try {
      Object result = resolver.resolveArgument(webRequest, paramSystemProperty);
      assertThat(result).isEqualTo("bar");
    }
    finally {
      System.clearProperty("systemProperty");
    }
  }

  @Test
  void resolveNameFromSystemPropertyThroughExpression() throws Throwable {
    String expected = "foo";
    mockRequest.addHeader("bar", expected);

    System.setProperty("systemProperty", "bar");
    try {
      Object result = resolver.resolveArgument(webRequest, paramResolvedNameWithExpression);
      assertThat(result).isEqualTo(expected);
    }
    finally {
      System.clearProperty("systemProperty");
    }
  }

  @Test
  void resolveNameFromSystemPropertyThroughPlaceholder() throws Throwable {
    String expected = "foo";
    mockRequest.addHeader("bar", expected);

    System.setProperty("systemProperty", "bar");
    try {
      Object result = resolver.resolveArgument(webRequest, paramResolvedNameWithPlaceholder);
      assertThat(result).isEqualTo(expected);
    }
    finally {
      System.clearProperty("systemProperty");
    }
  }

  @Test
  void resolveDefaultValueFromRequest() throws Throwable {
    Object result = resolver.resolveArgument(webRequest, paramContextPath);
    assertThat(result).isEqualTo("");
  }

  @Test
  void notFound() throws Throwable {
    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramNamedValueStringArray));
  }

  @Test
  @SuppressWarnings("deprecation")
  void dateConversion() throws Throwable {
    String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
    mockRequest.addHeader("name", rfc1123val);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramDate);
    assertThat(result).isEqualTo(new Date(rfc1123val));
  }

  @Test
  void instantConversion() throws Throwable {
    String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
    mockRequest.addHeader("name", rfc1123val);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramInstant);

    assertThat(result).isEqualTo(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)));
  }

  @Test
  void uuidConversionWithValidValue() throws Throwable {
    UUID uuid = UUID.randomUUID();
    mockRequest.addHeader("name", uuid.toString());

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramUuid);

    assertThat(result).isEqualTo(uuid);
  }

  @Test
  void uuidConversionWithInvalidValue() throws Throwable {
    mockRequest.addHeader("name", "bogus-uuid");

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    assertThatThrownBy(
            () -> resolver.resolveArgument(webRequest, paramUuid))
            .isInstanceOf(MethodArgumentTypeMismatchException.class)
            .extracting("propertyName").isEqualTo("name");
  }

  @Test
  void uuidConversionWithEmptyValue() throws Throwable {
    uuidConversionWithEmptyOrBlankValue("");
  }

  @Test
  void uuidConversionWithBlankValue() throws Throwable {
    uuidConversionWithEmptyOrBlankValue("     ");
  }

  private void uuidConversionWithEmptyOrBlankValue(String uuid) throws Throwable {
    mockRequest.addHeader("name", uuid);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    assertThatExceptionOfType(MissingRequestHeaderException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramUuid));
  }

  @Test
  void uuidConversionWithEmptyValueOptional() throws Throwable {
    uuidConversionWithEmptyOrBlankValueOptional("");
  }

  @Test
  void uuidConversionWithBlankValueOptional() throws Throwable {
    uuidConversionWithEmptyOrBlankValueOptional("     ");
  }

  private void uuidConversionWithEmptyOrBlankValueOptional(String uuid) throws Throwable {
    mockRequest.addHeader("name", uuid);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBinding(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramUuidOptional);

    assertThat(result).isNull();
  }

  void params(
          @RequestHeader(name = "name", defaultValue = "bar") String param1,
          @RequestHeader("name") String[] param2,
          @RequestHeader(name = "name", defaultValue = "#{systemProperties.systemProperty}") String param3,
          @RequestHeader(name = "name", defaultValue = "#{request.requestPath.value()}") String param4,
          @RequestHeader("#{systemProperties.systemProperty}") String param5,
          @RequestHeader("${systemProperty}") String param6,
          @RequestHeader("name") Map<?, ?> unsupported,
          @RequestHeader("name") Date dateParam,
          @RequestHeader("name") Instant instantParam,
          @RequestHeader("name") UUID uuid,
          @RequestHeader(name = "name", required = false) UUID uuidOptional) {
  }

}
