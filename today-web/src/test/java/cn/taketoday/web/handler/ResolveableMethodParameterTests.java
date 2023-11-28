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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/28 13:44
 * @since 3.0
 */
public class ResolveableMethodParameterTests {

  public void method(@Nullable String name) {

  }

  public void isRequired(String name, @RequestParam(name = "myAge") int age) {

  }

  public void getGenerics(List<String> names, Map<String, Integer> map, String name) {

  }

  @Test
  public void test() throws NoSuchMethodException {
    final Method method = ResolveableMethodParameterTests.class.getDeclaredMethod("method", String.class);
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
    final Method method = ResolveableMethodParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
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
