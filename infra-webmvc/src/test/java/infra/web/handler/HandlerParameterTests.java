/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.handler;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import infra.core.DefaultParameterNameDiscoverer;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.lang.Constant;
import infra.web.MockHandlerParameter;
import infra.web.annotation.RequestParam;
import infra.web.handler.method.HandlerParameter;
import infra.web.handler.method.NamedValueInfo;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/4/28 13:44
 * @since 3.0
 */
class HandlerParameterTests {

  public void method(@Nullable String name) {

  }

  public void isRequired(String name, @RequestParam(name = "myAge") int age) {

  }

  public void getGenerics(List<String> names, Map<String, Integer> map, String name) {

  }

  public void annotated(@RequestParam(name = "query", required = false, defaultValue = "fallback") String value) {

  }

  public void annotatedWithEmptyName(@RequestParam String value) {

  }

  public void annotatedWithValueAlias(@RequestParam("alias") String value) {

  }

  public void annotatedWithComposedAnnotation(@TokenParam String value) {

  }

  public void annotatedWithEmbeddedValues(
          @RequestParam(name = "${param.name}", defaultValue = "${param.default:fallback}") String value) {

  }

  public void kinds(String[] values, List<String> names, Map<String, Integer> map, Runnable task) {

  }

  @Deprecated
  @MethodMarker
  public void annotatedMethod(@ParameterMarker String value) {

  }

  public void plain(String value) {

  }

  @Test
  public void test() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("method", String.class);
    final HandlerParameter methodParameter = createParameter(0, method, "name");

    assertThat(methodParameter).isNotNull();

    assertThat(methodParameter.is(String.class)).isTrue();
    assertThat(methodParameter.is(Object.class)).isFalse();
    assertThat(methodParameter.getName()).isNotNull().isEqualTo("name");

