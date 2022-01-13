/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.lang.Required;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/28 13:44
 * @since 3.0
 */
public class ResolveableMethodParameterTests {

  public void method(String name) {

  }

  public void isRequired(@Required String name, @RequestParam(value = "myAge", required = true) int age) {

  }

  public void getGenerics(List<String> names, Map<String, Integer> map, String name) {

  }

  @Test
  public void test() throws NoSuchMethodException {
    final Method method = ResolveableMethodParameterTests.class.getDeclaredMethod("method", String.class);
    final ResolvableMethodParameter methodParameter = new ResolvableMethodParameter(0, method, "name");

    assertThat(methodParameter).isNotNull();
    assertThat(methodParameter.getHandlerMethod()).isNull();

    assertThat(methodParameter.is(String.class)).isTrue();
    assertThat(methodParameter.getName()).isNotNull().isEqualTo("name");

    assertThat(methodParameter.getParameterIndex()).isZero();
    assertThat(methodParameter.getParameterClass()).isEqualTo(String.class);
    assertThat(methodParameter.isRequired()).isFalse();
    assertThat(methodParameter.getDefaultValue()).isNull();

  }

  @Test
  public void testRequired() throws NoSuchMethodException {
    final Method method = ResolveableMethodParameterTests.class.getDeclaredMethod("isRequired", String.class, int.class);
    final ResolvableMethodParameter methodParameter = new ResolvableMethodParameter(0, method, "name");
    assertThat(methodParameter.isRequired()).isTrue();
    assertThat(methodParameter.isAssignableTo(CharSequence.class)).isTrue();
    assertThat(methodParameter.isInstance("dbashbgdsaydgasyu")).isTrue();
    assertThat(methodParameter.is(int.class)).isFalse();

    final ResolvableMethodParameter ageMethodParameter = new ResolvableMethodParameter(1, method, null);
    assertThat(ageMethodParameter.isRequired()).isTrue();
    assertThat(ageMethodParameter.getParameterIndex()).isEqualTo(1);
    assertThat(ageMethodParameter.getParameterClass()).isEqualTo(int.class);
    assertThat(ageMethodParameter.getName()).isEqualTo("myAge");
    assertThat(ageMethodParameter.isArray()).isFalse();
    assertThat(ageMethodParameter.isCollection()).isFalse();
    assertThat(ageMethodParameter.isInterface()).isFalse();
    assertThat(ageMethodParameter.getDefaultValue()).isEqualTo("0");

  }

  @Test
  public void getGenerics() throws NoSuchMethodException {
    final Method getGenerics = ResolveableMethodParameterTests.class.getDeclaredMethod("getGenerics", List.class, Map.class, String.class);
    final ResolvableMethodParameter listParameter = new ResolvableMethodParameter(0, getGenerics, "names");
    final ResolvableMethodParameter mapParameter = new ResolvableMethodParameter(1, getGenerics, "map");
    final ResolvableMethodParameter stringParameter = new ResolvableMethodParameter(2, getGenerics, "name");

    assertThat(listParameter.getParameterClass()).isInterface().isEqualTo(List.class);
    assertThat(mapParameter.getParameterClass()).isInterface().isEqualTo(Map.class);

    // getGenerics
    assertThat(listParameter.getGenerics()).hasSize(1);
    assertThat(mapParameter.getGenerics()).hasSize(2);
    assertThat(stringParameter.getGenerics()).isEmpty();

    // getGeneric
    assertThat(listParameter.getGeneric(0)).isEqualTo(String.class);
    assertThat(mapParameter.getGeneric(0)).isEqualTo(String.class);
    assertThat(mapParameter.getGeneric(1)).isEqualTo(Integer.class);

    assertThat(stringParameter.getGeneric(1)).isNull();
    assertThat(stringParameter.getGeneric(0)).isNull();

    // isGenericPresent
    assertThat(mapParameter.isGenericPresent(int.class)).isFalse();
    assertThat(mapParameter.isGenericPresent(Integer.class)).isTrue();
    assertThat(listParameter.isGenericPresent(Integer.class)).isFalse();
    assertThat(listParameter.isGenericPresent(String.class)).isTrue();

    assertThat(mapParameter.isGenericPresent(int.class, 0)).isFalse();
    assertThat(mapParameter.isGenericPresent(Integer.class, 1)).isTrue();
    assertThat(mapParameter.isGenericPresent(Integer.class, 0)).isFalse();
    assertThat(mapParameter.isGenericPresent(String.class, 1)).isFalse();
    assertThat(mapParameter.isGenericPresent(String.class, 0)).isTrue();

    assertThat(listParameter.isGenericPresent(Integer.class, 0)).isFalse();
    assertThat(listParameter.isGenericPresent(String.class, 0)).isTrue();
    assertThat(listParameter.isGenericPresent(String.class, 1)).isFalse();

    assertThat(stringParameter.isGenericPresent(String.class, 1)).isFalse();
    assertThat(stringParameter.isGenericPresent(String.class, 0)).isFalse();

  }

}
