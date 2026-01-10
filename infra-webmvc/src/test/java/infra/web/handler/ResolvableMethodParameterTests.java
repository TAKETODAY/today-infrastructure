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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import infra.core.annotation.SynthesizingMethodParameter;
import infra.web.MockResolvableMethodParameter;
import infra.web.annotation.RequestParam;
import infra.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/28 13:44
 * @since 3.0
 */
public class ResolvableMethodParameterTests {

  public void method(@Nullable String name) {

  }

  public void isRequired(String name, @RequestParam(name = "myAge") int age) {

  }

  public void getGenerics(List<String> names, Map<String, Integer> map, String name) {

  }

  @Test
  public void test() throws NoSuchMethodException {
    final Method method = ResolvableMethodParameterTests.class.getDeclaredMethod("method", String.class);
    final ResolvableMethodParameter methodParameter = createParameter(0, method, "name");

    assertThat(methodParameter).isNotNull();

    assertThat(methodParameter.is(String.class)).isTrue();
    assertThat(methodParameter.getName()).isNotNull().isEqualTo("name");

    assertThat(methodParameter.getParameterIndex()).isZero();
    assertThat(methodParameter.getParameterType()).isEqualTo(String.class);
    assertThat(methodParameter.isRequired()).isFalse();
    assertThat(methodParameter.getDefaultValue()).isNull();

  }

  @Test
  public void testRequired() throws NoSuchMethodException {
    final Method method = ResolvableMethodParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
    final ResolvableMethodParameter methodParameter = createParameter(0, method, "name");
    assertThat(methodParameter.isRequired()).isTrue();
    assertThat(methodParameter.isAssignableTo(CharSequence.class)).isTrue();
    assertThat(methodParameter.isInstance("dbashbgdsaydgasyu")).isTrue();
    assertThat(methodParameter.is(int.class)).isFalse();

    final ResolvableMethodParameter ageMethodParameter = createParameter(1, method, null);
    assertThat(ageMethodParameter.isRequired()).isTrue();
    assertThat(ageMethodParameter.getParameterIndex()).isEqualTo(1);
    assertThat(ageMethodParameter.getParameterType()).isEqualTo(int.class);
    assertThat(ageMethodParameter.getName()).isEqualTo("myAge");
    assertThat(ageMethodParameter.isArray()).isFalse();
    assertThat(ageMethodParameter.isCollection()).isFalse();
    assertThat(ageMethodParameter.isInterface()).isFalse();
    assertThat(ageMethodParameter.getDefaultValue()).isNull();

  }

  static ResolvableMethodParameter createParameter(int idx, Method method, String name) {
    SynthesizingMethodParameter parameter = SynthesizingMethodParameter.forExecutable(method, idx);
    return new MockResolvableMethodParameter(parameter, name);
  }

}
