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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.context.support.GenericWebApplicationContext;
import cn.taketoday.web.handler.method.MethodArgumentTypeMismatchException;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

  private MockHttpServletRequest servletRequest;

  private ServletRequestContext webRequest;
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

    servletRequest = new MockHttpServletRequest();
    webRequest = new ServletRequestContext(null, servletRequest, new MockHttpServletResponse());

    // Expose request to the current thread (for SpEL expressions)
    RequestContextHolder.set(webRequest);

  }

  @NonNull
  private SynthesizingMethodParameter getParameter(Method method, int parameterIndex) {
    SynthesizingMethodParameter parameter = new SynthesizingMethodParameter(method, parameterIndex);
    parameter.initParameterNameDiscovery(discoverer);
    return parameter;
  }

  @AfterEach
  void reset() {
    RequestContextHolder.remove();
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
    servletRequest.addHeader("name", expected);

    Object result = resolver.resolveArgument(webRequest, paramNamedDefaultValueStringHeader);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void resolveStringArrayArgument() throws Throwable {
    String[] expected = new String[] { "foo", "bar" };
    servletRequest.addHeader("name", expected);
    webRequest.setBindingContext(new BindingContext());

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
    servletRequest.addHeader("bar", expected);

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
    servletRequest.addHeader("bar", expected);

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
    servletRequest.setContextPath("/bar");

    Object result = resolver.resolveArgument(webRequest, paramContextPath);
    assertThat(result).isEqualTo("/bar");
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
    servletRequest.addHeader("name", rfc1123val);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramDate);
    assertThat(result).isEqualTo(new Date(rfc1123val));
  }

  @Test
  void instantConversion() throws Throwable {
    String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
    servletRequest.addHeader("name", rfc1123val);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramInstant);

    assertThat(result).isEqualTo(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)));
  }

  @Test
  void uuidConversionWithValidValue() throws Throwable {
    UUID uuid = UUID.randomUUID();
    servletRequest.addHeader("name", uuid.toString());

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramUuid);

    assertThat(result).isEqualTo(uuid);
  }

  @Test
  void uuidConversionWithInvalidValue() throws Throwable {
    servletRequest.addHeader("name", "bogus-uuid");

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

    assertThatExceptionOfType(MethodArgumentTypeMismatchException.class).isThrownBy(
            () -> resolver.resolveArgument(webRequest, paramUuid));
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
    servletRequest.addHeader("name", uuid);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

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
    servletRequest.addHeader("name", uuid);

    ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
    bindingInitializer.setConversionService(new DefaultFormattingConversionService());
    webRequest.setBindingContext(new BindingContext(bindingInitializer));

    Object result = resolver.resolveArgument(webRequest, paramUuidOptional);

    assertThat(result).isNull();
  }

  void params(
          @RequestHeader(name = "name", defaultValue = "bar") String param1,
          @RequestHeader("name") String[] param2,
          @RequestHeader(name = "name", defaultValue = "#{systemProperties.systemProperty}") String param3,
          @RequestHeader(name = "name", defaultValue = "#{request.contextPath}") String param4,
          @RequestHeader("#{systemProperties.systemProperty}") String param5,
          @RequestHeader("${systemProperty}") String param6,
          @RequestHeader("name") Map<?, ?> unsupported,
          @RequestHeader("name") Date dateParam,
          @RequestHeader("name") Instant instantParam,
          @RequestHeader("name") UUID uuid,
          @RequestHeader(name = "name", required = false) UUID uuidOptional) {
  }

}
