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

package infra.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:46
 */
class MethodClassKeyTests {

  @Test
  void comparesMethodSignaturesDifferently() throws Exception {
    Method method1 = String.class.getMethod("indexOf", int.class);
    Method method2 = String.class.getMethod("indexOf", String.class);

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

  @Test
  void comparesTargetClassesDifferently() throws Exception {
    Method toString = Object.class.getMethod("toString");

    MethodClassKey key1 = new MethodClassKey(toString, String.class);
    MethodClassKey key2 = new MethodClassKey(toString, Integer.class);

    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

  @Test
  void equalsWithSameMethodAndTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key1 = new MethodClassKey(method, String.class);
    MethodClassKey key2 = new MethodClassKey(method, String.class);

    assertThat(key1).isEqualTo(key2);
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  void equalsWithSameMethodAndDifferentTargetClass() throws Exception {
    Method toString = Object.class.getMethod("toString");
    MethodClassKey key1 = new MethodClassKey(toString, String.class);
    MethodClassKey key2 = new MethodClassKey(toString, Integer.class);

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  void equalsWithDifferentMethodsAndSameTargetClass() throws Exception {
    Method method1 = String.class.getMethod("trim");
    Method method2 = String.class.getMethod("strip");

    MethodClassKey key1 = new MethodClassKey(method1, String.class);
    MethodClassKey key2 = new MethodClassKey(method2, String.class);

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  void equalsWithNullTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key1 = new MethodClassKey(method, null);
    MethodClassKey key2 = new MethodClassKey(method, null);

    assertThat(key1).isEqualTo(key2);
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  void toStringWithTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, Integer.class);

    assertThat(key.toString()).contains("trim")
            .contains("on")
            .contains(Integer.class.getName());
  }

  @Test
  void toStringWithNullTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, null);

    assertThat(key.toString()).contains("trim")
            .doesNotContain("on");
  }

  @Test
  void compareToWithSameMethodAndTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key1 = new MethodClassKey(method, String.class);
    MethodClassKey key2 = new MethodClassKey(method, String.class);

    assertThat(key1.compareTo(key2)).isZero();
  }

  @Test
  void comparesMethodNamesDifferently() throws Exception {
    Method method1 = String.class.getMethod("toString");
    Method method2 = String.class.getMethod("valueOf", Object.class);

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isLessThan(0);
    assertThat(key2.compareTo(key1)).isGreaterThan(0);
  }

  @Test
  void comparesOverloadedMethodsDifferently() throws Exception {
    Method method1 = String.class.getMethod("substring", int.class);
    Method method2 = String.class.getMethod("substring", int.class, int.class);

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

  @Test
  void comparesSameMethodWithDifferentParameterTypesDifferently() throws Exception {
    Method method1 = String.class.getMethod("indexOf", String.class);
    Method method2 = String.class.getMethod("indexOf", String.class, int.class);

    MethodClassKey key1 = new MethodClassKey(method1, String.class);
    MethodClassKey key2 = new MethodClassKey(method2, String.class);

    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

  @Test
  void equalsWithSameMethodFromDifferentClasses() throws Exception {
    Method toString1 = String.class.getMethod("toString");
    Method toString2 = Object.class.getMethod("toString");

    MethodClassKey key1 = new MethodClassKey(toString1, null);
    MethodClassKey key2 = new MethodClassKey(toString2, null);

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  void compareToWithOneNullTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");

    MethodClassKey key1 = new MethodClassKey(method, String.class);
    MethodClassKey key2 = new MethodClassKey(method, null);

    // The comparison should only consider method name and toString when targetClass is null
    assertThat(key1.compareTo(key2)).isZero();
    assertThat(key2.compareTo(key1)).isZero();
  }

  @Test
  void comparesToNullReturnsSameResult() throws Exception {
    Method method1 = String.class.getMethod("trim");
    Method method2 = String.class.getMethod("trim");

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isZero();
  }

  @Test
  void compareToWithBothNullTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");

    MethodClassKey key1 = new MethodClassKey(method, null);
    MethodClassKey key2 = new MethodClassKey(method, null);

    assertThat(key1.compareTo(key2)).isZero();
  }

  @Test
  void compareToWithSameMethodAndMixedNullTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");

    MethodClassKey key1 = new MethodClassKey(method, null);
    MethodClassKey key2 = new MethodClassKey(method, String.class);
    MethodClassKey key3 = new MethodClassKey(method, Integer.class);

    assertThat(key1.compareTo(key2)).isZero();
    assertThat(key2.compareTo(key3)).isNotEqualTo(0);
  }

  @Test
  void compareToWithDifferentMethodsAndNullTargetClass() throws Exception {
    Method method1 = String.class.getMethod("trim");
    Method method2 = String.class.getMethod("strip");

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    // For same-length method names, compare by full method toString
    assertThat(key1.compareTo(key2)).isGreaterThan(0);
    assertThat(key2.compareTo(key1)).isLessThan(0);
  }

  @Test
  void compareToWithDifferentMethodNameLengths() throws Exception {
    Method method1 = String.class.getMethod("trim");
    Method method2 = String.class.getMethod("substring", int.class);

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isGreaterThan(0);
    assertThat(key2.compareTo(key1)).isLessThan(0);
  }

  @Test
  void compareMethodsWithSameNameInDifferentClasses() throws Exception {
    Method method1 = String.class.getMethod("toString");
    Method method2 = Integer.class.getMethod("toString");

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    // Same name methods should be differentiated by their full toString
    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

}