    assertThat(methodParameter.getParameterIndex()).isZero();
    assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
    assertThat(methodParameter.isRequired()).isFalse();
    assertThat(methodParameter.isNotRequired()).isTrue();
    assertThat(methodParameter.getDefaultValue()).isNull();

  }

  @Test
  public void testRequired() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
    final HandlerParameter methodParameter = createParameter(0, method, "name");
    assertThat(methodParameter.isRequired()).isTrue();
    assertThat(methodParameter.isNotRequired()).isFalse();
    assertThat(methodParameter.isAssignableTo(CharSequence.class)).isTrue();
    assertThat(methodParameter.isAssignableTo(Integer.class)).isFalse();
    assertThat(methodParameter.isInstance("dbashbgdsaydgasyu")).isTrue();
    assertThat(methodParameter.isInstance(1024)).isFalse();
    assertThat(methodParameter.is(int.class)).isFalse();

    final HandlerParameter ageMethodParameter = createParameter(1, method, null);
    assertThat(ageMethodParameter.isRequired()).isTrue();
    assertThat(ageMethodParameter.getParameterIndex()).isEqualTo(1);
    assertThat(ageMethodParameter.getParameterType()).isEqualTo(int.class);
    assertThat(ageMethodParameter.getName()).isEqualTo("myAge");
    assertThat(ageMethodParameter.isArray()).isFalse();
    assertThat(ageMethodParameter.isCollection()).isFalse();
    assertThat(ageMethodParameter.isInterface()).isFalse();
    assertThat(ageMethodParameter.getDefaultValue()).isNull();

  }

  @Test
  public void shouldRejectNullMethodParameter() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HandlerParameter((MethodParameter) null))
            .withMessage("parameter is required");
  }

  @Test
  public void shouldExposeDeclaredParameterKinds() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod(
            "kinds", String[].class, List.class, Map.class, Runnable.class);

    HandlerParameter arrayParameter = createParameter(0, method, null);
    assertThat(arrayParameter.isArray()).isTrue();
    assertThat(arrayParameter.isCollection()).isFalse();
    assertThat(arrayParameter.isInterface()).isFalse();

    HandlerParameter listParameter = createParameter(1, method, null);
    assertThat(listParameter.isArray()).isFalse();
    assertThat(listParameter.isCollection()).isTrue();
    assertThat(listParameter.isInterface()).isTrue();

    HandlerParameter mapParameter = createParameter(2, method, null);
    assertThat(mapParameter.isCollection()).isFalse();
    assertThat(mapParameter.isInterface()).isTrue();

    HandlerParameter runnableParameter = createParameter(3, method, null);
    assertThat(runnableParameter.isArray()).isFalse();
    assertThat(runnableParameter.isCollection()).isFalse();
    assertThat(runnableParameter.isInterface()).isTrue();
    assertThat(runnableParameter.isAssignableTo(Runnable.class)).isTrue();
  }

  @Test
  public void shouldResolveResolvableTypeLazilyAndCacheIt() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod(
            "getGenerics", List.class, Map.class, String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    ResolvableType resolvableType = parameter.getResolvableType();
    assertThat(resolvableType.resolve()).isEqualTo(List.class);
    assertThat(resolvableType.getGeneric(0).resolve()).isEqualTo(String.class);

    assertThat(parameter.getResolvableType()).isSameAs(resolvableType);

    HandlerParameter mapParameter = createParameter(1, method, null);
    ResolvableType mapType = mapParameter.getResolvableType();
    assertThat(mapType.resolve()).isEqualTo(Map.class);
    assertThat(mapType.getGeneric(0).resolve()).isEqualTo(String.class);
    assertThat(mapType.getGeneric(1).resolve()).isEqualTo(Integer.class);
  }

  @Test
  public void shouldCreateTypeDescriptorLazilyAndCacheIt() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod(
            "getGenerics", List.class, Map.class, String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.getTypeDescriptor().getType()).isEqualTo(List.class);
    assertThat(parameter.getTypeDescriptor()).isSameAs(parameter.getTypeDescriptor());
  }

  @Test
  public void shouldExposeMethodAndParameterAnnotations() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("annotatedMethod", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.getMethodAnnotations()).isNotEmpty();
    assertThat(parameter.getMethodAnnotation(Deprecated.class)).isNotNull();
    assertThat(parameter.getMethodAnnotation(MethodMarker.class)).isNotNull();
    assertThat(parameter.hasMethodAnnotation(Deprecated.class)).isTrue();
    assertThat(parameter.hasMethodAnnotation(MethodMarker.class)).isTrue();
    assertThat(parameter.hasMethodAnnotation(SuppressWarnings.class)).isFalse();
    assertThat(parameter.getMethodAnnotation(SuppressWarnings.class)).isNull();

    assertThat(parameter.getParameterAnnotations()).hasSize(1);
    assertThat(parameter.hasParameterAnnotations()).isTrue();
    assertThat(parameter.getParameterAnnotation(ParameterMarker.class)).isNotNull();
    assertThat(parameter.hasParameterAnnotation(ParameterMarker.class)).isTrue();
    assertThat(parameter.hasParameterAnnotation(RequestParam.class)).isFalse();
    assertThat(parameter.getParameterAnnotation(RequestParam.class)).isNull();

    HandlerParameter plainParameter = createParameter(
            0, HandlerParameterTests.class.getDeclaredMethod("plain", String.class), null);
    assertThat(plainParameter.getParameterAnnotations()).isEmpty();
    assertThat(plainParameter.hasParameterAnnotations()).isFalse();
    assertThat(plainParameter.getParameterAnnotation(ParameterMarker.class)).isNull();
    assertThat(plainParameter.hasParameterAnnotation(ParameterMarker.class)).isFalse();
  }

  @Test
  public void shouldReadNamedValueInfoFromRequestParam() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("annotated", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.hasNamedValueInfo()).isFalse();
    NamedValueInfo info = parameter.getNamedValueInfo();
    assertThat(parameter.hasNamedValueInfo()).isTrue();
    assertThat(parameter.getNamedValueInfo()).isSameAs(info);

    assertThat(info.name).isEqualTo("query");
    assertThat(info.required).isFalse();
    assertThat(info.defaultValue).isEqualTo("fallback");
    assertThat(parameter.getName()).isEqualTo("query");
    assertThat(parameter.isRequired()).isFalse();
    assertThat(parameter.getDefaultValue()).isEqualTo("fallback");
  }

  @Test
  public void shouldFallBackToDiscoveredParameterName() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("annotatedWithEmptyName", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.getName()).isEqualTo("value");
    assertThat(parameter.getParameterName()).isEqualTo("value");
    assertThat(parameter.getDefaultValue()).isNull();
  }

  @Test
  public void shouldResolveAliasForName() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("annotatedWithValueAlias", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.getName()).isEqualTo("alias");
  }

  @Test
  public void shouldReadNamedValueInfoFromComposedAnnotation() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod(
            "annotatedWithComposedAnnotation", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    assertThat(parameter.getName()).isEqualTo("token");
    assertThat(parameter.isRequired()).isFalse();
  }

  @Test
  public void shouldDetectEmbeddedValues() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod(
            "annotatedWithEmbeddedValues", String.class);
    HandlerParameter parameter = createParameter(0, method, null);

    NamedValueInfo info = parameter.getNamedValueInfo();
    assertThat(info.name).isEqualTo("${param.name}");
    assertThat(info.nameEmbedded).isTrue();
    assertThat(info.defaultValue).isEqualTo("${param.default:fallback}");
    assertThat(info.defaultValueEmbedded).isTrue();
  }

  @Test
  public void shouldConvertDefaultNoneSentinelToNull() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("annotatedWithEmptyName", String.class);
    SynthesizingMethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(method, 0);
    methodParameter.initParameterNameDiscovery(DefaultParameterNameDiscoverer.INSTANCE);
    HandlerParameter parameter = new HandlerParameter(methodParameter) {
      @Override
      protected NamedValueInfo createNamedValueInfo() {
        return new NamedValueInfo(Constant.DEFAULT_NONE, true, Constant.DEFAULT_NONE);
      }
    };
    assertThat(parameter.getName()).isEqualTo("value");
    assertThat(parameter.getDefaultValue()).isNull();
  }

  @Test
  public void shouldUseExplicitNamedValueInfo() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("plain", String.class);
    HandlerParameter parameter = createParameter(0, method, null);
    NamedValueInfo info = new NamedValueInfo("custom", false, "default");

    parameter.withNamedValueInfo(info);

    assertThat(parameter.hasNamedValueInfo()).isTrue();
    assertThat(parameter.getNamedValueInfo()).isSameAs(info);
    assertThat(parameter.getName()).isEqualTo("custom");
    assertThat(parameter.isRequired()).isFalse();
    assertThat(parameter.getDefaultValue()).isEqualTo("default");
  }

  @Test
  public void shouldFailWhenParameterNameIsNotAvailable() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("plain", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0) {
      @Override
      public @Nullable String getParameterName() {
        return null;
      }
    };
    HandlerParameter parameter = new HandlerParameter(methodParameter);

    assertThatThrownBy(parameter::getParameterName)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Name for argument of type [java.lang.String] not specified")
            .hasMessageContaining("'-parameters'");
  }

  @Test
  public void shouldResolveArgumentFromHttpContext() throws Exception {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("plain", String.class);
    HandlerParameter parameter = createParameter(0, method, null);
    MockHttpContext context = new MockHttpContext();

    context.setParameter("value", "today");
    assertThat(parameter.resolveArgument(context)).isEqualTo("today");

    context.removeParameter("value");
    assertThat(parameter.resolveArgument(context)).isNull();
  }

  @Test
  public void shouldExposeUnderlyingMethodParameter() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("plain", String.class);
    SynthesizingMethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(method, 0);
    HandlerParameter parameter = new HandlerParameter(methodParameter);

    assertThat(parameter.getMethodParameter()).isSameAs(methodParameter);
    assertThat(parameter.getMethod()).isEqualTo(method);
    assertThat(parameter.getParameterIndex()).isZero();
  }

  @Test
  public void shouldCopySharedState() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("plain", String.class);
    HandlerParameter original = createParameter(0, method, "name");
    original.setAttribute("key", "value");

    String name = original.getName();
    ResolvableType resolvableType = original.getResolvableType();
    var typeDescriptor = original.getTypeDescriptor();

    HandlerParameter copy = new HandlerParameter(original);

    assertThat(copy.getMethodParameter()).isSameAs(original.getMethodParameter());
    assertThat(copy.getAttribute("key")).isEqualTo("value");
    assertThat(copy.getName()).isEqualTo(name);
    assertThat(copy.getResolvableType()).isSameAs(resolvableType);
    assertThat(copy.getTypeDescriptor()).isSameAs(typeDescriptor);
    assertThat(copy.hasNamedValueInfo()).isTrue();
    assertThat(copy).isEqualTo(original);
    assertThat(copy.hashCode()).isEqualTo(original.hashCode());

    copy.setAttribute("another", "attribute");
    assertThat(original.getAttribute("another")).isEqualTo("attribute");
  }

  @Test
  public void shouldImplementEqualityBasedOnMethodParameter() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
    HandlerParameter first = createParameter(0, method, "name");
    HandlerParameter second = createParameter(0, method, "otherName");
    HandlerParameter third = createParameter(1, method, "name");

    assertThat(first).isEqualTo(first);
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first).isNotEqualTo(third);
    assertThat(first).isNotEqualTo(null);
    assertThat(first).isNotEqualTo("not a parameter");
  }

  @Test
  public void shouldPrintMethodNameAndParameterIndex() throws NoSuchMethodException {
    final Method method = HandlerParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
    HandlerParameter parameter = createParameter(1, method, "name");

    assertThat(parameter).hasToString("method 'isRequired' parameter 1");
  }

  static HandlerParameter createParameter(int idx, Method method, String name) {
    SynthesizingMethodParameter parameter = SynthesizingMethodParameter.forExecutable(method, idx);
    parameter.initParameterNameDiscovery(DefaultParameterNameDiscoverer.INSTANCE);
    return new MockHandlerParameter(parameter, name);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface MethodMarker {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @interface ParameterMarker {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @RequestParam(name = "token", required = false)
  @interface TokenParam {
  }

}
