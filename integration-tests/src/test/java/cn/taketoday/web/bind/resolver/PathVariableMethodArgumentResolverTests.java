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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 21:14
 */
class PathVariableMethodArgumentResolverTests {

  private PathVariableMethodArgumentResolver resolver;

  private ServletRequestContext webRequest;

  private MockHttpServletRequest request;

  private ResolvableMethodParameter paramNamedString;
  private ResolvableMethodParameter paramString;
  private ResolvableMethodParameter paramNotRequired;
  private ResolvableMethodParameter paramOptional;

  @BeforeEach
  public void setup() throws Throwable {
    resolver = new PathVariableMethodArgumentResolver();
    request = new MockHttpServletRequest();
    webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    webRequest.setMatchingMetadata(new HandlerMatchingMetadata(webRequest));

    Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
    paramNamedString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));
    paramString = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 1));
    paramNotRequired = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 2));
    paramOptional = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 3));
  }

  @Test
  public void supportsParameter() {
    assertThat(resolver.supportsParameter(paramNamedString)).as("Parameter with @PathVariable annotation").isTrue();
    assertThat(resolver.supportsParameter(paramString)).as("Parameter without @PathVariable annotation").isFalse();
  }

  private void applyTemplateVars() {
    RequestPath requestPath = RequestPath.parse("/mock/value", null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("/mock/{name}");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);
    webRequest.setMatchingMetadata(matchingMetadata);
  }

  private void applyTemplateVars(String name, String value) {
    RequestPath requestPath = RequestPath.parse("/mock/" + value, null);
    PathPattern pathPattern = PathPatternParser.defaultInstance.parse("/mock/{" + name + "}");

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            new Object(), "/mock", requestPath, pathPattern, PathPatternParser.defaultInstance);
    webRequest.setMatchingMetadata(matchingMetadata);
  }

  @Test
  public void resolveArgument() throws Throwable {
    applyTemplateVars();

    String result = (String) resolver.resolveArgument(webRequest, paramNamedString);
    assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");
  }

  @Test
  public void resolveArgumentNotRequired() throws Throwable {
    applyTemplateVars();

    String result = (String) resolver.resolveArgument(webRequest, paramNotRequired);
    assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");

    // not required
    applyTemplateVars("name", "");

    Object value = resolver.resolveArgument(webRequest, paramNotRequired);
    assertThat(value).isNull();
  }

  @Test
  public void resolveArgumentWrappedAsOptional() throws Throwable {
    applyTemplateVars();

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    @SuppressWarnings("unchecked")
    Optional<String> result = (Optional<String>)
            resolver.resolveArgument(webRequest, paramOptional);
    assertThat(result.get()).as("PathVariable not resolved correctly").isEqualTo("value");

    // not required
    applyTemplateVars("name", "");
    Object value = resolver.resolveArgument(webRequest, paramOptional);
    assertThat(value).isNotNull().isInstanceOf(Optional.class);

    result = (Optional<String>) value;

    assertThat(result).isEmpty();
  }

  @Test
  public void handleMissingValue() throws Throwable {
    assertThatExceptionOfType(MissingPathVariableException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramNamedString));
  }

  @Test
  public void nullIfNotRequired() throws Throwable {
    assertThat(resolver.resolveArgument(webRequest, paramNotRequired)).isNull();
  }

  @Test
  public void wrapEmptyWithOptional() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    assertThat(resolver.resolveArgument(webRequest, paramOptional)).isEqualTo(Optional.empty());
  }

  @SuppressWarnings("unused")
  public void handle(@PathVariable("name") String param1, String param2,
          @PathVariable(name = "name", required = false) String param3,
          @PathVariable("name") Optional<String> param4) {
  }

}
