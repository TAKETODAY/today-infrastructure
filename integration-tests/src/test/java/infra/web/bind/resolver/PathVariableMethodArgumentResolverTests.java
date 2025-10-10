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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.core.conversion.support.DefaultConversionService;
import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ReflectionUtils;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.annotation.PathVariable;
import infra.web.bind.MissingPathVariableException;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.handler.method.MethodArgumentTypeMismatchException;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.util.UriComponentsBuilder;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

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

  @Test
  void resolveArgumentWithMapType() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleMap", Map.class);
    ResolvableMethodParameter paramMap = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("name", "value");

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramMap);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(String.class);
    assertThat(result).isEqualTo("value");
  }

  @Test
  void resolveArgumentWithIntegerConversion() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleInteger", Integer.class);
    ResolvableMethodParameter paramInt = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("id", "123");

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramInt);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(Integer.class);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void resolveArgumentWithCustomTypeConversion() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleCustom", CustomType.class);
    ResolvableMethodParameter paramCustom = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("custom", "test-value");

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    DefaultConversionService conversionService = new DefaultConversionService();
    conversionService.addConverter(String.class, CustomType.class, CustomType::new);
    initializer.setConversionService(conversionService);

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramCustom);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(CustomType.class);
    assertThat(((CustomType) result).value).isEqualTo("test-value");
  }

  @Test
  void handleMissingValueAfterConversion() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleInteger", Integer.class);
    ResolvableMethodParameter paramInt = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("id", "invalid");

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();

    assertThatExceptionOfType(MethodArgumentTypeMismatchException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramInt));
  }

  @Test
  void contributeMethodArgumentWithStringValue() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handle", String.class);
    MethodParameter parameter = new SynthesizingMethodParameter(method, 0);

    UriComponentsBuilder builder = UriComponentsBuilder.create();
    Map<String, Object> uriVariables = new java.util.HashMap<>();

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    resolver.contributeMethodArgument(parameter, "testValue", builder, uriVariables, null);

    assertThat(uriVariables).containsEntry("name", "testValue");
  }

  @Test
  void contributeMethodArgumentWithIntegerValue() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleInteger", Integer.class);
    MethodParameter parameter = new SynthesizingMethodParameter(method, 0);

    UriComponentsBuilder builder = UriComponentsBuilder.create();
    Map<String, Object> uriVariables = new java.util.HashMap<>();

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    resolver.contributeMethodArgument(parameter, 123, builder, uriVariables, null);

    assertThat(uriVariables).containsEntry("id", "123");
  }

  @Test
  void contributeMethodArgumentWithMapType() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleMap", Map.class);
    MethodParameter parameter = new SynthesizingMethodParameter(method, 0);

    UriComponentsBuilder builder = UriComponentsBuilder.create();
    Map<String, Object> uriVariables = new java.util.HashMap<>();

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    resolver.contributeMethodArgument(parameter, new java.util.HashMap<>(), builder, uriVariables, null);

    assertThat(uriVariables).isEmpty();
  }

  @Test
  void supportsParameterWithMapWithoutName() throws Exception {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleMapWithoutName", Map.class);
    ResolvableMethodParameter paramMap = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    assertThat(resolver.supportsParameter(paramMap)).isFalse();
  }

  @Test
  void supportsParameterWithMapWithName() throws Exception {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleMap", Map.class);
    ResolvableMethodParameter paramMap = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    assertThat(resolver.supportsParameter(paramMap)).isTrue();
  }

  @Test
  void resolveArgumentTypeMismatch() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleInteger", Integer.class);
    ResolvableMethodParameter paramInt = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("id", "invalid");

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();

    assertThatExceptionOfType(MethodArgumentTypeMismatchException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramInt))
            .satisfies(ex -> {
              assertThat(ex.getName()).isEqualTo("id");
              assertThat(ex.getParameter()).isSameAs(paramInt.getParameter());
            });
  }

  @Test
  void resolveArgumentWithNullValueAndPrimitiveType() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handlePrimitive", int.class);
    ResolvableMethodParameter paramInt = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    applyTemplateVars("count", "");

    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());
    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();

    assertThatExceptionOfType(MissingPathVariableException.class)
            .isThrownBy(() -> resolver.resolveArgument(webRequest, paramInt));
  }

  @Test
  void contributeMethodArgumentWithNullValue() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handle", String.class);
    MethodParameter parameter = new SynthesizingMethodParameter(method, 0);

    UriComponentsBuilder builder = UriComponentsBuilder.create();
    Map<String, Object> uriVariables = new java.util.HashMap<>();

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    resolver.contributeMethodArgument(parameter, null, builder, uriVariables, null);

    assertThat(uriVariables).containsEntry("name", "null");
  }

  @Test
  void contributeMethodArgumentWithCustomConversionService() throws Throwable {
    Method method = ReflectionUtils.findMethod(TestController.class, "handleInteger", Integer.class);
    MethodParameter parameter = new SynthesizingMethodParameter(method, 0);

    UriComponentsBuilder builder = UriComponentsBuilder.create();
    Map<String, Object> uriVariables = new java.util.HashMap<>();

    DefaultConversionService conversionService = new DefaultConversionService();
    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    resolver.contributeMethodArgument(parameter, 456, builder, uriVariables, conversionService);

    assertThat(uriVariables).containsEntry("id", "456");
  }

  @Test
  void handleResolvedValueStoresInPathVariables() throws Throwable {
    applyTemplateVars("name", "stored-value");

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    Object result = resolver.resolveArgument(webRequest, paramNamedString);

    assertThat(webRequest.matchingMetadata().getPathVariables()).containsEntry("name", "stored-value");
  }

  @Test
  void resolveNameReturnsNullWhenNoMatchingMetadata() throws Throwable {
    MockRequestContext context = new MockRequestContext(null, new HttpMockRequestImpl(), new MockHttpResponseImpl());
    // No matching metadata set

    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver();
    Object result = resolver.resolveName("name", paramNamedString, context);

    assertThat(result).isNull();
  }

  @Test
  void constructorWithConfigurableBeanFactory() {
    ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);
    PathVariableMethodArgumentResolver resolver = new PathVariableMethodArgumentResolver(beanFactory);

    assertThat(resolver).isNotNull();
  }

  static class TestController {
    public void handleMap(@PathVariable("name") Map<String, String> paramMap) { }

    public void handleMapWithoutName(@PathVariable Map<String, String> paramMap) { }

    public void handleInteger(@PathVariable("id") Integer id) { }

    public void handleCustom(@PathVariable("custom") CustomType custom) { }

    public void handle(@PathVariable("name") String name) { }

    public void handlePrimitive(@PathVariable("count") int count) {

    }
  }

  static class CustomType {
    private final String value;

    public CustomType(String value) {
      this.value = value;
    }
  }

  @SuppressWarnings("unused")
  public void handle(@PathVariable("name") String param1, String param2,
          @PathVariable(name = "name", required = false) String param3,
          @PathVariable("name") @Nullable String param4) {
  }

}
