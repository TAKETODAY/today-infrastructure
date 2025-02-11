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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.core.annotation.SynthesizingMethodParameter;
import infra.core.conversion.support.DefaultConversionService;
import infra.http.server.RequestPath;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ReflectionUtils;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.annotation.PathVariable;
import infra.web.bind.MissingPathVariableException;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 21:14
 */
class PathVariableMethodArgumentResolverTests {

  private PathVariableMethodArgumentResolver resolver;

  private MockRequestContext webRequest;

  private HttpMockRequestImpl request;

  private ResolvableMethodParameter paramNamedString;
  private ResolvableMethodParameter paramString;
  private ResolvableMethodParameter paramNotRequired;
  private ResolvableMethodParameter paramOptional;

  @BeforeEach
  public void setup() throws Throwable {
    resolver = new PathVariableMethodArgumentResolver();
    request = new HttpMockRequestImpl();
    webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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

    String result = (String) resolver.resolveArgument(webRequest, paramOptional);
    assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");

    // not required
    applyTemplateVars("name", "");
    Object value = resolver.resolveArgument(webRequest, paramOptional);
    assertThat(value).isNull();

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

    assertThat(resolver.resolveArgument(webRequest, paramOptional))
            .isNull();
  }

  @SuppressWarnings("unused")
  public void handle(@PathVariable("name") String param1, String param2,
          @PathVariable(name = "name", required = false) String param3,
          @PathVariable("name") @Nullable String param4) {
  }

}
