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

  @Test
  void equalsWithSameInstance() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, String.class);

    assertThat(key).isEqualTo(key);
  }

  @Test
  void equalsWithNull() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, String.class);

    assertThat(key).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentObjectType() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, String.class);

    assertThat(key).isNotEqualTo("not a MethodClassKey");
  }

  @Test
  void hashCodeWithMethodOnly() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, null);

    assertThat(key.hashCode()).isEqualTo(method.hashCode());
  }

  @Test
  void hashCodeWithMethodAndTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, String.class);

    int expectedHash = method.hashCode() + String.class.hashCode() * 29;
    assertThat(key.hashCode()).isEqualTo(expectedHash);
  }

  @Test
  void toStringWithoutTargetClass() throws Exception {
    Method method = String.class.getMethod("trim");
    MethodClassKey key = new MethodClassKey(method, null);

    assertThat(key.toString()).isEqualTo(method.toString());
  }

  @Test
  void compareToWithDifferentTargetClassNames() throws Exception {
    Method toString = Object.class.getMethod("toString");

    MethodClassKey key1 = new MethodClassKey(toString, String.class);
    MethodClassKey key2 = new MethodClassKey(toString, Integer.class);

    int result = key1.compareTo(key2);
    assertThat(result).isEqualTo(String.class.getName().compareTo(Integer.class.getName()));
  }

  @Test
  void compareToWithOneNullTargetClassReturnsZero() throws Exception {
    Method method = String.class.getMethod("trim");

    MethodClassKey keyWithClass = new MethodClassKey(method, String.class);
    MethodClassKey keyWithoutClass = new MethodClassKey(method, null);

    assertThat(keyWithClass.compareTo(keyWithoutClass)).isEqualTo(0);
    assertThat(keyWithoutClass.compareTo(keyWithClass)).isEqualTo(0);
  }

  @Test
  void compareToWithSameMethodNameButDifferentParameters() throws Exception {
    Method method1 = String.class.getMethod("valueOf", Object.class);
    Method method2 = String.class.getMethod("valueOf", char[].class);

    MethodClassKey key1 = new MethodClassKey(method1, null);
    MethodClassKey key2 = new MethodClassKey(method2, null);

    assertThat(key1.compareTo(key2)).isNotEqualTo(0);
  }

  @Test
  void compareToReturnsZeroForIdenticalKeys() throws Exception {
    Method method = String.class.getMethod("trim");

    MethodClassKey key1 = new MethodClassKey(method, String.class);
    MethodClassKey key2 = new MethodClassKey(method, String.class);

    assertThat(key1.compareTo(key2)).isEqualTo(0);
  }

  @Test
  void compareToDifferentMethodsByName() throws Exception {
    Method methodA = String.class.getMethod("charAt", int.class); // 'charAt' comes before 'length'
    Method methodB = String.class.getMethod("length");

    MethodClassKey keyA = new MethodClassKey(methodA, null);
    MethodClassKey keyB = new MethodClassKey(methodB, null);

    assertThat(keyA.compareTo(keyB)).isNegative();
    assertThat(keyB.compareTo(keyA)).isPositive();
  }

}