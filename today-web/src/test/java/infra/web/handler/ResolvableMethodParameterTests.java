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
